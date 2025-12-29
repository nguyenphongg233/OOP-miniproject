package ecosystem.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ecosystem.Settings;

public class Grid {
    public interface GridListener {
        default void organismAdded(Organism o) {}
        default void organismRemoved(Organism o) {}
        default void organismUpdated(Organism o) {}
    }
    private final java.util.List<GridListener> listeners = new java.util.ArrayList<>();

    public void addListener(GridListener l) { if (l != null) listeners.add(l); }
    public void removeListener(GridListener l) { listeners.remove(l); }
        // Trả về sinh vật đầu tiên tại vị trí (x, y), hoặc null nếu không có
    public Organism getOrganismAt(int x, int y) {
        for (Organism o : organisms) {
            if (o.getX() == x && o.getY() == y) return o;
        }
        return null;
    }
    private int width;
    private int height;
    private List<Organism> organisms;
    // map for fast id -> organism lookup
    private java.util.Map<Integer, Organism> idIndex;
    private int plantEnergy;
    private double plantGrowRate;
    // Logical terrain map for each cell (water/sand/grass/rock)
    private int[][] terrain;
    // terrain type constants (shared with EnvironmentGenerator)
    public static final int TERRAIN_WATER = 0;
    public static final int TERRAIN_SAND  = 1;
    public static final int TERRAIN_GRASS = 2;
    public static final int TERRAIN_ROCK  = 3;
    private static final Random RNG = new Random();

    public Grid() {
        this(new Settings());
    }

    // Initialize grid using Settings (from GUI)
    public Grid(Settings s) {
        this.width = s.getGridWidth();
        this.height = s.getGridHeight();
        this.organisms = new ArrayList<>();
        this.idIndex = new java.util.HashMap<>();
        this.plantEnergy = s.getPlantEnergy();
        this.plantGrowRate = s.getPlantGrowRate();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getPlantEnergy() { return plantEnergy; }
    public double getPlantGrowRate() { return plantGrowRate; }

    /** Install or replace the terrain map. Dimensions must match grid size. */
    public void setTerrain(int[][] terrain) {
        if (terrain == null) {
            this.terrain = null;
            return;
        }
        if (terrain.length != width || terrain[0].length != height) {
            // silently ignore mismatched terrain
            return;
        }
        this.terrain = terrain;
    }

    /** Expose the current terrain map (may be null). */
    public int[][] getTerrain() {
        return terrain;
    }

    /** Return terrain code at (x,y), defaulting to GRASS if unknown. */
    public int getTerrainAt(int x, int y) {
        if (terrain == null || !inBounds(x, y)) return TERRAIN_GRASS;
        return terrain[x][y];
    }

    /** True if animals can stand/move on this cell (grass or rock). */
    public boolean isWalkableTerrain(int x, int y) {
        int t = getTerrainAt(x, y);
        return t == TERRAIN_GRASS || t == TERRAIN_ROCK;
    }

    /** True if plants can grow/survive on this cell (grass only). */
    public boolean isPlantHabitable(int x, int y) {
        int t = getTerrainAt(x, y);
        return t == TERRAIN_GRASS;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public void addOrganism(Organism o) { 
        organisms.add(o);
        idIndex.put(o.getId(), o);
        for (GridListener l : listeners) {
            try { l.organismAdded(o); } catch (Exception ex) {}
        }
    }
    public void removeOrganism(Organism o) { 
        organisms.remove(o); 
        idIndex.remove(o.getId());
        for (GridListener l : listeners) {
            try { l.organismRemoved(o); } catch (Exception ex) {}
        }
    }

    /** O(1) lookup by id (returns null if not present) */
    public Organism getOrganismById(int id) {
        return idIndex.get(id);
    }

    public List<Organism> organismsAt(int x, int y) {
        List<Organism> ret = new ArrayList<>();
        for (Organism o : organisms) {
            if (o.getX() == x && o.getY() == y) ret.add(o);
        }
        return ret;
    }

    /**
     * Return all organisms currently on the grid (live list reference may change after steps).
     */
    public List<Organism> getOrganisms() {
        return organisms;
    }

    public boolean isCellEmpty(int x, int y) { return organismsAt(x, y).isEmpty(); }

    public List<int[]> getNeighborPositions(int x, int y) {
        List<int[]> poss = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (inBounds(nx, ny)) poss.add(new int[]{nx, ny});
            }
        }
        return poss;
    }

    public List<int[]> getEmptyNeighbors(int x, int y) {
        List<int[]> empties = new ArrayList<>();
        for (int[] p : getNeighborPositions(x, y)) {
            if (isCellEmpty(p[0], p[1])) empties.add(p);
        }
        return empties;
    }

    public Organism findNeighborOfType(int x, int y, Class<?> cls) {
        for (int[] p : getNeighborPositions(x, y)) {
            List<Organism> objs = organismsAt(p[0], p[1]);
            for (Organism o : objs) {
                if (cls.isInstance(o)) return o;
            }
        }
        return null;
    }

