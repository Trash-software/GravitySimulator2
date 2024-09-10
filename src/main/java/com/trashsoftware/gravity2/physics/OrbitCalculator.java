package com.trashsoftware.gravity2.physics;

public class OrbitCalculator {
    public static double[] calculateBarycenter(AbstractObject star1, AbstractObject star2) {
        int dimension = star1.getPosition().length;
        double totalMass = star1.getMass() + star2.getMass();
        double[] barycenter = new double[dimension];

        for (int i = 0; i < dimension; i++) {
            barycenter[i] = (star1.getPosition()[i] * star1.getMass() + star2.getPosition()[i] * star2.getMass()) / totalMass;
        }

        return barycenter;
    }

    /**
     * @return {semi-major, eccentricity}
     */
    public static double[] computeBasic(CelestialObject small,
                                        double[] barycenter,
                                        double totalMass,
                                        double[] v,
                                        double G) {
        double mu = G * totalMass;
        double[] r = VectorOperations.subtract(small.position, barycenter);

        double[] h = VectorOperations.crossProduct(r, v); // Specific angular momentum
//        double hMag = VectorOperations.magnitude(h);

        double epsilon = (Math.pow(VectorOperations.magnitude(v), 2) / 2) - (mu / VectorOperations.magnitude(r));

        double a = -mu / (2 * epsilon);

        double[] eVec = VectorOperations.subtract(VectorOperations.scale(VectorOperations.crossProduct(v, h), 1 / mu), VectorOperations.scale(r, 1 / VectorOperations.magnitude(r)));
        double e = VectorOperations.magnitude(eVec);
//        double T = 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
        return new double[]{a, e};
    }

    public static OrbitalElements computeOrbitSpecs3d(double[] relativePosition,
                                                      double[] relativeVelocity,
                                                      double totalMass,
                                                      double G) {
        double[] r = relativePosition;
        double[] v = relativeVelocity;
        double mu = G * totalMass;

        double[] h = VectorOperations.crossProduct(r, v); // Specific angular momentum
        double hMag = VectorOperations.magnitude(h);

        double epsilon = (Math.pow(VectorOperations.magnitude(v), 2) / 2) - (mu / VectorOperations.magnitude(r));

        double a = -mu / (2 * epsilon);

        double[] eVec = VectorOperations.subtract(VectorOperations.scale(VectorOperations.crossProduct(v, h), 1 / mu), VectorOperations.scale(r, 1 / VectorOperations.magnitude(r)));
        double e = VectorOperations.magnitude(eVec);

        double i = Math.acos(VectorOperations.clamp(h[2] / hMag, -1, 1));

        double[] n = VectorOperations.crossProduct(new double[]{0, 0, 1}, h);
        double nMag = VectorOperations.magnitude(n);

        double Omega = Math.atan2(n[1], n[0]);

        double omega = Math.acos(VectorOperations.clamp(VectorOperations.dotProduct(n, eVec) / (nMag * e), -1, 1));
        if (eVec[2] < 0) {
            omega = 2 * Math.PI - omega;
        }

        double nu = Math.acos(VectorOperations.clamp(VectorOperations.dotProduct(eVec, r) / (e * VectorOperations.magnitude(r)), -1, 1));
        if (VectorOperations.dotProduct(r, v) < 0) {
            nu = 2 * Math.PI - nu;
        }

        double T = 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);

