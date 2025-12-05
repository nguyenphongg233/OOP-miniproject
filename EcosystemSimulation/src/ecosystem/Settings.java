package ecosystem;

public class Settings {
    // Grid
    public int gridWidth = 50;
    public int gridHeight = 30;

    // Initial counts
    public int initialPlants = 40;
    public int initialHerbivores = 10;
    public int initialCarnivores = 5;

    // Plant parameters
    public int plantEnergy = 5;
    public double plantGrowRate = 0.1;

    // Herbivore params
    public int herbivoreStartEnergy = 20;
    public int herbivoreMoveCost = 1;
    public int herbivoreEatGain = 6;
    public int herbivoreReproduceThreshold = 30;

    // Carnivore params
    public int carnivoreStartEnergy = 30;
    public int carnivoreMoveCost = 2;
    public int carnivoreEatGain = 18;
    public int carnivoreReproduceThreshold = 50;

    public Settings() {
    }
}
