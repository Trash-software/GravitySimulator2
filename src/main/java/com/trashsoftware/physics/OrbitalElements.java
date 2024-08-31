package com.trashsoftware.physics;
public class OrbitalElements {
    public double semiMajorAxis;  // Semi-major axis (a)
    public double eccentricity;   // Eccentricity (e)
    public double inclination;    // Inclination (i)
    public double ascendingNode;  // Longitude of ascending node (Ω)
    public double argumentOfPeriapsis; // Argument of periapsis (ω)
    public double trueAnomaly;    // True anomaly (ν)
    public double period;         // Orbital period (T)
    public double angularMomentum;

    public OrbitalElements(double a, double e, double i, double Ω, double ω, double ν, double T,
                           double h) {
        this.semiMajorAxis = a;
        this.eccentricity = e;
        this.inclination = i;
        this.ascendingNode = Ω;
        this.argumentOfPeriapsis = ω;
        this.trueAnomaly = ν;
        this.period = T;
        this.angularMomentum = h;
    }
    
    public boolean isElliptical() {
        return eccentricity < 0.999;
    }

    @Override
    public String toString() {
        return "OrbitSpecs{" +
                "semiMajorAxis=" + semiMajorAxis +
                ", eccentricity=" + eccentricity +
                ", inclination=" + inclination +
                ", ascendingNode=" + ascendingNode +
                ", argumentOfPeriapsis=" + argumentOfPeriapsis +
                ", trueAnomaly=" + trueAnomaly +
                ", period=" + period +
                '}';
    }
}
