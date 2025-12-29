/**
 * Simulation parameters: grid size, organism counts, energy costs/gains, rates.
 */
package ecosystem;

public class Settings {
    // Grid
    private int gridWidth = 50;
    private int gridHeight = 30;

    // Initial counts
    private int initialPlants = 40;
    private int initialHerbivores = 10;
    private int initialCarnivores = 5;

    // Plant parameters
    private int plantEnergy = 5;
    private double plantGrowRate = 0.1;

    // Herbivore params
    private int herbivoreStartEnergy = 20;
    private int herbivoreMoveCost = 1;
    private int herbivoreEatGain = 6;
    private int herbivoreReproduceThreshold = 30;
    // per-day metabolism cost and absorption rate (0<rate<=1)
    private int herbivoreMetabolismCost = 1;
    private double herbivoreAbsorptionRate = 1.0;

    // Carnivore params
    private int carnivoreStartEnergy = 30;
    private int carnivoreMoveCost = 2;
    private int carnivoreEatGain = 18;
    private int carnivoreReproduceThreshold = 50;
    private int carnivoreMetabolismCost = 2;
    private double carnivoreAbsorptionRate = 1.0;

    
    public Settings() {
    }

    // Getters and setters
    public int getGridWidth() { return gridWidth; }
    public void setGridWidth(int gridWidth) { this.gridWidth = gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public void setGridHeight(int gridHeight) { this.gridHeight = gridHeight; }

    public int getInitialPlants() { return initialPlants; }
    public void setInitialPlants(int initialPlants) { this.initialPlants = initialPlants; }
    public int getInitialHerbivores() { return initialHerbivores; }
    public void setInitialHerbivores(int initialHerbivores) { this.initialHerbivores = initialHerbivores; }
    public int getInitialCarnivores() { return initialCarnivores; }
    public void setInitialCarnivores(int initialCarnivores) { this.initialCarnivores = initialCarnivores; }

    public int getPlantEnergy() { return plantEnergy; }
    public void setPlantEnergy(int plantEnergy) { this.plantEnergy = plantEnergy; }
    public double getPlantGrowRate() { return plantGrowRate; }
    public void setPlantGrowRate(double plantGrowRate) { this.plantGrowRate = plantGrowRate; }

    public int getHerbivoreStartEnergy() { return herbivoreStartEnergy; }
    public void setHerbivoreStartEnergy(int herbivoreStartEnergy) { this.herbivoreStartEnergy = herbivoreStartEnergy; }
    public int getHerbivoreMoveCost() { return herbivoreMoveCost; }
    public void setHerbivoreMoveCost(int herbivoreMoveCost) { this.herbivoreMoveCost = herbivoreMoveCost; }
    public int getHerbivoreEatGain() { return herbivoreEatGain; }
    public void setHerbivoreEatGain(int herbivoreEatGain) { this.herbivoreEatGain = herbivoreEatGain; }
    public int getHerbivoreReproduceThreshold() { return herbivoreReproduceThreshold; }
    public void setHerbivoreReproduceThreshold(int herbivoreReproduceThreshold) { this.herbivoreReproduceThreshold = herbivoreReproduceThreshold; }
    public int getHerbivoreMetabolismCost() { return herbivoreMetabolismCost; }
    public void setHerbivoreMetabolismCost(int herbivoreMetabolismCost) { this.herbivoreMetabolismCost = herbivoreMetabolismCost; }
    public double getHerbivoreAbsorptionRate() { return herbivoreAbsorptionRate; }
    public void setHerbivoreAbsorptionRate(double herbivoreAbsorptionRate) { this.herbivoreAbsorptionRate = herbivoreAbsorptionRate; }

    public int getCarnivoreStartEnergy() { return carnivoreStartEnergy; }
    public void setCarnivoreStartEnergy(int carnivoreStartEnergy) { this.carnivoreStartEnergy = carnivoreStartEnergy; }
    public int getCarnivoreMoveCost() { return carnivoreMoveCost; }
    public void setCarnivoreMoveCost(int carnivoreMoveCost) { this.carnivoreMoveCost = carnivoreMoveCost; }
    public int getCarnivoreEatGain() { return carnivoreEatGain; }
    public void setCarnivoreEatGain(int carnivoreEatGain) { this.carnivoreEatGain = carnivoreEatGain; }
    public int getCarnivoreReproduceThreshold() { return carnivoreReproduceThreshold; }
    public void setCarnivoreReproduceThreshold(int carnivoreReproduceThreshold) { this.carnivoreReproduceThreshold = carnivoreReproduceThreshold; }
    public int getCarnivoreMetabolismCost() { return carnivoreMetabolismCost; }
    public void setCarnivoreMetabolismCost(int carnivoreMetabolismCost) { this.carnivoreMetabolismCost = carnivoreMetabolismCost; }
    public double getCarnivoreAbsorptionRate() { return carnivoreAbsorptionRate; }
    public void setCarnivoreAbsorptionRate(double carnivoreAbsorptionRate) { this.carnivoreAbsorptionRate = carnivoreAbsorptionRate; }
}
