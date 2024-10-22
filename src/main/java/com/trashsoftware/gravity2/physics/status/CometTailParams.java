package com.trashsoftware.gravity2.physics.status;

public class CometTailParams {
    
    public final boolean isIon;
    public final double tailLength;
    public final double tailDensity;
    public final double[] velocity;
//    public final double[] initDirection;
    public final double[] directionChange;
    
    CometTailParams(boolean isIon, double tailLength, double tailDensity,
                    double[] velocity, double[] directionChange) {
        this.isIon = isIon;
        this.tailLength = tailLength;
        this.tailDensity = tailDensity;
//        this.initDirection = initDirection;
        this.velocity = velocity;
        this.directionChange = directionChange;
    }
}
