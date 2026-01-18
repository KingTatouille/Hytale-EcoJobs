package fr.snoof.jobs.model;

public class JobReward {
    private long xp;
    private double money;

    public JobReward() {
        this.xp = 0;
        this.money = 0;
    }

    public JobReward(long xp, double money) {
        this.xp = xp;
        this.money = money;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }
}
