package com.trashsoftware.gravity2.physics;

public enum BodyType {
    ICE(1500, 1, 900),
    TERRESTRIAL(900, 0.5, 2000),
    ICE_GIANT(13500, 5e3, 1.0),
    GAS_GIANT(14300, 1e4, 0.16),
    STAR(3e8, Double.NaN, 1e-12);
    
    public final double thermalSkinHeatCapacity;
    public final double thermalSkinDepth;
    public final double thermalSkinDensity;
    
    BodyType(double thermalSkinHeatCapacity, double thermalSkinDepth, double thermalSkinDensity) {
        this.thermalSkinHeatCapacity = thermalSkinHeatCapacity;
        this.thermalSkinDepth = thermalSkinDepth;
        this.thermalSkinDensity = thermalSkinDensity;
    }
    
    public BodyType merge(BodyType another) {
        int sn = this.ordinal();
        int on = another.ordinal();
        if (sn >= on) {
            return this;
        } else {
            return another;
        }
    }
    
    public BodyType disassemble(double childMass) {
        if (this == TERRESTRIAL || this == ICE) return this;
        if (this == GAS_GIANT) {
            if (childMass < SystemPresets.JUPITER_MASS * 0.1) return ICE;
            else return this;
        }
        if (this == ICE_GIANT) {
            if (childMass < SystemPresets.EARTH_MASS) return ICE;
            else return this;
        }
        if (this == STAR) {
            if (childMass < SystemPresets.JUPITER_MASS * 80) {
                return GAS_GIANT.disassemble(childMass);
            } else {
                return this;
            }
        }
        throw new IllegalArgumentException();
    }
}
