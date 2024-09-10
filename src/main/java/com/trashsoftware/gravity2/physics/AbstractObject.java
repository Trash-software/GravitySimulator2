package com.trashsoftware.gravity2.physics;

public interface AbstractObject {
    
    double getMass();
    
    double[] getPosition();
    
    double[] getVelocity();
    
    AbstractObject getMaster();
    
    default double[] getEclipticPlaneNormal() {
        AbstractObject hillMaster = getMaster();
        if (hillMaster == null) return new double[]{0, 0, 1};
        double[] relPos = VectorOperations.subtract(getPosition(), hillMaster.getPosition());
        double[] relVel = VectorOperations.subtract(getVelocity(), hillMaster.getVelocity());
        return VectorOperations.normalize(
                VectorOperations.crossProduct(relPos, relVel)
        );
    }
}
