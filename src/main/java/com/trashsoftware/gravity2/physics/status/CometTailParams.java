package com.trashsoftware.gravity2.physics.status;

public class CometTailParams {
    
    public final boolean isIon;
    public final double tailLength;
    public final double tailDensity;
    public final double[] velocity;
//    public final double[] initDirection;
    public final double[] directionChange;
    public final double timeSteps;
    
    CometTailParams(boolean isIon, double tailLength, double tailDensity,
                    double[] velocity, double[] directionChange, double timeSteps) {
        this.isIon = isIon;
        this.tailLength = tailLength;
        this.tailDensity = tailDensity;
//        this.initDirection = initDirection;
        this.velocity = velocity;
        this.directionChange = directionChange;
        this.timeSteps = timeSteps;
    }
}
