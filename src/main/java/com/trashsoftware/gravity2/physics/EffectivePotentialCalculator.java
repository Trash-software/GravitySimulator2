package com.trashsoftware.gravity2.physics;

public class EffectivePotentialCalculator {
    
    public final CelestialObject master, object;
    private final double G;
    public final OrbitalElements oe;
    private final double angularVel;
    public final double[] barycenter;
    
    public EffectivePotentialCalculator(double G, 
                                        CelestialObject master, 
                                        CelestialObject object,
                                        OrbitalElements oe,
                                        double[] barycenter) {
        this.G = G;
        this.master = master;
        this.object = object;
        this.oe = oe;

        angularVel = calculateAngularVelocity2d(master.position, object.position,
                master.velocity, object.velocity);
        this.barycenter = barycenter;
    }

    private static double calculateAngularVelocity2d(double[] r1, double[] r2, double[] v1, double[] v2) {
        // Relative position vector r21 = r2 - r1
        double r21x = r2[0] - r1[0];
        double r21y = r2[1] - r1[1];

        // Relative velocity vector v21 = v2 - v1
        double v21x = v2[0] - v1[0];
        double v21y = v2[1] - v1[1];

        // Compute the distance rC (magnitude of r21)
        double rC = Math.sqrt(r21x * r21x + r21y * r21y);

        // Compute the cross product (magnitude of angular momentum per unit mass)
        double h = r21x * v21y - r21y * v21x;

        // Compute the angular velocity

        return h / (rC * rC);
    }

    public double compute(
            double x,
            double y) {
        double r1 = Math.sqrt(Math.pow(x - master.getX(), 2) + Math.pow(y - master.getY(), 2));
        double r2 = Math.sqrt(Math.pow(x - object.getX(), 2) + Math.pow(y - object.getY(), 2));

        // Gravitational potentials
        double Vg1 = -G * master.mass / r1;
        double Vg2 = -G * object.mass / r2;

        // Centrifugal potential (relative to the barycenter)
        double rC = Math.sqrt(Math.pow(x - barycenter[0], 2) + Math.pow(y - barycenter[1], 2));

        // angular velocity
//        double angularVel = oe.angularMomentum / (rC * rC);

        double Vc = -0.5 * Math.pow(angularVel * rC, 2);

        return Vg1 + Vg2 + Vc;
    }
}
