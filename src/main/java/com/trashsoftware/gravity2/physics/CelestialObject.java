package com.trashsoftware.gravity2.physics;

import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.physics.status.Comet;
import com.trashsoftware.gravity2.physics.status.Star;
import com.trashsoftware.gravity2.physics.status.Status;
import com.trashsoftware.gravity2.presets.SystemPresets;
import com.trashsoftware.gravity2.utils.JsonUtil;
import com.trashsoftware.gravity2.utils.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class CelestialObject implements Comparable<CelestialObject>, AbstractObject {

    public static final double REF_HEAT_CAPACITY = 14300.0;
    public static final double STEFAN_BOLTZMANN_CONSTANT = 5.670374419e-8;
    public static final double BOLTZMANN_CONSTANT = 1.380649e-23;
    public static final double PROTON_MASS = 1.6726e-27;

    protected double[] position;
    protected double[] velocity;
    protected double[] rotationAxis;
    protected double angularVelocity;
    protected double mass;
    protected double tidalLoveNumber = 0.15;
    protected double dissipationFunction = 100;
    protected double emissivity = 0.95;
    protected BodyType bodyType;
    protected double equatorialRadius, polarRadius;
    protected double internalThermalEnergy;
    protected double surfaceThermalEnergy;
    protected String id;
    protected String shownName;
    
    protected Status status;

    private boolean exist = true;

    /**
     * Status
     */
    private double rotationAngle;
    private double lastBreakTime;
    private double timeInsideRocheLimit;  // the time steps of this inside other's roche limit
    protected double dieTime = -1;
    private int debrisLevel;

    private String colorCode;
    private String lightColorCode;
    private final String texturePath;

    protected transient double[] lastAcceleration;
    //    protected transient double[] orbitBasic;  // semi-major, eccentricity
    protected transient CelestialObject maxGravityObject;
    protected transient CelestialObject gravityMaster;  // the
    protected transient CelestialObject hillMaster;
    protected transient double hillRadius;
    protected transient double possibleRocheLimit;
    protected transient double approxRocheLimit;
//    protected transient double lastLuminosity;

    CelestialObject(String id,
                    BodyType bodyType,
                    double mass,
                    double equatorialRadius,
                    double polarRadius,
                    double[] position,
                    double[] velocity,
                    double[] rotationAxis,
                    double angularVelocity,
                    String colorCode,
                    String texturePath,
                    double internalAvgTemp) {

        if (position.length != velocity.length) {
            throw new IllegalArgumentException("You are in what dimensional world?");
        }
        lastAcceleration = new double[position.length];

        this.id = id;
        this.bodyType = bodyType;
        this.mass = mass;
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.position = position;
        this.velocity = velocity;
        this.colorCode = colorCode;
        this.rotationAxis = VectorOperations.normalize(rotationAxis);
        this.angularVelocity = angularVelocity;
        this.internalThermalEnergy = calculateThermalEnergyByTemperature(REF_HEAT_CAPACITY, mass, internalAvgTemp);

        this.texturePath = texturePath;

        possibleRocheLimit = Simulator.computeMaxRocheLimit(this);
//        approxRocheLimit = Simulator.computeRocheLimitLiquid(this);
        approxRocheLimit = Simulator.computeRocheLimitSolid(this);
        
//        updateLuminosity();
        updateStatus(true);
    }

    public static CelestialObject create2d(String name,
                                           double mass,
                                           double radius,
                                           double x,
                                           double y,
                                           double vx,
                                           double vy,
                                           String colorCode) {
        return createNd(name,
                mass,
                radius,
                2,
                new double[]{x, y},
                new double[]{vx, vy},
                colorCode);
    }

    public static CelestialObject create2d(String id,
                                           double mass,
                                           double radius,
                                           double x,
                                           double y,
                                           String colorCode) {
        return create2d(id,
                mass,
                radius,
                x,
                y,
                0,
                0,
                colorCode);
    }

    public static CelestialObject createNd(String id,
                                           double mass,
                                           double radius,
                                           int dim,
                                           String colorCode) {
        return createNd(id,
                mass,
                radius,
                dim,
                new double[dim],
                new double[dim],
                colorCode);
    }

    public static CelestialObject createReal(String id,
                                             BodyType bodyType,
                                             double mass,
                                             double equatorialRadius,
                                             double polarRadius,
                                             int dim,
                                             double[] position,
                                             double[] velocity,
                                             double[] axis,
                                             double angularVelocity,
                                             String colorCode,
                                             String texturePath,
                                             double internalAvgTemp) {
        CelestialObject co = new CelestialObject(
                id,
                bodyType,
                mass,
                equatorialRadius,
                polarRadius,
                new double[dim],
                new double[dim],
                axis,
                angularVelocity,
                colorCode,
                texturePath,
                internalAvgTemp
        );
        // argument pos/vel can be shorter than dim
        System.arraycopy(position, 0, co.position, 0, position.length);
        System.arraycopy(velocity, 0, co.velocity, 0, velocity.length);
        return co;
    }

    public static CelestialObject create3d(String id,
                                           double mass,
                                           double radius,
                                           double[] position,
                                           double[] velocity,
                                           String colorCode) {
        return createNd(id, mass, radius, 3, position, velocity, colorCode);
    }

    public static CelestialObject createNd(String id,
                                           double mass,
                                           double radius,
                                           int dim,
                                           double[] position,
                                           double[] velocity,
                                           String colorCode) {
        double[] axis = new double[]{0, 0, 1};
        CelestialObject co = new CelestialObject(
                id,
                BodyType.simpleInfer(mass),
                mass,
                radius,
                radius,
                new double[dim],
                new double[dim],
                axis,
                1e-8,
                colorCode,
                null,
                5000
        );
        // argument pos/vel can be shorter than dim
        System.arraycopy(position, 0, co.position, 0, position.length);
        System.arraycopy(velocity, 0, co.velocity, 0, velocity.length);

        return co;
    }

    public static CelestialObject fromJson(JSONObject json) {
        JSONArray positionArr = json.getJSONArray("position");
        JSONArray velocityArr = json.getJSONArray("velocity");
        JSONArray axisArr = json.getJSONArray("rotationAxis");

        double[] position = JsonUtil.jsonArrayToDoubleArray(positionArr);
        double[] velocity = JsonUtil.jsonArrayToDoubleArray(velocityArr);
        double[] rotationAxis = JsonUtil.jsonArrayToDoubleArray(axisArr);
        String texturePath = null;
        if (json.has("texturePath")) {
            texturePath = json.getString("texturePath");
            if ("null".equals(texturePath)) texturePath = null;
        }

//        int dim = position.length;

        CelestialObject co = new CelestialObject(
                json.getString("id"),
                BodyType.valueOf(json.getString("bodyType")),
                json.getDouble("mass"),
                json.getDouble("equatorialRadius"),
                json.getDouble("polarRadius"),
                position,
                velocity,
                rotationAxis,
                json.getDouble("angularVelocity"),
                json.getString("colorCode"),
                texturePath,
                0
        );
        
        co.shownName = JsonUtil.optString(json, "shownName", null);

        co.exist = json.getBoolean("exist");
        co.lightColorCode = JsonUtil.optString(json, "lightColorCode", null);
        co.debrisLevel = json.getInt("debrisLevel");
        for (String attr : new String[]{
                "tidalLoveNumber",
                "dissipationFunction",
                "emissivity",
                "surfaceThermalEnergy",
                "internalThermalEnergy",
                "rotationAngle",
                "lastBreakTime",
                "dieTime",
                "timeInsideRocheLimit"
        }) {
            try {
                CelestialObject.class.getDeclaredField(attr).set(co, json.getDouble(attr));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        return co;
    }

    public JSONObject toJson() {
        try {
            return JsonUtil.objectToJson(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
//        JSONObject json = new JSONObject();
//
//        json.put("name", name);
//        json.put("mass", mass);
//        json.put("equatorialRadius", equatorialRadius);
//        json.put("polarRadius", polarRadius);
//        json.put("exist", exist);
//        json.put("colorCode", colorCode);
//        json.put("lightColorCode", lightColorCode);
//        json.put("texturePath", texturePath);
//        json.put("position", new JSONArray(position));
//        json.put("velocity", new JSONArray(velocity));
//        json.put("rotationAxis", new JSONArray(rotationAxis));
//        json.put("angularVelocity", angularVelocity);
//        json.put("rotationAngle", rotationAngle);
//        json.put("internalThermalEnergy", internalThermalEnergy);
//        json.put("tidalLoveNumber", tidalLoveNumber);
//
//        return json;
    }

    public String getId() {
        return id;
    }

//    public void setColor(ColorRGBA color) {
//        this.color = color;
//
//        PhongMaterial material = new PhongMaterial();
//        material.setDiffuseColor(color);
//        model.setMaterial(material);
//    }
    
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
    
    public void forceSetBasics(double mass, double avgRadius) {
        double internalTemp = getBodyAverageTemperature();
        
        double adjustRatio = avgRadius / getAverageRadius();
        this.mass = mass;
        equatorialRadius *= adjustRatio;
        polarRadius *= adjustRatio;
        
        internalThermalEnergy = calculateThermalEnergyByTemperature(REF_HEAT_CAPACITY, mass, internalTemp);
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setLightColorCode(String lightColorCode) {
        this.lightColorCode = lightColorCode;
    }

    public String getLightColorCode() {
        return lightColorCode;
    }

    public void setTidalConstants(double tidalLoveNumber, double dissipationFunction) {
        this.tidalLoveNumber = tidalLoveNumber;
        this.dissipationFunction = dissipationFunction;
    }

    public void setRotationAngle(double rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    protected void updateRotation(double timeSteps) {
        double deg = Math.toDegrees(angularVelocity) * timeSteps;

        rotationAngle += deg;
        if (rotationAngle >= 360) rotationAngle -= 360;
        else if (rotationAngle < 0) rotationAngle += 360;
    }

    public double transitionalKineticEnergy() {
        return 0.5 * mass * VectorOperations.dotProduct(velocity, velocity);
    }

    public double rotationalKineticEnergy() {
        return 0.5 * momentOfInertiaRot() * angularVelocity * angularVelocity;
    }

    public double momentOfInertiaRot() {
        return momentOfInertiaRot(mass, equatorialRadius);
    }

    public double shapeFactor() {
        if (polarRadius > equatorialRadius) throw new RuntimeException();
        if (polarRadius == equatorialRadius) return 1.0;
        double e = Math.sqrt(1 - (polarRadius * polarRadius) / (equatorialRadius * equatorialRadius));
        return Math.asin(e) / e;
    }

    private static double momentOfInertiaRot(double mass, double equatorialRadius) {
        return 0.4 * mass * equatorialRadius * equatorialRadius;
    }

    public double[] angularMomentum() {
        return VectorOperations.scale(rotationAxis, momentOfInertiaRot() * angularVelocity);
    }

    private double computeNewAngularVelocity(double[] totalAngularMomentum,
                                             double totalMass,
                                             double newEquatorialRadius) {
        double momentOfInertia = momentOfInertiaRot(totalMass, newEquatorialRadius); // Adjust for new mass and shape
        return VectorOperations.magnitude(totalAngularMomentum) / momentOfInertia;
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

    public double estimateAlbedo() {
        int[] colorRGB = GuiUtils.stringToColorIntRGBA255(colorCode);

        double rgbAvg = (colorRGB[0] + colorRGB[1] + colorRGB[2]) / 3.0 / 256;
//        System.out.println("Albedo of " + colorCode + ": " + rgbAvg);
        return rgbAvg * 0.6;
    }

    private double thermalSkinMass(double surfaceArea) {
        double thickness = bodyType.thermalSkinDepth;
        return thickness * surfaceArea * bodyType.thermalSkinDensity;
    }

    private double getSurfaceTemperature(double surfaceArea) {
        double skinMass = thermalSkinMass(surfaceArea);
        return surfaceThermalEnergy / (skinMass * bodyType.thermalSkinHeatCapacity);
    }

    public double getSurfaceTemperature() {
        return getSurfaceTemperature(getSurfaceArea());
    }

    public void forceSetSurfaceTemperature(double kelvin) {
        double surfaceArea = getSurfaceArea();
        double skinMass = thermalSkinMass(surfaceArea);
        surfaceThermalEnergy = kelvin * skinMass * bodyType.thermalSkinHeatCapacity;
    }

    private static double fIncident(double luminosity, double distance) {
        return luminosity / (4 * Math.PI * Math.pow(distance, 2));
    }

    public double calculateLightReceived(double luminosity, double distance) {
        double approxLightArea = Math.pow(getAverageRadius(), 2) * Math.PI;
        return fIncident(luminosity, distance) * approxLightArea;
    }

    public void receiveLight(double[] sourcePos, double luminosity, double timeStep) {
        double albedo = estimateAlbedo();
        double distance = VectorOperations.distance(sourcePos, position);
        double received = calculateLightReceived(luminosity, distance);
        double absorbed = (1 - albedo) * received;
        // the above are all in 1 unit time

        surfaceThermalEnergy += absorbed * timeStep;
    }

    public void emitThermalPower(double timeStep) {
        double curTemp = getSurfaceTemperature();
        double surfaceArea = getSurfaceArea();
        double emission = thermalEmission(curTemp, surfaceArea);
        surfaceThermalEnergy -= emission * timeStep;
        surfaceThermalEnergy = Math.max(0, surfaceThermalEnergy);
    }
    
    public double vapor(Comet comet, double timeStep, List<Star> lightSources) {
        // todo
        double sa = getSurfaceArea();
        double mlr = comet.massLossRate(getSurfaceTemperature(sa), sa);
//        this.mass -= mlr * timeStep;
        return mlr;
    }

    /**
     * The following two emission does not relate to nuclear reaction
     */
    private double thermalEmission(double currentSurfaceTemp, double surfaceArea) {
        return emissivity * STEFAN_BOLTZMANN_CONSTANT * Math.pow(currentSurfaceTemp, 4) * surfaceArea;
    }

    public double getThermalEmission() {
        double surfaceArea = getSurfaceArea();
        return thermalEmission(getSurfaceTemperature(surfaceArea), surfaceArea);
    }

    public Status getStatus() {
        return status;
    }
    
    public double getLuminosity() {
        if (status instanceof Star star) return star.getLuminosity();
        return 0;
    }

    public boolean isEmittingLight() {
        return getLuminosity() > 0;
    }
    
    protected void updateStatus(boolean updateStars) {
        if (getMass() < SystemPresets.JUPITER_MASS * 80) {
            if (bodyType == BodyType.ICE) {
                if (getSurfaceTemperature() > 150) {
                    // todo: sublimation temperature
                    
                    if (status instanceof Comet comet) {
                        
                    } else {
                        status = new Comet(this);
                    }
                } else {
                    status = null;
                }
            } else {
                status = null;
            }
        } else if (updateStars) {
            // todo: assume is main sequence
            double ratio = Math.pow(getMass() / SystemPresets.SOLAR_MASS, 3.5);
            double luminosity = ratio * SystemPresets.SOLAR_LUMINOSITY;
            if (status instanceof Star star) {
                star.setLuminosity(luminosity);
            } else {
                status = new Star(this, luminosity);
            }
        }
    }
    
//    protected double updateLuminosity() {
//        if (getMass() < SystemPresets.JUPITER_MASS * 80) {
//            lastLuminosity = 0;
//        } else {
//            // todo: assume is main sequence
//            double ratio = Math.pow(getMass() / SystemPresets.SOLAR_MASS, 3.5);
//            lastLuminosity = ratio * SystemPresets.SOLAR_LUMINOSITY;
//        }
//        return lastLuminosity;
//    }
    
    public double orbitSpeedOfN(double G, double n) {
        return Math.sqrt(n * G * getMass() / getAverageRadius());
    }

    public BodyType getBodyType() {
        return bodyType;
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

    @Override
    public double[] getVelocity() {
        return velocity;
    }

    @Override
    public double[] getPosition() {
        return position;
    }

    public double[] getLastRecordedAcceleration() {
        return lastAcceleration;
    }

    public double accelerationAlongMovingDirection() {
        double dot = VectorOperations.dotProduct(lastAcceleration, velocity);
        return dot / VectorOperations.magnitude(velocity);
    }

    public double getRotationPeriod() {
        return 2 * Math.PI / angularVelocity;
    }

    public double getAngularVelocity() {
        return angularVelocity;
    }

    public double getAxisTiltAngle() {
        // todo: for showing, it's better to consider the axis tilt vs its orbit pane

        // Step 1: Calculate the magnitude of the vector (x, y, z)
        double magnitude = VectorOperations.magnitude(rotationAxis);

        // Step 2: Compute the dot product with the Z-axis (0, 0, 1)
        double dotProduct = rotationAxis[2]; // Since dot product with (0, 0, 1) is just the z-component

        // Step 3: Calculate the angle using acos (result will be in radians)
        double angleRadians = Math.acos(dotProduct / magnitude);

        // Convert the angle to degrees
        return Math.toDegrees(angleRadians);
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

    protected void setPositionOverride(double[] position) {
        this.position = position;
    }

    public void setPosition(double[] position) {
        System.arraycopy(position, 0, this.position, 0, position.length);
    }

    @Override
    public double getMass() {
        return mass;
    }

    public double getAverageRadius() {
        return (2 * equatorialRadius + polarRadius) / 3;
    }

    public double getEquatorialRadius() {
        return equatorialRadius;
    }

    public double getPolarRadius() {
        return polarRadius;
    }

    public void setRadius(double equatorialRadius, double polarRadius) {
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;

//        setScale(shownScale);  // reset the model radius
    }

    public void setRadiusByVolume(double volume) {
        double ratio = equatorialRadius / polarRadius;

        // Calculate the polar radius based on the volume and the ratio
        double Rpol = Math.pow(3 * volume / (4 * Math.PI * ratio * ratio), 1.0 / 3);

        // Calculate the equatorial radius based on the ratio
        double Req = ratio * Rpol;

        setRadius(Req, Rpol);
    }

    protected void updateRadiusByMassDensity(double density) {
        double volume = this.mass / density;
        setRadiusByVolume(volume);
    }

    public double[] getRotationAxis() {
        return rotationAxis;
    }

    public void updateRotationAxis(double[] newRotationAxis) {
        this.rotationAxis = newRotationAxis;
    }

    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Forced to set the new rotation status. This of course break the energy conservation.
     */
    public void forcedSetRotation(double[] axis, double angularVelocity) {
        this.rotationAxis = VectorOperations.normalize(axis);

        this.angularVelocity = angularVelocity;
    }

    public void forceSetMass(double mass) {
        this.mass = mass;
    }

    public void gainMattersFrom(Simulator simulator, CelestialObject object, double timeStep) {
        if (object.equatorialRadius < 50) return;  // too small
        if (object.getMass() < 1e6) return;  // too light
        double maxGain = Math.min(getMass() * 3e-7, SystemPresets.MOON_MASS * 1e-3) * timeStep;
        maxGain = Math.min(maxGain, object.getMass() - 5e5);  // half of 1e6
        double gain = Math.random() * maxGain;
        double transformedRatio = gain / object.getMass();

        double energyBefore = transitionalKineticEnergy() +
                rotationalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(this) +
                internalThermalEnergy +
                object.transitionalKineticEnergy() +
                object.rotationalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(object) +
                object.internalThermalEnergy +
                simulator.potentialEnergyBetween(this, object);

        double[] transferredMomentum = VectorOperations.scale(object.velocity, gain);

        double objDensity = object.getDensity();
        object.mass -= gain;
        object.internalThermalEnergy *= (1 - transformedRatio);
        double rad = object.getAverageRadius();
        object.updateRadiusByMassDensity(objDensity);
//        System.out.println(object.name + " Radius: " + rad + " " + object.getAverageRadius());

        object.bodyType = object.bodyType.disassemble(object.getMass());

        // momentum conservation
        VectorOperations.addInPlace(velocity, VectorOperations.scale(transferredMomentum, 1 / getMass()));

        double thisVolume = getVolume();
        thisVolume += gain / objDensity;
        this.mass += gain;
        setRadiusByVolume(thisVolume);

        double energyAfter = transitionalKineticEnergy() +
                rotationalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(this) +
                internalThermalEnergy +
                object.transitionalKineticEnergy() +
                object.rotationalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(object) +
                object.internalThermalEnergy +
                simulator.potentialEnergyBetween(this, object);
        double energyLoss = energyBefore - energyAfter;
        this.internalThermalEnergy += energyLoss;
    }

    public CelestialObject disassemble(Simulator simulator,
                                       CelestialObject forceSource,
                                       double actualRocheLimit) {
        if (debrisLevel > 2) {
            return null;
        }
        double timeSinceLastDisassemble = simulator.getTimeStepAccumulator() - lastBreakTime;
        if (timeSinceLastDisassemble < Simulator.MIN_DISASSEMBLE_TIME) {
            return null;
        }
        if (getAverageRadius() < Simulator.MIN_DEBRIS_RADIUS) {
            return null;
        }
        double dt = VectorOperations.distance(position, forceSource.position);
        double volumeInRoche = intersectionVolume(actualRocheLimit, getAverageRadius(), dt);

        if (volumeInRoche < Simulator.MIN_DEBRIS_VOLUME * 2) {
            return null;
        }
        // Calculate probability of event occurring using exponential function
        double probability = 1 - Math.exp(-Simulator.DISASSEMBLE_LAMBDA * timeInsideRocheLimit);
//        System.out.println("Prob:" + probability);
        if (simulator.random.nextDouble() < probability) {
            // break up
            timeInsideRocheLimit = 0;
            lastBreakTime = simulator.getTimeStepAccumulator();
            double debrisVol = simulator.random.nextDouble(Simulator.MIN_DEBRIS_VOLUME,
                    Math.min(volumeInRoche - Simulator.MIN_DEBRIS_VOLUME, getVolume() / 2));

            System.out.println("Disassemble!");

            double energyBefore = transitionalKineticEnergy() +
                    rotationalKineticEnergy() +
                    simulator.gravitationalBindingEnergyOf(this) +
                    internalThermalEnergy +
                    simulator.potentialEnergyBetween(this, forceSource);

            double debrisRadius = calculateRadiusByVolume(debrisVol);
            double debrisMass = getDensity() * debrisVol;

            double[] AB = VectorOperations.subtract(forceSource.position, position);
            // Distance between the centers
            double distanceAB = VectorOperations.magnitude(AB);
            double t = ((getEquatorialRadius() + debrisRadius) * 1.01) / distanceAB;
            // Interpolate to find the collision point (starting from A's center)
            double[] debrisPos = VectorOperations.add(position,
                    VectorOperations.scale(AB, t));
            double[] relVel = VectorOperations.subtract(velocity, forceSource.velocity);
            double[] debrisRelVel = VectorOperations.scale(relVel, 1.0);
            double[] debrisVel = VectorOperations.add(forceSource.velocity, debrisRelVel);

            double temperature = getBodyAverageTemperature();
            BodyType debrisType = bodyType.disassemble(debrisMass);

            CelestialObject debris = new CelestialObject(
                    id + "-debris",
                    debrisType,
                    debrisMass,
                    debrisRadius,
                    debrisRadius,
                    debrisPos,
                    debrisVel,
                    Arrays.copyOf(rotationAxis, rotationAxis.length),
                    0,
                    Util.darker(colorCode, 0.25),
                    null,
                    temperature
            );
            debris.debrisLevel = debrisLevel + 1;
            debris.lastBreakTime = lastBreakTime;

            double remVolume = getVolume() - debrisVol;

            this.mass -= debrisMass;
            this.internalThermalEnergy -= debris.internalThermalEnergy;

            this.bodyType = bodyType.disassemble(this.mass);

            setRadiusByVolume(remVolume);

            // do energy conservation
            double thisEnergyAfter = transitionalKineticEnergy() +
                    rotationalKineticEnergy() +
                    simulator.gravitationalBindingEnergyOf(this) +
                    internalThermalEnergy;
            double debrisEnergy = debris.transitionalKineticEnergy() +
                    debris.rotationalKineticEnergy() +
                    simulator.gravitationalBindingEnergyOf(debris) +
                    debris.internalThermalEnergy;
            double gPotEnergy = simulator.potentialEnergyBetween(this, debris) +
                    simulator.potentialEnergyBetween(this, forceSource) +
                    simulator.potentialEnergyBetween(debris, forceSource);
            double energyAfter = thisEnergyAfter +
                    debrisEnergy +
                    gPotEnergy;
            double energyChange = energyBefore - energyAfter;

            // distribute the remaining energy
            debris.internalThermalEnergy += energyChange / 2;
            internalThermalEnergy += energyChange / 2;

            return debris;
        } else {
            timeInsideRocheLimit += simulator.timeStep;
            return null;
        }
    }

    public void collideWith(Simulator simulator, CelestialObject other, double[] collisionPoint) {
        if (other.mass > this.mass) {
            throw new RuntimeException("Calling 'collideWith()' in the wrong order");
        }

        System.out.println(id + " collides with " + other.getId());

        // Step 2: Calculate initial total energy (translational + rotational + gravitational potential)
        double initialEnergy = this.rotationalKineticEnergy() +
                this.transitionalKineticEnergy() +
                other.rotationalKineticEnergy() +
                other.transitionalKineticEnergy() +
                this.internalThermalEnergy +
                other.internalThermalEnergy +
                simulator.potentialEnergyBetween(this, other) +
                simulator.gravitationalBindingEnergyOf(this) +
                simulator.gravitationalBindingEnergyOf(other);

        // Step 1: Combine masses
        double totalMass = this.mass + other.mass;
        double totalVolume = this.getVolume() + other.getVolume();

        // Step 2: Compute the new velocity using conservation of linear momentum
        double[] newVelocity = VectorOperations.add(
                VectorOperations.scale(this.velocity, this.mass),
                VectorOperations.scale(other.velocity, other.mass)
        );
        newVelocity = VectorOperations.scale(newVelocity, 1 / totalMass);

        // Step 3: Calculate angular momentum of the system before collision
        double[] L_A = this.angularMomentum();
        double[] r_BA = VectorOperations.subtract(collisionPoint, this.position);
        double[] relVel = VectorOperations.subtract(other.velocity, velocity);
        double[] L_B_to_A = VectorOperations.crossProduct(r_BA,
                VectorOperations.scale(relVel, other.mass));

        // Step 4: Combine angular momenta
        double[] totalAngularMomentum = VectorOperations.add(L_A, L_B_to_A);

        // update radius

        this.mass = totalMass;
        this.velocity = newVelocity;
        this.bodyType = bodyType.merge(other.bodyType, totalMass);
        if (this.bodyType.adaptiveDensity) {
            double newDensity = BodyType.massiveObjectDensity(this.mass);
            double newVolume = this.mass / newDensity;
            setRadiusByVolume(newVolume);
        } else {
            setRadiusByVolume(totalVolume);
        }

        // Step 5: Compute new rotation axis and angular velocity for object A
        double[] newRotationAxis = VectorOperations.normalize(totalAngularMomentum);
        double newAngVel = computeNewAngularVelocity(totalAngularMomentum, this.mass, this.equatorialRadius);

        // Step 5: Assign the new attributes to the surviving object
        this.rotationAxis = newRotationAxis;
        this.angularVelocity = newAngVel;

        // Step 6: Determine remaining energy for translational motion
        double newEnergySum = this.rotationalKineticEnergy() +
                this.transitionalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(this) +
                this.internalThermalEnergy;
        double difference = initialEnergy - newEnergySum;
//        System.out.println("Energy difference: " + difference);
        this.internalThermalEnergy += difference;
    }

    public boolean isExist() {
        return exist;
    }

    public double getVolume() {
        return 4.0 / 3.0 * Math.PI * equatorialRadius * equatorialRadius * polarRadius;
    }

    public double getSurfaceArea() {
        // approximation
        double p = 1.6075;
        double a = equatorialRadius;
        double b = equatorialRadius;
        double c = polarRadius;
        double ap = Math.pow(a, p);
        double bp = Math.pow(b, p);
        double cp = Math.pow(c, p);
        double inside = (ap * bp + ap * cp + bp * cp) / 3;
        return 4 * Math.PI * Math.pow(inside, 1 / p);
    }

    public double getDensity() {
        return mass / getVolume();
    }
    
    public double getOblateness() {
        double eqr = getEquatorialRadius();
        return (eqr - getPolarRadius()) / eqr;
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

    public String getTexturePath() {
        return texturePath;
    }

    public CelestialObject getMaxGravityObject() {
        return maxGravityObject;
    }

    public CelestialObject getGravityMaster() {
        return gravityMaster;
    }

    public CelestialObject getHillMaster() {
        return hillMaster;
    }

    public void setHillMaster(CelestialObject hillMaster) {
        this.hillMaster = hillMaster;
    }

    public void setMaxGravityObject(CelestialObject maxGravityObject) {
        this.maxGravityObject = maxGravityObject;
    }

    @Override
    public CelestialObject getMaster() {
        return hillMaster;
    }

    public double getHillRadius() {
        return hillRadius;
    }

    public double getApproxRocheLimit() {
        return approxRocheLimit;
    }

    @Override
    public int compareTo(CelestialObject o) {
        int massCmp = Double.compare(this.mass, o.mass);
        if (massCmp != 0) return massCmp;
        return this.id.compareTo(o.id);
    }

    @Override
    public String toString() {
        return "CelestialObject{" + id + "}";
    }

    public static double radiusOf(double mass, double density) {
        double volume = mass / density;
        return Math.cbrt((3 * volume) / (4 * Math.PI));
    }

    public static double angularVelocityOf(double rotationPeriod) {
        if (rotationPeriod == Double.POSITIVE_INFINITY) return 0;
        return 2 * Math.PI / rotationPeriod;
    }

    public static double calculateRadiusByVolume(double volume) {
        return Math.pow(3 * volume / (4 * Math.PI), 1.0 / 3);
    }

    public static double intersectionVolume(double r1, double r2, double d) {
        // Check if the spheres are not intersecting
        if (d >= r1 + r2) {
            return 0.0;
        }

        // If the spheres are perfectly overlapping
        if (d == 0 && r1 == r2) {
            return (4.0 / 3.0) * Math.PI * Math.pow(r1, 3); // Volume of one sphere
        }

        // Volume of intersection for overlapping spheres
        double part1 = Math.PI / (12.0 * d);
        double term1 = Math.pow(r1 + r2 - d, 2);
        double term2 = Math.pow(d, 2) + 2 * d * (r1 + r2) - 3 * Math.pow(r1 - r2, 2);

        return part1 * term1 * term2;
    }

    public static double approxSurfaceTemperatureOf(CelestialObject co,
                                                    List<Star> sources) {
        double albedo = co.estimateAlbedo();
        double totalFi = 0;
        for (Star source : sources) {
            double distance = VectorOperations.distance(source.co.getPosition(), co.getPosition());
            double fIncident = fIncident(source.getLuminosity(), distance);
            totalFi += fIncident;
        }

        double absorbed = (1 - albedo) * totalFi;
        double low = 4 * co.emissivity * STEFAN_BOLTZMANN_CONSTANT;
        return Math.pow(absorbed / low, 0.25);
    }

    public static double approxLuminosityOfStar(double mass) {
        if (mass < SystemPresets.JUPITER_MASS * 80) {
            return 0;
        } else {
            // todo: assume is main sequence
            double ratio = Math.pow(mass / SystemPresets.SOLAR_MASS, 3.5);
            return ratio * SystemPresets.SOLAR_LUMINOSITY;
        }
    }

    public static double approxColorTemperatureOfStar(double luminosity, double radius) {
        double stefanBoltzmannConstant = 5.67e-8;
        double divisor = 4 * Math.PI * radius * radius * stefanBoltzmannConstant;
        return Math.pow(luminosity / divisor, 0.25);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
