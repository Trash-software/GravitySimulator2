package com.trashsoftware.gravity2.physics;

public class FullOrbitSpec {
    
    public final OrbitalElements elements;
    public final AbstractObject child;
    public final double massInvolved;
    public final double[] barycenter;
    
    public FullOrbitSpec(OrbitalElements elements,
                         AbstractObject child,
                         double massInvolved,
                         double[] barycenter) {
        this.elements = elements;
        this.child = child;
        this.massInvolved = massInvolved;
        this.barycenter = barycenter;
    }
}
