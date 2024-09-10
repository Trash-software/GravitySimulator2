package com.trashsoftware.gravity2.physics;

import com.jme3.texture.Texture;
import com.trashsoftware.gravity2.gui.JmeApp;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class CelestialObject implements Comparable<CelestialObject>, AbstractObject {

    public static final double REF_HEAT_CAPACITY = 14300.0;

    protected double[] position;
    protected double[] velocity;
    protected double[] rotationAxis;
    protected double angularVelocity;
    protected double mass;
    protected double tidalLoveNumber = 0.15;
    protected double dissipationFunction = 100;
    protected double equatorialRadius, polarRadius;
    protected double thermalEnergy;
    protected String name;

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
    private final Texture texture;

    protected transient double[] lastAcceleration;
    //    protected transient double[] orbitBasic;  // semi-major, eccentricity
    protected transient CelestialObject gravityMaster;
    protected transient CelestialObject hillMaster;
    protected transient double hillRadius;
    protected transient double possibleRocheLimit;
    protected transient double approxRocheLimit;

    CelestialObject(String name,
                    double mass,
                    double equatorialRadius,
                    double polarRadius,
                    double[] position,
                    double[] velocity,
                    double[] rotationAxis,
                    double angularVelocity,
                    String colorCode,
                    Texture diffuseMap,
                    double avgTemp) {

        if (position.length != velocity.length) {
            throw new IllegalArgumentException("You are in what dimensional world?");
        }
        lastAcceleration = new double[position.length];

        this.name = name;
        this.mass = mass;
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.position = position;
        this.velocity = velocity;
        this.colorCode = colorCode;
        this.rotationAxis = VectorOperations.normalize(rotationAxis);
        this.angularVelocity = angularVelocity;
        this.thermalEnergy = calculateThermalEnergyByTemperature(REF_HEAT_CAPACITY, mass, avgTemp);

        this.texture = diffuseMap;

        possibleRocheLimit = Simulator.computeMaxRocheLimit(this);
//        approxRocheLimit = Simulator.computeRocheLimitLiquid(this);
        approxRocheLimit = Simulator.computeRocheLimitSolid(this);
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

    public static CelestialObject create2d(String name,
                                           double mass,
                                           double radius,
                                           double x,
                                           double y,
                                           String colorCode) {
        return create2d(name,
                mass,
                radius,
                x,
                y,
                0,
                0,
                colorCode);
    }

    public static CelestialObject createNd(String name,
                                           double mass,
                                           double radius,
                                           int dim,
                                           String colorCode) {
        return createNd(name,
                mass,
                radius,
                dim,
                new double[dim],
                new double[dim],
                colorCode);
    }

    public static CelestialObject createReal(String name,
                                             double mass,
                                             double equatorialRadius,
                                             double polarRadius,
                                             int dim,
                                             double[] position,
                                             double[] velocity,
                                             double[] axis,
                                             double angularVelocity,
                                             String colorCode,
                                             Texture diffuseMap,
                                             double avgTemp) {
        CelestialObject co = new CelestialObject(
                name,
                mass,
                equatorialRadius,
                polarRadius,
                new double[dim],
                new double[dim],
                axis,
                angularVelocity,
                colorCode,
                diffuseMap,
                avgTemp
        );
        // argument pos/vel can be shorter than dim
        System.arraycopy(position, 0, co.position, 0, position.length);
        System.arraycopy(velocity, 0, co.velocity, 0, velocity.length);
        return co;
    }

    public static CelestialObject createNd(String name,
                                           double mass,
                                           double radius,
                                           int dim,
                                           double[] position,
                                           double[] velocity,
                                           String colorCode) {
        double[] axis = new double[]{0, 0, 1};
        CelestialObject co = new CelestialObject(
                name,
                mass,
                radius,
                radius,
                new double[dim],
                new double[dim],
                axis,
                0.0,
                colorCode,
                null,
                273.15
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

        double[] position = Util.jsonArrayToDoubleArray(positionArr);
        double[] velocity = Util.jsonArrayToDoubleArray(velocityArr);
        double[] rotationAxis = Util.jsonArrayToDoubleArray(axisArr);
        Texture texture = null;
        if (json.has("texture")) {
            String textureString = json.getString("texture");
            if (textureString != null && !"null".equals(textureString)) {
                texture = JmeApp.getInstance().getAssetManager().loadTexture(textureString);
            }
        }

//        int dim = position.length;

        CelestialObject co = new CelestialObject(
                json.getString("name"),
                json.getDouble("mass"),
                json.getDouble("equatorialRadius"),
                json.getDouble("polarRadius"),
                position,
                velocity,
                rotationAxis,
                json.getDouble("angularVelocity"),
//                new ColorRGBA(json.getString("color")),
                null,
                texture,
                0
        );

        co.exist = json.getBoolean("exist");
//        co.shownScale = json.getDouble("shownScale");
        co.rotationAngle = json.getDouble("rotationAngle");
        co.thermalEnergy = json.getDouble("thermalEnergy");

        return co;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("name", name);
        json.put("mass", mass);
        json.put("equatorialRadius", equatorialRadius);
        json.put("polarRadius", polarRadius);
        json.put("exist", exist);
//        json.put("shownScale", shownScale);
//        json.put("color", Util.colorToHex(color));
        json.put("texture", texture != null ? texture.getName() : "null");
        json.put("position", new JSONArray(position));
        json.put("velocity", new JSONArray(velocity));
        json.put("rotationAxis", new JSONArray(rotationAxis));
        json.put("angularVelocity", angularVelocity);
        json.put("rotationAngle", rotationAngle);
        json.put("thermalEnergy", thermalEnergy);

        return json;
    }

    public String getName() {
        return name;
    }

//    public void setColor(ColorRGBA color) {
//        this.color = color;
//
//        PhongMaterial material = new PhongMaterial();
//        material.setDiffuseColor(color);
//        model.setMaterial(material);
//    }


    public String getColorCode() {
        return colorCode;
    }

    public void setTidalConstants(double tidalLoveNumber, double dissipationFunction) {
        this.tidalLoveNumber = tidalLoveNumber;
        this.dissipationFunction = dissipationFunction;
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

    public boolean isEmittingLight() {
        return getLuminosity() > 0;
    }

    public double getLuminosity() {
        if (name.equals("Sun")) {
            return 3.828e26;
        } else {
            return 0;
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

    public void gainMattersFrom(Simulator simulator, CelestialObject object) {

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
                    thermalEnergy +
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

            CelestialObject debris = new CelestialObject(
                    name + "-debris",
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
            this.thermalEnergy -= debris.thermalEnergy;

            setRadiusByVolume(remVolume);

            // do energy conservation
            double thisEnergyAfter = transitionalKineticEnergy() +
                    rotationalKineticEnergy() +
                    simulator.gravitationalBindingEnergyOf(this) +
                    thermalEnergy;
            double debrisEnergy = debris.transitionalKineticEnergy() +
                    debris.rotationalKineticEnergy() +
                    simulator.gravitationalBindingEnergyOf(debris) +
                    debris.thermalEnergy;
            double gPotEnergy = simulator.potentialEnergyBetween(this, debris) +
                    simulator.potentialEnergyBetween(this, forceSource) +
                    simulator.potentialEnergyBetween(debris, forceSource);
            double energyAfter = thisEnergyAfter +
                    debrisEnergy +
                    gPotEnergy;
            double energyChange = energyBefore - energyAfter;

            // distribute the remaining energy
            debris.thermalEnergy += energyChange / 2;
            thermalEnergy += energyChange / 2;

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

        System.out.println(name + " collides with " + other.getName());

        // Step 2: Calculate initial total energy (translational + rotational + gravitational potential)
        double initialEnergy = this.rotationalKineticEnergy() +
                this.transitionalKineticEnergy() +
                other.rotationalKineticEnergy() +
                other.transitionalKineticEnergy() +
                this.thermalEnergy +
                other.thermalEnergy +
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
        setRadiusByVolume(totalVolume);
        double newEqRadius = this.equatorialRadius;

        // Step 5: Compute new rotation axis and angular velocity for object A
        double[] newRotationAxis = VectorOperations.normalize(totalAngularMomentum);
        double newAngVel = computeNewAngularVelocity(totalAngularMomentum, totalMass, newEqRadius);

        // Step 5: Assign the new attributes to the surviving object
        this.mass = totalMass;
        this.velocity = newVelocity;
        this.rotationAxis = newRotationAxis;
        this.angularVelocity = newAngVel;

        // Step 6: Determine remaining energy for translational motion
        double newEnergySum = this.rotationalKineticEnergy() +
                this.transitionalKineticEnergy() +
                simulator.gravitationalBindingEnergyOf(this) +
                this.thermalEnergy;
        double difference = initialEnergy - newEnergySum;
//        System.out.println("Energy difference: " + difference);
        this.thermalEnergy += difference;
    }

    public boolean isExist() {
        return exist;
    }

    public double getVolume() {
        return 4.0 / 3.0 * Math.PI * equatorialRadius * equatorialRadius * polarRadius;
    }

    public double getDensity() {
        return mass / getVolume();
    }

    public double getThermalEnergy() {
        return thermalEnergy;
    }

    public double getBodyAverageTemperature() {
        return thermalEnergy / mass / REF_HEAT_CAPACITY;
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

    public Texture getTexture() {
        return texture;
    }

    public CelestialObject getGravityMaster() {
        return gravityMaster;
    }

    public CelestialObject getHillMaster() {
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
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return "CelestialObject{" + name + "}";
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

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
