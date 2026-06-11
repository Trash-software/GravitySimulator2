package com.trashsoftware.gravity2.physics;

import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.gui.Vector3d;
import org.json.JSONObject;

import static com.trashsoftware.gravity2.physics.CelestialObject.REF_HEAT_CAPACITY;
import static com.trashsoftware.gravity2.physics.CelestialObject.STEFAN_BOLTZMANN_CONSTANT;

public abstract class RealObject implements Comparable<RealObject>, AbstractObject {

    protected double mass;
    protected double[] position;
    protected double[] velocity;

    protected String id;
    protected String shownName;
    protected boolean exist = true;

    protected String colorCode;

    protected double internalThermalEnergy;
    protected double emissivity = 0.95;

    protected transient double[] lastAcceleration;
    //    protected transient double[] orbitBasic;  // semi-major, eccentricity
    protected transient RealObject maxGravityObject;
    protected transient RealObject gravityMaster;  // the
    protected transient RealObject hillMaster;

    protected transient double hillRadius;
    protected double dieTime = -1;
    
    protected RealObject(String id,
                         double mass,
                         double[] position,
                         double[] velocity,
                         String colorCode) {
        if (position.length != velocity.length) {
            throw new IllegalArgumentException("You are in what dimensional world?");
        }
        this.id = id;
        this.mass = mass;
        this.position = position;
        this.velocity = velocity;
        this.colorCode = colorCode;

        lastAcceleration = new double[position.length];
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double[] getPosition() {
        return position;
    }

    @Override
    public double[] getVelocity() {
        return velocity;
    }

    public double getSpeed() {
        return VectorOperations.magnitude(velocity);
    }

    protected void setVelocityOverride(double[] velocity) {
        this.velocity = velocity;
    }

    public void setVelocity(double[] velocity) {
        System.arraycopy(velocity, 0, this.velocity, 0, velocity.length);
    }

    public void setVelocity(Vector3d velocity) {
        if (this.velocity.length != 3) {
            throw new IllegalArgumentException("setVelocity(Vector3d) only works for 3d simulation");
        }
        this.velocity[0] = velocity.x;
        this.velocity[1] = velocity.y;
        this.velocity[2] = velocity.z;
    }

    protected void setPositionOverride(double[] position) {
        this.position = position;
    }

    public void setPosition(double[] position) {
        System.arraycopy(position, 0, this.position, 0, position.length);
    }

    public RealObject getHillMaster() {
        return hillMaster;
    }

    public void setHillMaster(RealObject hillMaster) {
        this.hillMaster = hillMaster;
    }

    public RealObject getMaxGravityObject() {
        return maxGravityObject;
    }

    public RealObject getGravityMaster() {
        return gravityMaster;
    }

    public void setMaxGravityObject(RealObject maxGravityObject) {
        this.maxGravityObject = maxGravityObject;
    }

    @Override
    public RealObject getMaster() {
        return hillMaster;
    }

    @Override
    public int compareTo(RealObject o) {
        int massCmp = Double.compare(this.mass, o.mass);
        if (massCmp != 0) return massCmp;
        return this.id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public boolean isExist() {
        return exist;
    }

    public String getId() {
        return id;
    }

    public void setShownName(String shownName) {
        this.shownName = shownName;
    }

    public String getShownName() {
        return shownName;
    }

    public String getNameShowing() {
        if (shownName != null) return shownName;
        else return id;
    }

    public double getHillRadius() {
        return hillRadius;
    }

    public double getX() {
        return position[0];
    }

    public double getY() {
        return position[1];
    }

    public double getZ() {
        return position[2];
    }

    public String getColorCode() {
        return colorCode;
    }

    public abstract JSONObject toJson();
    
    public abstract double getAverageRadius();
    
    public abstract double getMajorRadius();
    
    public abstract double getVolume();
    
    protected abstract void absorbThermalEnergy(double absorbed, double timeStep);
    
    public abstract double getLuminosity();
    
    public boolean isEmittingLight() {
        return getLuminosity() > 0;
    }

    public abstract void emitThermalPower(double timeStep);

    /**
     * @return the distance that is considered as "too close" to this object
     */
    public abstract double proximityWarningDistance();

    public double transitionalKineticEnergy() {
        return 0.5 * mass * VectorOperations.dotProduct(velocity, velocity);
    }

    public void receiveLight(double[] sourcePos, double luminosity, double timeStep) {
        double albedo = estimateAlbedo();
        double distance = VectorOperations.distance(sourcePos, position);
        double received = calculateLightReceived(luminosity, distance);
        double absorbed = (1 - albedo) * received;
        // the above are all in 1 unit time

        absorbThermalEnergy(absorbed, timeStep);
    }

    public double calculateLightReceived(double luminosity, double distance) {
        double approxLightArea = Math.pow(getAverageRadius(), 2) * Math.PI;
        return fIncident(luminosity, distance) * approxLightArea;
    }

    public double estimateAlbedo() {
        int[] colorRGB = GuiUtils.stringToColorIntRGBA255(colorCode);

        double rgbAvg = (colorRGB[0] + colorRGB[1] + colorRGB[2]) / 3.0 / 256;
//        System.out.println("Albedo of " + colorCode + ": " + rgbAvg);
        return rgbAvg * 0.6;
    }

    protected static double fIncident(double luminosity, double distance) {
        return luminosity / (4 * Math.PI * Math.pow(distance, 2));
    }

    /**
     * @return the star-system level of this, 0 is the most relative star. If no star, the most central is 1
     */
    public int getLevelFromStar() {
        if (isEmittingLight()) return 0;

        if (hillMaster == null) {
            return 1;
        } else {
            return hillMaster.getLevelFromStar() + 1;
        }
    }

    /**
     * The following two emission does not relate to nuclear reaction
     */
    protected double thermalEmission(double currentSurfaceTemp, double surfaceArea) {
        return emissivity * STEFAN_BOLTZMANN_CONSTANT * Math.pow(currentSurfaceTemp, 4) * surfaceArea;
    }

    public double getInternalThermalEnergy() {
        return internalThermalEnergy;
    }

    public double getBodyAverageTemperature() {
        return internalThermalEnergy / mass / REF_HEAT_CAPACITY;
    }

    public static double calculateThermalEnergyByTemperature(double c, double mass, double k) {
        return c * mass * k;
    }

    public void destroy(double dieTime) {
        this.exist = false;
        this.dieTime = dieTime;
//        model.setVisible(false);
        // let them be garbage collected
//        model = null;
//        scale = null;
    }

    public double getDieTime() {
        return dieTime;
    }

    public double[] getLastRecordedAcceleration() {
        return lastAcceleration;
    }

    public double accelerationAlongMovingDirection() {
        double dot = VectorOperations.dotProduct(lastAcceleration, velocity);
        return dot / VectorOperations.magnitude(velocity);
    }
}