    public void stepAll() {
        // Snapshot organisms at the beginning of the day
        List<Organism> snapshot = new ArrayList<>(organisms);

        // 1) Day start: age and basal metabolism
        for (Organism o : snapshot) {
            o.onDayStart();
        }

        // 2) Plant growth (spread to 4-neighbor cells) based on starting positions
        for (Organism o : snapshot) {
            if (o instanceof Plant && o.isAlive()) {
                ((Plant)o).dayUpdate(this);
            }
        }

        // 3) Collect animals that are still alive after metabolism
        List<Animal> animals = new ArrayList<>();
        for (Organism o : snapshot) {
            if (o instanceof Animal && o.isAlive()) {
                animals.add((Animal)o);
            }
        }

        // 3a) Decide random movement intents (stay vs move) for each animal
        java.util.Map<Animal, int[]> desiredMoves = new java.util.HashMap<>();
        for (Animal a : animals) {
            if (!a.isAlive()) continue;
            // 50% chance to attempt a move
            if (RNG.nextBoolean()) {
                int[] target = a.chooseRandomMoveTarget(this);
                if (target != null) {
                    desiredMoves.put(a, target);
                }
            }
        }

        // 3b) Apply movements simultaneously, resolving collisions
        java.util.Set<String> reservedTargets = new java.util.HashSet<>();
        for (Animal a : animals) {
            int[] target = desiredMoves.get(a);
            if (target == null || !a.isAlive()) continue;
            int tx = target[0], ty = target[1];
            String key = tx + "," + ty;
            // cell must remain empty and not already taken by another mover
            if (!isCellEmpty(tx, ty)) continue;
            if (reservedTargets.contains(key)) continue;
            a.setPosition(tx, ty);
            a.adjustEnergy(-a.getMoveCost());
            reservedTargets.add(key);
        }

        // 4) Eating phase after all movements
        for (Animal a : animals) {
            if (!a.isAlive()) continue;
            if (a instanceof Herbivore) {
                ((Herbivore)a).eat(this);
            } else if (a instanceof Carnivore) {
                ((Carnivore)a).eat(this);
            }
        }

        // 5) Reproduction phase
        for (Animal a : animals) {
            if (!a.isAlive()) continue;
            if (a instanceof Herbivore) {
                ((Herbivore)a).tryReproduce(this);
            } else if (a instanceof Carnivore) {
                ((Carnivore)a).tryReproduce(this);
            }
        }

        // 6) Remove dead organisms and rebuild id index
        List<Organism> alive = new ArrayList<>();
        for (Organism o : organisms) if (o.isAlive()) alive.add(o);

        organisms = alive;
        idIndex.clear();
        for (Organism o : organisms) idIndex.put(o.getId(), o);

        // Notify listeners that all surviving organisms have been updated this day
        for (Organism o : organisms) {
            for (GridListener l : listeners) {
                try { l.organismUpdated(o); } catch (Exception ex) {}
            }
        }
    }

    public void populateBasic(int initialPlants, int initialHerbivores, int initialCarnivores,
                              int herbivoreEnergy, int herbivoreMoveCost, int herbivoreEatGain,
                              int carnivoreEnergy, int carnivoreMoveCost, int carnivoreEatGain,
                              int herbivoreReproduceThreshold, int herbivoreMetabolismCost, double herbivoreAbsorptionRate,
                              int carnivoreReproduceThreshold, int carnivoreMetabolismCost, double carnivoreAbsorptionRate) {
        for (int i = 0; i < initialPlants; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Plant(x, y, plantEnergy));
        }
        for (int i = 0; i < initialHerbivores; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Herbivore(x, y, herbivoreEnergy, herbivoreMoveCost, herbivoreEatGain,
                herbivoreReproduceThreshold, herbivoreMetabolismCost, herbivoreAbsorptionRate));
        }
        for (int i = 0; i < initialCarnivores; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Carnivore(x, y, carnivoreEnergy, carnivoreMoveCost, carnivoreEatGain,
                carnivoreReproduceThreshold, carnivoreMetabolismCost, carnivoreAbsorptionRate));
        }
    }

    /**
     * After a terrain map has been installed, ensure that initial organisms
     * are not placed on invalid terrain (e.g. animals in water, plants off grass).
     * Call this right after setting terrain for a newly created grid.
     */
    public void ensureOrganismsOnValidTerrain() {
        if (terrain == null) return;
        for (Organism o : organisms) {
            if (o instanceof Plant) {
                if (!isPlantHabitable(o.getX(), o.getY())) {
                    relocateOrganismToValidCell(o, true);
                }
            } else if (o instanceof Animal) {
                if (!isWalkableTerrain(o.getX(), o.getY())) {
                    relocateOrganismToValidCell(o, false);
                }
            }
        }
    }

    /**
     * Move the given organism to a random empty cell that is valid for its type.
     * If no suitable cell is found within a reasonable number of attempts,
     * the organism is left at its current position.
     */
    private void relocateOrganismToValidCell(Organism o, boolean plant) {
        if (width <= 0 || height <= 0) return;
        int maxAttempts = Math.max(100, width * height);
        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            if (!isCellEmpty(x, y)) continue;
            if (plant) {
                if (!isPlantHabitable(x, y)) continue;
            } else {
                if (!isWalkableTerrain(x, y)) continue;
            }
            o.setPosition(x, y);
            return;
        }
        // If we cannot find any valid cell, keep the organism where it is.
    }

    public List<String> asciiGrid() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                List<Organism> objs = organismsAt(x, y);
                if (objs.isEmpty()) sb.append('.');
                else sb.append(objs.get(0).toString());
            }
            rows.add(sb.toString());
        }
        return rows;
    }

    public java.util.Map<String, Integer> counts() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        for (Organism o : organisms) {
            String name = o.getClass().getSimpleName();
            map.put(name, map.getOrDefault(name, 0) + 1);
        }
        return map;
    }
}
