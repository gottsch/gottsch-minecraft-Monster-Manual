package mod.gottsch.neo.gmm.core.entity.ai.goal;

public abstract class ChanceSummonGoal extends SummonGoal {
    protected double probability = 100;

    public ChanceSummonGoal(int summonCoolDownTime, double probability) {
        super(summonCoolDownTime);
        this.probability = probability;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
