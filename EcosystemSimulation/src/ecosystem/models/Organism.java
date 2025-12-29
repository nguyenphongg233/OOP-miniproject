
/**
 * Base organism model: common fields/behavior for `Plant`, `Herbivore`, `Carnivore`.
 */
package ecosystem.models;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Organism {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;

    public int getId() { return id; }

    /**
     * Return a human-friendly ID with a type prefix and zero-padded numeric suffix.
     * Examples: Plant -> P000001, Herbivore -> H000012, Carnivore -> C000123
     */
    public String getLabelId() {
        // Prefix by type initial and zero-pad numeric id to 8 digits, e.g. P00000001
        String prefix = "?";
        String type = this.getClass().getSimpleName();
        if (type.startsWith("Plant")) prefix = "P";
        else if (type.startsWith("Herbivore")) prefix = "H";
        else if (type.startsWith("Carnivore")) prefix = "C";
        else prefix = type.isEmpty() ? "?" : type.substring(0, 1).toUpperCase();
        return String.format("%s%08d", prefix, id);
    }

    public int getAge() {
        return age;
    }

    private int x;
    private int y;
    private int energy;
    private int age;

    // Protected helpers for subclasses to modify state safely
    protected void setPosition(int nx, int ny) { this.x = nx; this.y = ny; }
    protected void adjustEnergy(int delta) { this.energy += delta; }
    protected void setEnergy(int e) { this.energy = e; }
    protected void incrementAge() { this.age++; }

    public Organism(int x, int y, int energy) {
        this.id = NEXT_ID.getAndIncrement();
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.age = 0;
    }

    public int getEnergy() {
        return energy;
    }

    /**
     * Called once per simulated day for every organism.
     * Default behavior: simply increase age by 1.
     */
    public void onDayStart() {
        incrementAge();
    }

    public abstract void step(Grid grid);

    // Type key for rendering / polymorphic behavior (default: class simple name)
    public String getType() { return this.getClass().getSimpleName(); }

    public boolean isAlive() {
        return energy > 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public int distanceTo(Organism other) {
        return Math.abs(this.x - other.getX()) + Math.abs(this.y - other.getY());
    }

    @Override
    public String toString() {
        return "?";
    }
}
