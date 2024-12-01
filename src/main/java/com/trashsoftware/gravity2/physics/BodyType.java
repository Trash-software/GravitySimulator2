package com.trashsoftware.gravity2.physics;

import com.trashsoftware.gravity2.presets.SystemPresets;
import com.trashsoftware.gravity2.utils.Util;

public enum BodyType {
    ICE(1500, 0.5, 900, false),
    TERRESTRIAL(900, 0.5, 2000, false),
    ICE_GIANT(13500, 5e3, 1.0, true),
    GAS_GIANT(14300, 1e4, 0.16, true),
    BROWN_DWARF(3e8, 1e5, 1e-6, true),
    STAR(3e8, 1e6, 1e-7, true);

    public final double thermalSkinHeatCapacity;
    public final double thermalSkinDepth;
    public final double thermalSkinDensity;
    public final boolean adaptiveDensity;

    BodyType(double thermalSkinHeatCapacity, 
             double thermalSkinDepth, 
             double thermalSkinDensity,
             boolean adaptiveDensity) {
        this.thermalSkinHeatCapacity = thermalSkinHeatCapacity;
        this.thermalSkinDepth = thermalSkinDepth;
        this.thermalSkinDensity = thermalSkinDensity;
        this.adaptiveDensity = adaptiveDensity;
    }

    public static BodyType simpleInfer(double mass) {
        if (mass <= SystemPresets.MOON_MASS * 0.5) return ICE;
        else if (mass <= SystemPresets.JUPITER_MASS * 0.03) return TERRESTRIAL;
        else if (mass <= SystemPresets.JUPITER_MASS * 13) return GAS_GIANT;
        else if (mass <= SystemPresets.JUPITER_MASS * 80) return BROWN_DWARF;
        else return STAR;
    }
    
    public static double massiveObjectDensity(double mass) {
        if (mass < SystemPresets.JUPITER_MASS * 13) {
            return gasGiantDensity(mass);
        } else if (mass < SystemPresets.JUPITER_MASS * 70.8) {
            // 70.8 is just a peak point of density
            return brownDwarfDensity(mass);
        } else {
            return starDensity(mass);
        }
    }

    public static double starDensity(double mass) {
        double relMass = mass / SystemPresets.SOLAR_MASS;
        double alpha;
        if (relMass < 0.5) {
            alpha = 0.8;
        } else if (relMass < 8.0) {
            if (relMass <= 1.0) {
                alpha = Util.linearMapping(0.5, 1.0, 0.8, 1.0, relMass);
            } else {
                alpha = Util.linearMapping(1.0, 8.0, 1.0, 0.8, relMass);
            }
        } else {
            double x = relMass - 7.0;  // x start from 1
            double multiplier = 0.8 - 0.5;
            alpha = 1 / Math.pow(x, 0.75) * multiplier + 0.5;
        }
        return Math.pow(relMass, 1 - alpha * 3) * SystemPresets.SOLAR_DENSITY;
    }

    public static double brownDwarfDensity(double mass) {
        double massJup = mass / SystemPresets.JUPITER_MASS;
        if (massJup < 13) {
            return 6000;
        } 
//        else if (massJup > 80) {
//            return 80000;
//        }

        // Smooth transition: density at 13 Jupiter masses (~6 g/cm³)
        double baseDensity = gasGiantDensity(13 * SystemPresets.JUPITER_MASS) / 1000;  // Continuity at 13 Jupiter masses (g/cm³)

        // Approximate density increase for brown dwarfs (non-linear)
        double densityGcm3;
        if (massJup <= 30) {
            // For masses between 13 and 30 Jupiter masses, use a moderate increase
            densityGcm3 = baseDensity + (15.0 - baseDensity) * Math.pow((massJup - 13.0) / (30.0 - 13.0), 1.5);
        } else {
            // For masses > 30 Jupiter masses, density increases more rapidly
            densityGcm3 = 15.0 + (100.0 - 15.0) * Math.pow((massJup - 30.0) / (80.0 - 30.0), 3.0);
        }

        // Convert density from g/cm³ to kg/m³ (1 g/cm³ = 1000 kg/m³)
        return densityGcm3 * 1000;  // Return the approximate density in kg/m³
    }

    public static double gasGiantDensity(double mass) {
        double massJup = mass / SystemPresets.JUPITER_MASS;
        if (massJup < 0.03) return 200;

        // Constants for the non-linear approximation of gas giant density
        // Lower masses (~0.03 M_Jup) have densities ~0.2 g/cm³, higher masses (~13 M_Jup) have ~6 g/cm³
        double baseDensity = 0.2;  // Density at 0.03 Jupiter masses (g/cm³)
        double jupiterDensity = 1.33; // Density at 1 Jupiter mass (g/cm³)
        double maxDensity = 6.0;   // Approximate density at 13 Jupiter masses (g/cm³)

        // Use a non-linear interpolation formula to model the density curve
        double densityGcm3;
        if (massJup <= 1) {
            // For masses <= 1 Jupiter mass, use a quadratic interpolation
            densityGcm3 = baseDensity + (jupiterDensity - baseDensity) * Math.pow(massJup, 0.7);
        } else {
            // For masses > 1 Jupiter mass, use a different curve to model the stronger compression
            densityGcm3 = jupiterDensity + (maxDensity - jupiterDensity) * Math.pow((massJup - 1.0) / (13.0 - 1.0), 2.5);
        }

        // Convert density from g/cm³ to kg/m³ (1 g/cm³ = 1000 kg/m³)
        return densityGcm3 * 1000;  // Return the approximate density in kg/m³
    }

    public BodyType merge(BodyType another, double newMass) {
        int sn = this.ordinal();
        int on = another.ordinal();
        if (sn >= on) {
            if (newMass >= SystemPresets.JUPITER_MASS * 80) {
                return STAR;
            } else if (newMass >= SystemPresets.JUPITER_MASS * 13) {
                return BROWN_DWARF;
            }
            return this;
        } else {
            return another.merge(this, newMass);
        }
    }

    public BodyType disassemble(double childMass) {
        if (this == TERRESTRIAL || this == ICE) return this;
        if (this == GAS_GIANT) {
            if (childMass < SystemPresets.JUPITER_MASS * 0.03) return ICE;
            else return this;
        }
        if (this == ICE_GIANT) {
            if (childMass < SystemPresets.EARTH_MASS) return ICE;
            else return this;
        }
        if (this == BROWN_DWARF) {
            if (childMass < SystemPresets.JUPITER_MASS * 13) {
                return GAS_GIANT.disassemble(childMass);
            } else {
                return this;
            }
        }
        if (this == STAR) {
            if (childMass < SystemPresets.JUPITER_MASS * 80) {
                return BROWN_DWARF.disassemble(childMass);
            } else {
                return this;
            }
        }
        throw new IllegalArgumentException();
    }
    
    private double inferAvgRadius(double mass) {
        double density = massiveObjectDensity(mass);
        double volume = mass / density;

        return Math.pow(3 * volume / (4 * Math.PI), 1.0 / 3);
    }

    public static void main(String[] args) {
        var a = simpleInfer(SystemPresets.JUPITER_MASS * 12.5);
        System.out.println(a.inferAvgRadius(SystemPresets.JUPITER_MASS * 12.5) / SystemPresets.JUPITER_RADIUS_KM / 1000);
        
        System.out.println(gasGiantDensity(SystemPresets.JUPITER_MASS * 12.5));
        System.out.println(brownDwarfDensity(SystemPresets.JUPITER_MASS * 70.8));
        System.out.println(starDensity(SystemPresets.JUPITER_MASS * 70.8));
    }
}
