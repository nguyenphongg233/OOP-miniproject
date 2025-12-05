
package ecosystem.models;

public abstract class Organism {
    public int getAge() {
        return age;
    }

    protected int x;
    protected int y;
    protected int energy;
    protected int age;

    public Organism(int x, int y, int energy) {
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.age = 0;
    }

    public int getEnergy() {
        return energy;
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
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    @Override
    public String toString() {
        return "?";
    }
}
