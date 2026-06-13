package com.trashsoftware.gravity2.physics;

import com.trashsoftware.gravity2.utils.JsonUtil;
import org.json.JSONObject;

public class DustObject extends RealObject {

    public static final double MAXIMUM_DENSITY = 1e-6;
    public static final double MINIMUM_DENSITY = 1e-11;

    protected double radius;

    public DustObject(String id,
                      double mass,
                      double[] position,
                      double[] velocity,
                      String colorCode,
                      double radius) {
        super(id, mass, position, velocity, colorCode);

        this.radius = radius;
        
        validateDensity();
    }

    @Override
    public String toString() {
        return "DustObject{" + id + "}";
    }

    @Override
    public double getAverageRadius() {
        return radius;
    }

    @Override
    public double getMajorRadius() {
        return radius;
    }

    @Override
    public double rotationalKineticEnergy() {
        return 0;  // todo
    }

    @Override
    protected void absorbThermalEnergy(double absorbed, double timeStep) {
        internalThermalEnergy += absorbed * timeStep;
    }

    @Override
    public void emitThermalPower(double timeStep) {
        double curTemp = getTemperature();
        double surfaceArea = getApproxSurfaceArea();
        double emission = thermalEmission(curTemp, surfaceArea);
        internalThermalEnergy -= emission * timeStep;
        internalThermalEnergy = Math.max(0, internalThermalEnergy);
    }

    private double getApproxSurfaceArea() {
        return 4 * Math.PI * radius * radius;
    }

    public double getTemperature() {
        return internalThermalEnergy / (mass * BodyType.DUST_THERMAL_CAPACITY);
    }

    @Override
    public double getLuminosity() {
        return 0;
    }

    @Override
    public double proximityWarningDistance() {
        return 0;
    }

    @Override
    public double getVolume() {
        return 0.75 * Math.PI * radius * radius * radius;
    }

    public JSONObject toJson() {
        try {
            return JsonUtil.objectToJson(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void validateDensity() {
        if (getDensity() > MAXIMUM_DENSITY) {
            System.out.println("Old density: " + getDensity() + ", reducing");
            double volume = mass / MAXIMUM_DENSITY;
            radius = Math.cbrt((3 * volume) / (4 * Math.PI));
        } else if (getDensity() < MINIMUM_DENSITY) {
            System.out.println("Old density: " + getDensity() + ", increasing");
            double volume = mass / MINIMUM_DENSITY;
            radius = Math.cbrt((3 * volume) / (4 * Math.PI));
        }
    }
}