        return new OrbitalElements(a, 
                e, 
                Math.toDegrees(i), 
                Math.toDegrees(Omega), 
                Math.toDegrees(omega), 
                Math.toDegrees(nu), 
                T,
                hMag);
    }

    public static OrbitalElements computeOrbitSpecsPlanar(AbstractObject child,
                                                          double[] relativeVelocity,
                                                          double[] barycenter,
                                                          double totalMass,
                                                          double G) {
        double[] r = VectorOperations.subtract(child.getPosition(), barycenter);
        double mu = G * totalMass;

        double[] h = VectorOperations.crossProduct(r, relativeVelocity); // Specific angular momentum
        double hMag = VectorOperations.magnitude(h);

        double epsilon = (Math.pow(VectorOperations.magnitude(relativeVelocity), 2) / 2) - (mu / VectorOperations.magnitude(r));

        double a = -mu / (2 * epsilon);

        double[] eVec = VectorOperations.subtract(VectorOperations.scale(VectorOperations.crossProduct(relativeVelocity, h), 1 / mu), VectorOperations.scale(r, 1 / VectorOperations.magnitude(r)));
        double e = VectorOperations.magnitude(eVec);

        // For 2D, the inclination is always 0
        double i = 0;

        // For 2D, the node vector is always along the z-axis
//        double[] n = VectorOperations.crossProduct(new double[]{0, 0, 1}, h);
//        double nMag = VectorOperations.magnitude(n);
//        System.out.println(Arrays.toString(n) + " " + nMag);

        double Omega = 0;

        double omega;
//        if (nMag != 0) {
//            omega = Math.acos(VectorOperations.clamp(VectorOperations.dotProduct(n, eVec) / (nMag * e), -1, 1));
//            if (eVec[2] < 0) {
//                omega = 2 * Math.PI - omega;
//            }
//        } else {
            omega = Math.atan2(eVec[1], eVec[0]);
//        }
        omega += Math.PI;

        double nu = Math.atan2(r[1], r[0]) - omega;
        if (nu < 0) {
            nu += 2 * Math.PI;
        }

        double T = 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);

        return new OrbitalElements(a, 
                e, 
                Math.toDegrees(i), 
                Math.toDegrees(Omega), 
                Math.toDegrees(omega), 
                Math.toDegrees(nu), 
                T,
                hMag);
    }

    public static OrbitalElements computeOrbitSpecsCorrect(AbstractObject star,
                                                           double[] relativeVelocity,
                                                           double[] barycenter,
                                                           double totalMass,
                                                           double G) {
        double[] rVec = VectorOperations.subtract(star.getPosition(), barycenter);
        double[] vVec = relativeVelocity;
        double mu = G * totalMass;

        double r = VectorOperations.magnitude(rVec);
        double v = VectorOperations.magnitude(vVec);

        double[] rVecOverR = VectorOperations.scale(rVec, 1 / r);

        double vr = VectorOperations.dotProduct(
                rVecOverR,
                vVec
        );

        double vp = Math.sqrt(Math.pow(v, 2) - Math.pow(vr, 2));

        // 来自gpt的2行
        double epsilon = (Math.pow(VectorOperations.magnitude(relativeVelocity), 2) / 2) - (mu / r);
        double a = -mu / (2 * epsilon);

        double[] hVec = VectorOperations.crossProduct(rVec, vVec);
        double h = VectorOperations.magnitude(hVec);

        double i = Math.acos(hVec[2] / h);

        double[] k = new double[]{0, 0, 1};
        double[] nVec = VectorOperations.crossProduct(k, hVec);
        double n = VectorOperations.magnitude(nVec);

        double Omega = 2 * Math.PI - Math.acos(nVec[0] / n);

        double[] eVec = VectorOperations.subtract(
                VectorOperations.scale(
                        VectorOperations.crossProduct(vVec, hVec), 1 / mu),
                rVecOverR);
        double e = VectorOperations.magnitude(eVec);

        double omega = 2 * Math.PI - Math.acos(
                VectorOperations.dotProduct(nVec, eVec) / (n * e)
        );

        double nu = Math.acos(VectorOperations.dotProduct(rVecOverR, VectorOperations.scale(eVec, 1 / e)));

        double T = 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);

        return new OrbitalElements(
                a,
                e,
                Math.toDegrees(i),
                Math.toDegrees(Omega),
                Math.toDegrees(omega),
                Math.toDegrees(nu),
                T,
                h
        );
    }
}
