/**
 * Lightweight, immutable snapshot for safe UI listing/binding of organisms.
 */
package ecosystem.models;

public class OrganismSnapshot {
    public final int id;
    public final String labelId;
    public final String type;
    public final int x;
    public final int y;
    public final int age;
    public final int energy;
    public final boolean alive;

    public OrganismSnapshot(int id, String labelId, String type, int x, int y, int age, int energy, boolean alive) {
        this.id = id;
        this.labelId = labelId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.age = age;
        this.energy = energy;
        this.alive = alive;
    }

    public static OrganismSnapshot from(Organism o) {
        return new OrganismSnapshot(
            o.getId(),
            o.getLabelId(),
            o.getType(),
            o.getX(),
            o.getY(),
            o.getAge(),
            o.getEnergy(),
            o.isAlive()
        );
    }

    @Override
    public String toString() {
        return labelId;
    }
}
