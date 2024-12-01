package com.trashsoftware.gravity2.physics.status;

import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.presets.SystemPresets;

public class Star extends Status {
    
    double luminosity;
    
    public Star(CelestialObject co, double luminosity) {
        super(co);
        
        this.luminosity = luminosity;
    }

    public double getLuminosity() {
        return luminosity;
    }

    public void setLuminosity(double luminosity) {
        this.luminosity = luminosity;
    }

    public double getEmissionColorTemperature() {
        double lumin = getLuminosity();
        if (lumin == 0) return 0;
        double radius = co.getAverageRadius();
        return computeEmissionColorTemperature(lumin, radius);
    }

    public double getCoronaTemperature() {
        double effTemp = getEmissionColorTemperature();
        if (effTemp == 0) return 0;
        return computeCoronaTemperature(effTemp);
    }

    public double getStellarWindSpeed() {
        double coronaTemp = getCoronaTemperature();
        if (coronaTemp == 0) return 0;
        return Math.sqrt(CelestialObject.BOLTZMANN_CONSTANT * coronaTemp / CelestialObject.PROTON_MASS) * 7.5;
//        double vEsc = co.orbitSpeedOfN(G, 2);
//        return Math.min(base, vEsc);
    }
    
    public double[] estimateHabitableZone() {
        double lSun = getLuminosity() / SystemPresets.SOLAR_LUMINOSITY;
//        double tEff = getEmissionColorTemperature() / 5770.0;
//        System.out.println("lSun: " + lSun + ", tEff: " + tEff);
//        double effTempAdj = Math.pow(tEff, 4);
        double dInner = Math.sqrt(lSun / 1.1);
        double dOuter = Math.sqrt(lSun / 0.53);
//        System.out.println("Inner: " + dInner + ", outer: " + dOuter);
        return new double[]{dInner * SystemPresets.AU, dOuter * SystemPresets.AU};
    }

    public static double computeEmissionColorTemperature(double luminosity, double radius) {
        double divisor = 4 * Math.PI * radius * radius * CelestialObject.STEFAN_BOLTZMANN_CONSTANT;
        return Math.pow(luminosity / divisor, 0.25);
    }

    public static double computeCoronaTemperature(double effTemp) {
        return 1e6 * Math.sqrt(effTemp / 5770);
    }
}
