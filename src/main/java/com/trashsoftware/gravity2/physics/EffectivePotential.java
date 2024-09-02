package com.trashsoftware.gravity2.physics;

public class EffectivePotential {
    public final double[][] effectivePotential;
    public final double minEP;
    public double[][] lagrangePoints;

    EffectivePotential(double[][] effectivePotential, double minEP, double[]... lagrangePoints) {
        this.effectivePotential = effectivePotential;
        this.minEP = minEP;
        this.lagrangePoints = lagrangePoints;
    }
}