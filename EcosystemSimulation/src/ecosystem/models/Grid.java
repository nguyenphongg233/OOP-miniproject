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
    private Integer herbivoreReproduceThreshold;
    private Integer carnivoreReproduceThreshold;
    private static final Random RNG = new Random();

    public Grid() {
        this(new ecosystem.Settings());
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
    public int getHerbivoreReproduceThreshold() { return herbivoreReproduceThreshold; }
    public int getCarnivoreReproduceThreshold() { return carnivoreReproduceThreshold; }

    public void setHerbivoreReproduceThreshold(int t) { this.herbivoreReproduceThreshold = t; }
    public void setCarnivoreReproduceThreshold(int t) { this.carnivoreReproduceThreshold = t; }

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
        List<Organism> copy = new ArrayList<>(organisms);
        for (Organism o : copy) {
            o.step(this);
        }
        // remove dead
        List<Organism> alive = new ArrayList<>();
        for (Organism o : organisms) if (o.isAlive()) alive.add(o);
        // compute changes: removed and updated
        java.util.Map<Integer, Organism> before = new java.util.HashMap<>();
        for (Organism o : organisms) before.put(o.getId(), o);
        java.util.Map<Integer, Organism> after = new java.util.HashMap<>();
        for (Organism o : alive) after.put(o.getId(), o);

        // removed
        for (Integer id : before.keySet()) {
            if (!after.containsKey(id)) {
                Organism removed = before.get(id);
                for (GridListener l : listeners) {
                    try { l.organismRemoved(removed); } catch (Exception ex) {}
                }
            }
        }
        // updated (present in after)
        for (Integer id : after.keySet()) {
            Organism updated = after.get(id);
            for (GridListener l : listeners) {
                try { l.organismUpdated(updated); } catch (Exception ex) {}
            }
        }

        organisms = alive;
        // rebuild id index to reflect removals/moves
        idIndex.clear();
        for (Organism o : organisms) idIndex.put(o.getId(), o);
    }

    public void populateBasic(int initialPlants, int initialHerbivores, int initialCarnivores,
                              int herbivoreEnergy, int herbivoreMoveCost, int herbivoreEatGain,
                              int carnivoreEnergy, int carnivoreMoveCost, int carnivoreEatGain) {
        for (int i = 0; i < initialPlants; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Plant(x, y, plantEnergy));
        }
        for (int i = 0; i < initialHerbivores; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Herbivore(x, y, herbivoreEnergy, herbivoreMoveCost, herbivoreEatGain));
        }
        for (int i = 0; i < initialCarnivores; i++) {
            int x = RNG.nextInt(width);
            int y = RNG.nextInt(height);
            addOrganism(new Carnivore(x, y, carnivoreEnergy, carnivoreMoveCost, carnivoreEatGain));
        }
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
