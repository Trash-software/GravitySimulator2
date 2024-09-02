package com.trashsoftware.gravity2.physics;

import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;
import com.trashsoftware.gravity2.gui.JmeApp;
import org.json.JSONArray;
import org.json.JSONObject;

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

//    private Geometry model;
//    private final Scale viewScale = new Scale(1, 1, 1);
//    private final Rotate axisTilt;
//    private final Rotate rotation;
//    private double shownScale = 1.0;

    private String colorCode;
    private final Texture texture;

    protected transient double[] lastAcceleration;
//    protected transient double[] orbitBasic;  // semi-major, eccentricity
    protected transient CelestialObject gravityMaster;
    protected transient CelestialObject hillMaster;
    protected transient double hillRadius;
    protected transient Geometry hillSphereModel;
    protected transient double possibleRocheLimit;
    protected transient double approxRocheLimit;
    protected transient Geometry rocheSphereModel;

    public CelestialObject(String name,
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
        
//        convertToFxAxisTilt(this.rotationAxis);
        
//        rotation = new Rotate(0, Rotate.Y_AXIS);
        
        updateTransforms();

        possibleRocheLimit = Simulator.computeMaxRocheLimit(this);
        approxRocheLimit = Simulator.computeRocheLimitLiquid(this);
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
    
    private void updateTransforms() {
//        model.getTransforms().clear();
//        
//        double ratio = polarRadius / equatorialRadius;
//        // Step 1: Create a custom affine transformation
//        Affine transform = new Affine();
//
//        // Step 2: Apply the tilt (rotation around X-axis by 26.7 degrees)
//        Rotate axisTilt = convertToFxAxisTilt(rotationAxis);
//        transform.appendRotation(axisTilt.getAngle(), 0, 0, 0, axisTilt.getAxis());
//        transform.appendScale(1.0, ratio, 1.0);
//
//        model.getTransforms().addAll(transform, rotation, viewScale);
    }

    private static Object convertToFxAxisTilt(double[] axis) {
//        System.out.println(Arrays.toString(axis));
        // Desired direction (x, y, z)
        if (axis[2] < 0) {
            axis = VectorOperations.reverseVector(axis);
        }
        
        double targetX = axis[0];
        double targetY = axis[1];
        double targetZ = axis[2];

        // Normalize the target direction vector
        double length = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        double unitX = targetX / length;
        double unitY = targetY / length;
        double unitZ = targetZ / length;

        // Initial direction vector (assumed along X-axis)
        double initialX = 0;
        double initialY = 1;
        double initialZ = 0;

        // Calculate the cross product (axis of rotation)
        double axisX = initialY * unitZ - initialZ * unitY;
        double axisY = initialZ * unitX - initialX * unitZ;
        double axisZ = initialX * unitY - initialY * unitX;

        // Calculate the angle between the two vectors using the dot product
        double dotProduct = initialX * unitX + initialY * unitY + initialZ * unitZ;
        double angle = Math.toDegrees(Math.acos(dotProduct));

        // Apply the rotation to the sphere
//        return new Rotate(angle, new Point3D(axisX, axisY, axisZ));
        return null;
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
        
        int sign = 1;
        if (rotationAxis[rotationAxis.length - 1] < 0) {
            sign = -1;
        }
        rotationAngle += deg * sign;
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

    private double[] angularMomentum() {
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

    public double getRadius() {
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

    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Forced to set the new rotation status. This of course break the energy conservation.
     */
    public void forcedSetRotation(double[] axis, double angularVelocity) {
        this.rotationAxis = VectorOperations.normalize(axis);
        updateTransforms();
        
        this.angularVelocity = angularVelocity;
    }

    public void forceSetMass(double mass) {
        this.mass = mass;
    }

    public void collideWith(Simulator simulator, CelestialObject other, double[] collisionPoint) {
        if (other.mass > this.mass) {
            throw new RuntimeException("Calling 'collideWith()' in the wrong order");
        }

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
        double[] L_B_to_A = VectorOperations.crossProduct(r_BA,
                VectorOperations.scale(other.velocity, other.mass));

        // Step 4: Combine angular momenta
        double[] totalAngularMomentum = VectorOperations.subtract(L_A, L_B_to_A);

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
        
        updateTransforms();
        
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

    public void destroy() {
        this.exist = false;
//        model.setVisible(false);
        // let them be garbage collected
//        model = null;
//        scale = null;
        hillSphereModel = null;
        rocheSphereModel = null;
    }

    public Texture getTexture() {
        return texture;
    }

    public Geometry getHillSphereModel() {
//        if (hillSphereModel == null) {
//            hillSphereModel = new Sphere();
//            PhongMaterial material = new PhongMaterial();
//            material.setDiffuseColor(MainView.opaqueOf(color.brighter(), 0.05));
//            hillSphereModel.setMaterial(material);
//        }
        return hillSphereModel;
    }

    public void setHillSphereRadius(double hillRadius) {
//        getHillSphereModel().setRadius(hillRadius * shownScale);
    }

    public Geometry getRocheSphereModel() {
//        if (rocheSphereModel == null) {
//            rocheSphereModel = new Sphere();
//            PhongMaterial material = new PhongMaterial();
//            material.setDiffuseColor(MainView.opaqueOf(color.darker(), 0.1));
//            rocheSphereModel.setMaterial(material);
//        }
        return rocheSphereModel;
    }

    public void setRocheSphereRadius(double rocheLimit) {
//        getRocheSphereModel().setRadius(rocheLimit * shownScale);
//        System.out.println(name + " Set to " + (rocheLimit * shownScale) + " radius: " + (radius * shownScale));
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
}
