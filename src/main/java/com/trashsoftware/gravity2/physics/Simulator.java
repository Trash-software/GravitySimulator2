package com.trashsoftware.gravity2.physics;

import com.trashsoftware.gravity2.physics.status.Comet;
import com.trashsoftware.gravity2.physics.status.Star;
import com.trashsoftware.gravity2.presets.SystemPresets;
import com.trashsoftware.gravity2.utils.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Simulator {

    public static final double PATH_INTERVAL = 100.0;
    public static final int PATH_LIMIT = 1048576;
    public static final double MIN_DISASSEMBLE_TIME = 500.0;
    public static final double MIN_DEBRIS_RADIUS = 1e4;
    public static final double MIN_DEBRIS_VOLUME = 4.0 / 3 * Math.PI * Math.pow(MIN_DEBRIS_RADIUS, 3);
    public static final double DISASSEMBLE_LAMBDA = 5e-5;
    public static final double MAX_TIME_AFTER_DIE = 3e5;

    public static final double PLANET_MAX_MASS = 0.8;

    protected double timeStep = 1;
    private transient double lastTimeStepAccumulator = 0;
    private double timeStepAccumulator = 0;
    private final int dimension;
    protected double tidalEffectFactor = 1;
//    protected double tidalEffectFactor = 1e25;

    protected double G;
    protected double gravityDtPower;
    private double epsilon = 0.0;
    private double cutOffForce;
    private boolean enableDisassemble = true;

    /**
     * All objects, always sorted from massive to light
     */
    private final List<CelestialObject> objects = new ArrayList<>();
    private final Map<CelestialObject, Deque<double[]>> recentPaths = new HashMap<>();  // [x,y,timeStep]
    private final Deque<double[]> barycenterPath = new ArrayDeque<>();
    private double[] barycenter;

    // temp buffers
    private transient double[][] forcesBuffer;
    private transient double[] dimDtBuffer;
    private transient final List<CelestialObject> debrisBuffer = new ArrayList<>();
    private transient final List<CelestialObject> newlyDestroyed = new ArrayList<>();

    private transient final Map<CelestialObject, HieraticalSystem> systemMap = new HashMap<>();
    private final transient List<HieraticalSystem> rootSystems = new ArrayList<>();
    private transient int forceCounter1, forceCounter2;

    private transient final ForkJoinPool forceCalculationPool = new ForkJoinPool();
    protected transient final Random random = new Random();

    public Simulator(int dimension, double G, double gravityDtPower) {
        this.dimension = dimension;
        this.G = G;
        this.gravityDtPower = gravityDtPower;
    }

    public Simulator() {
        this(3, 6.67430e-11, 2);
    }

    public static Simulator loadFromJson(JSONObject json) {
        int dim = json.getInt("dimension");
        double G = json.getDouble("G");
        double gravityDtPower = json.getDouble("gravityDtPower");

        Simulator simulator = new Simulator(dim, G, gravityDtPower);

        simulator.timeStep = json.getDouble("timeStep");
        simulator.timeStepAccumulator = json.getDouble("timeStepAccumulator");
        simulator.epsilon = json.getDouble("epsilon");
        simulator.cutOffForce = json.getDouble("cutOffForce");
        simulator.tidalEffectFactor = json.getDouble("tidalEffectFactor");
        simulator.enableDisassemble = json.getBoolean("enableDisassemble");

        JSONArray objectsArr = json.getJSONArray("objects");
        for (int i = 0; i < objectsArr.length(); i++) {
            CelestialObject co = CelestialObject.fromJson(objectsArr.getJSONObject(i));
            simulator.addObject(co);
        }

        return simulator;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("dimension", dimension);
        json.put("G", G);
        json.put("gravityDtPower", gravityDtPower);

        json.put("timeStep", timeStep);
        json.put("timeStepAccumulator", timeStepAccumulator);
        json.put("epsilon", epsilon);
        json.put("cutOffForce", cutOffForce);
        json.put("tidalEffectFactor", tidalEffectFactor);
        json.put("enableDisassemble", enableDisassemble);

        JSONArray objectsArray = new JSONArray();
        for (CelestialObject co : objects) {
            objectsArray.put(co.toJson());
        }
        json.put("objects", objectsArray);

        return json;
    }

    public void setTimeStep(double timeStep) {
        this.timeStep = timeStep;
    }

    public double getTimeStepAccumulator() {
        return timeStepAccumulator;
    }

    /**
     * Higher epsilon will result in faster simulation but lower accuracy
     *
     * @param epsilon the epsilon
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        updateForceThreshold();
    }

    public void setEnableDisassemble(boolean enableDisassemble) {
        this.enableDisassemble = enableDisassemble;
    }

    public boolean isEnableDisassemble() {
        return enableDisassemble;
    }

    public void setG(double g) {
        this.G = g;
    }

    public double getG() {
        return G;
    }

    public void setGravityDtPower(double gravityDtPower) {
        this.gravityDtPower = gravityDtPower;
    }

    public double getGravityDtPower() {
        return gravityDtPower;
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * @return whether the size has changed
     */
    public SimResult simulate(int nPhysicalFrames, boolean highPerformanceMode) {
        int nObj = objects.size();
        boolean changeHappen = false;

        SimResult result = SimResult.NORMAL;

        updateForceThreshold();

        double[] zeros = new double[dimension];
        double performedTimeSteps = 0;

        for (int step = 0; step < nPhysicalFrames; step++) {
            // Calculate forces based on current positions
            calculateAllForces(objects);

            // Half-step velocity update
            for (int i = 0; i < objects.size(); i++) {
                CelestialObject object = objects.get(i);
                if (Arrays.equals(zeros, forcesBuffer[i])) {
                    Arrays.fill(object.lastAcceleration, 0);
                    continue;
                }
                for (int j = 0; j < dimension; j++) {
                    object.lastAcceleration[j] = 0.5 * forcesBuffer[i][j] / object.mass;
                    objects.get(i).velocity[j] += object.lastAcceleration[j] * timeStep;
                }
            }

            // Full-step position update
            for (CelestialObject obj : objects) {
                for (int j = 0; j < dimension; j++) {
                    obj.position[j] += obj.velocity[j] * timeStep;
                }
            }

            // Check for collisions and handle them
            if (handleCollisions(objects, timeStep)) {
                changeHappen = true;
            }

            // Calculate forces based on new positions
            calculateAllForces(objects);

            // Half-step velocity update
            for (int i = 0; i < objects.size(); i++) {
                CelestialObject object = objects.get(i);
                if (Arrays.equals(zeros, forcesBuffer[i])) {
                    Arrays.fill(object.lastAcceleration, 0);
                    continue;
                }
                for (int j = 0; j < dimension; j++) {
                    double newAcc = 0.5 * forcesBuffer[i][j] / object.mass;
                    object.lastAcceleration[j] += newAcc;
                    object.velocity[j] += newAcc * timeStep;
                }
                if (object.hillMaster != null && object.hillMaster.possibleRocheLimit != 0.0) {
                    // hill master is heavier, so its velocity will be updated earlier
                    double[] relVel = VectorOperations.subtract(object.velocity, object.hillMaster.velocity);
                    if (VectorOperations.magnitude(relVel) * timeStep > object.hillMaster.possibleRocheLimit * 0.33) {
                        System.out.println("Too fast: " + object.id);
                        result = SimResult.TOO_FAST;
                    }
                }
            }

            if (!debrisBuffer.isEmpty()) {
                for (CelestialObject debris : debrisBuffer) {
                    addObject(debris);
                }
                debrisBuffer.clear();
                changeHappen = true;
            }

            if (!highPerformanceMode) {
                if (timeStepAccumulator - lastTimeStepAccumulator >= PATH_INTERVAL) {
                    updateBarycenter();
                    for (CelestialObject obj : objects) {
                        addPath(obj, timeStepAccumulator);
                    }
                    addBarycenterPath(timeStepAccumulator);

                    lastTimeStepAccumulator = timeStepAccumulator;
                }
            }

            timeStepAccumulator += timeStep;
            performedTimeSteps += timeStep;

            if (result == SimResult.TOO_FAST) break;
        }
        if (!highPerformanceMode) {
            gcPaths(timeStepAccumulator);
        }

        updateBarycenter();

        if (!highPerformanceMode) {
            for (CelestialObject co : objects) {
                co.updateRotation(performedTimeSteps);
            }
            updateTidal(performedTimeSteps);
            updateIndependentStatus();  // pre update
            performTemperatureChange(performedTimeSteps);
            // post update
            if (updateDependentStatus(performedTimeSteps)) {
                changeHappen = true;
            }
        }

        boolean changed = changeHappen || objects.size() != nObj;
        if (changed) {
            keepOrder();
            if (result == SimResult.NORMAL) result = SimResult.NUM_CHANGED;
        }

        if (!highPerformanceMode) {
            updateMasters();
        }

        return result;
    }

    private boolean handleCollisions(List<CelestialObject> objects, double timeStep) {
        boolean happen = false;
        int n = objects.size();
        for (int i = n - 1; i >= 0; i--) {
            CelestialObject coi = objects.get(i);
            for (int j = i - 1; j >= 0; j--) {
                CelestialObject coj = objects.get(j);
                double distance = VectorOperations.distance(coi.position, coj.position);
                boolean collide = distance < coi.getAverageRadius() + coj.getAverageRadius();

                // Determine which object is heavier
                CelestialObject heavier = coi.mass >= coj.mass ? coi : coj;
                CelestialObject lighter = heavier == coi ? coj : coi;

                if (collide) {
                    double[] AB = VectorOperations.subtract(lighter.position, heavier.position);
                    // Distance between the centers
                    double distanceAB = VectorOperations.magnitude(AB);
                    // Calculate the ratio of A's radius to the distance between A and B
                    double t = heavier.getEquatorialRadius() / distanceAB;
                    // Interpolate to find the collision point (starting from A's center)
                    double[] collisionPoint = VectorOperations.add(heavier.position,
                            VectorOperations.scale(AB, t));
                    heavier.collideWith(this, lighter, collisionPoint);

                    // Remove the lighter object
                    objects.remove(lighter);
                    systemMap.remove(lighter);
                    lighter.destroy(timeStepAccumulator);
                    newlyDestroyed.add(lighter);

                    // Adjust loop counters to account for the removed object
                    n--;
                    happen = true;
                    break; // Restart checking for collisions with updated list
                } else {
                    // not collide, check roche
                    if (distance < lighter.possibleRocheLimit) {
                        // heavier one is also inside lighter's roche limit
                        // unlikely to happen, but put it here
                        double actualRoche = computeRocheLimitSolid(lighter, heavier.getDensity());
                        if (distance - heavier.getAverageRadius() < actualRoche) {
//                            lighter.gainMattersFrom(this, heavier, timeStep);
                            if (enableDisassemble) {
                                CelestialObject debris = heavier.disassemble(this, lighter, actualRoche);
                                if (debris != null) {
                                    debrisBuffer.add(debris);
                                }
                            } else {
                                lighter.gainMattersFrom(this, heavier, timeStep);
                            }
                        }
                    }

                    if (distance < heavier.possibleRocheLimit) {
                        // This is the most common scenario
                        // if the above happen, this will also likely to happen
                        double actualRoche = computeRocheLimitSolid(heavier, lighter.getDensity());
                        if (distance - lighter.getAverageRadius() < actualRoche) {
//                            heavier.gainMattersFrom(this, lighter, timeStep);
                            if (enableDisassemble) {
                                CelestialObject debris = lighter.disassemble(this, heavier, actualRoche);
                                if (debris != null) {
                                    debrisBuffer.add(debris);
                                }
                            } else {
                                heavier.gainMattersFrom(this, lighter, timeStep);
                            }
                        }
                    }
                }
            }
        }
        return happen;
    }

    public void updateForceThreshold() {
        double logMassSum = 0.0;
        double logDistanceSum = 0.0;
        int count = 0;

        for (int i = 0; i < objects.size(); i++) {
            logMassSum += Math.log(objects.get(i).mass);

            for (int j = i + 1; j < objects.size(); j++) {
                double distance = VectorOperations.distance(objects.get(i).position, objects.get(j).position);
                logDistanceSum += Math.log(distance);
                count++;
            }
        }

        double typicalLogMass = logMassSum / objects.size();
        double typicalLogDistance = logDistanceSum / count;

        double typicalMass = Math.exp(typicalLogMass);
        double typicalDistance = Math.exp(typicalLogDistance);

        // Calculate typical gravitational force using log-scaled values
        double fTypical = (G * typicalMass * typicalMass) / (typicalDistance * typicalDistance);

        // Calculate F_THRESHOLD as a fraction of the typical force
        cutOffForce = epsilon * fTypical;
    }

    @Deprecated
    public double[][] calculateAllForces2(List<CelestialObject> objects) {
        int n = objects.size();
        if (forcesBuffer == null || forcesBuffer.length < n || forcesBuffer[0].length != dimension) {
            forcesBuffer = new double[n][dimension];
        } else {
            for (double[] doubles : forcesBuffer) {
                Arrays.fill(doubles, 0.0);
            }
        }
        if (dimDtBuffer == null || dimDtBuffer.length != dimension) {
            dimDtBuffer = new double[dimension];
        } else {
            Arrays.fill(dimDtBuffer, 0.0);
        }

        for (int i = 0; i < n; i++) {
            CelestialObject coi = objects.get(i);
            for (int j = i + 1; j < n; j++) {
                CelestialObject coj = objects.get(j);
                double sqrDt = 0;
                for (int d = 0; d < dimension; d++) {
                    dimDtBuffer[d] = coj.position[d] - coi.position[d];
                    sqrDt += dimDtBuffer[d] * dimDtBuffer[d];
                }
                double distance = Math.sqrt(sqrDt);
                double forceMagnitude = G * coi.mass * coj.mass / Math.pow(distance, gravityDtPower);

                double[] fi = forcesBuffer[i];
                double[] fj = forcesBuffer[j];
                for (int d = 0; d < dimension; d++) {
                    double fAtD = forceMagnitude * dimDtBuffer[d] / distance;
                    fi[d] += fAtD;
                    fj[d] -= fAtD;
                }
            }
        }
        return forcesBuffer;
    }

    public void calculateAllForces(List<CelestialObject> objects) {
        int n = objects.size();
        if (n == 0) return;
        if (forcesBuffer == null || forcesBuffer.length < n || forcesBuffer[0].length != dimension) {
            forcesBuffer = new double[n][dimension];
        } else {
            for (double[] doubles : forcesBuffer) {
                Arrays.fill(doubles, 0.0);
            }
        }

        forceCounter1 = 0;
        forceCounter2 = 0;

        forceCalculationPool.invoke(new ForceCalculationTask(0, n));
//        System.out.println("Effective force: " + forceCounter1 + ", not: " + forceCounter2);
    }

    public double calculateCutoffDistance(double m1, double m2) {
        return cutOffForce == 0 ? Double.MAX_VALUE : Math.sqrt(G * m1 * m2 / cutOffForce);
    }

    private void forceBetween(int i, CelestialObject coi,
                              int j, CelestialObject coj) {
        double[] dimDtBuffer = new double[dimension];
        double sqrDt = 0;
        for (int d = 0; d < dimension; d++) {
            dimDtBuffer[d] = coj.position[d] - coi.position[d];
            sqrDt += dimDtBuffer[d] * dimDtBuffer[d];
        }
        double distance = Math.sqrt(sqrDt);
        double cutOffDistance = calculateCutoffDistance(coi.mass, coj.mass);

        if (distance < cutOffDistance) {
            // only for potential performance improvement
            double dtPow = gravityDtPower == 2 ? distance * distance : Math.pow(distance, gravityDtPower);
            double forceMagnitude = G * coi.mass * coj.mass / dtPow;
            forceCounter1++;

            double[] fi = forcesBuffer[i];
            double[] fj = forcesBuffer[j];
            for (int d = 0; d < dimension; d++) {
                double fAtD = forceMagnitude * dimDtBuffer[d] / distance;
                fi[d] += fAtD;
                fj[d] -= fAtD;
            }
        } else {
            forceCounter2++;
        }
    }

    class ForceCalculationTask extends RecursiveAction {
        static int PARALLELISM_THRESHOLD = 1000;

        private final int start;
        private final int end;

        ForceCalculationTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        private static int nCalls(int start, int end, int nObj) {
            int totalPairs = (end - start) * (nObj - 1);
            int sumEnd = (end * (end - 1)) / 2;
            int sumStart = (start * (start - 1)) / 2;

            return totalPairs - sumEnd + sumStart;
        }

        @Override
        protected void compute() {
            int nObj = objects.size();
            int nComputations = nCalls(start, end, nObj);
//            System.out.println(nComputations);
            if (nComputations <= PARALLELISM_THRESHOLD || end - start < 2) {
//            if (end - start <= 10) {
                for (int i = start; i < end; i++) {
                    for (int j = i + 1; j < nObj; j++) {
                        forceBetween(i, objects.get(i), j, objects.get(j));
                    }
                }
            } else {
                int mid = (start + end) / 2;
                ForceCalculationTask leftTask = new ForceCalculationTask(start, mid);
                ForceCalculationTask rightTask = new ForceCalculationTask(mid, end);
                invokeAll(leftTask, rightTask);
            }
        }
    }

    public void setHighPerformanceMode(boolean highPerformanceMode) {
        if (highPerformanceMode) {
            barycenterPath.clear();
            recentPaths.clear();
            rootSystems.clear();
        }
    }

    private void addPath(CelestialObject object, double tsa) {
        Deque<double[]> path = recentPaths.computeIfAbsent(object, o -> new ArrayDeque<>());
        double[] p = new double[dimension + 1];
        System.arraycopy(object.position, 0, p, 0, object.position.length);
        p[p.length - 1] = tsa;
        path.addFirst(p);
    }

    private void addBarycenterPath(double tsa) {
        double[] p = new double[dimension + 1];
        double[] bc = barycenter();
        System.arraycopy(bc, 0, p, 0, bc.length);
        p[p.length - 1] = tsa;
        barycenterPath.addFirst(p);
    }

    private void gcPaths(double tsa) {
        for (var path : recentPaths.values()) {
            while (path.size() > PATH_LIMIT) {
                path.removeLast();
            }
        }
        recentPaths.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        while (barycenterPath.size() > PATH_LIMIT) {
            barycenterPath.removeLast();
        }
    }

    public void addObject(CelestialObject celestialObject) {
        renameIfConflict(celestialObject);

        if (celestialObject.position.length < dimension) {
            int oldDim = celestialObject.position.length;
            System.out.printf("Casting object %s from %dD to %dD\n",
                    celestialObject.id,
                    oldDim,
                    dimension);
            double[] newDPos = new double[dimension];
            double[] newDVel = new double[dimension];
            System.arraycopy(celestialObject.position, 0, newDPos, 0, oldDim);
            System.arraycopy(celestialObject.velocity, 0, newDVel, 0, oldDim);
            celestialObject.setPositionOverride(newDPos);
            celestialObject.setVelocityOverride(newDVel);
            celestialObject.lastAcceleration = new double[dimension];
        }

        this.objects.add(celestialObject);
        keepOrder();
        updateBarycenter();
    }

    private void keepOrder() {
        objects.sort((a, b) -> Double.compare(b.mass, a.mass));
    }

    private void renameIfConflict(CelestialObject newObject) {
        String origName = newObject.getId();
        String name = origName;
        int counter = 0;
        while (nameConflict(name)) {
            counter += 1;
            name = origName + "-" + counter;
        }
        if (!name.equals(origName)) newObject.id = name;
    }

    private boolean nameConflict(String name) {
        for (CelestialObject object : objects) {
            if (name.equals(object.getId())) return true;
        }
        return false;
    }

    public void shiftWholeSystem(double[] positionShift) {
        if (positionShift.length != dimension) {
            throw new IllegalArgumentException();
        }
        for (CelestialObject object : objects) {
            VectorOperations.addInPlace(object.position, positionShift);
        }
        for (Deque<double[]> path : recentPaths.values()) {
            for (double[] position : path) {
                VectorOperations.addInPlace(position, positionShift);
            }
        }
        for (double[] position : barycenterPath) {
            VectorOperations.addInPlace(position, positionShift);
        }

        updateBarycenter();
    }

    public void accelerateWholeSystem(double[] acceleration) {
        if (acceleration.length != dimension) {
            throw new IllegalArgumentException();
        }
        for (CelestialObject object : objects) {
            VectorOperations.addInPlace(object.velocity, acceleration);
        }

        updateBarycenter();
    }

    public void rotateWholeSystem(double[] newZAxis) {
        newZAxis = VectorOperations.normalize(newZAxis);

        for (CelestialObject co : objects) {
            double[] newPos = SystemPresets.rotateToXYPlane(co.getPosition(), newZAxis);
            double[] newVel = SystemPresets.rotateToXYPlane(co.getVelocity(), newZAxis);
            double[] newAxis = VectorOperations.normalize(
                    SystemPresets.rotateToXYPlane(co.getRotationAxis(), newZAxis));

            co.setPosition(newPos);
            co.setVelocity(newVel);
            co.forcedSetRotation(newAxis, co.angularVelocity);
        }
    }

    public double findMassOfPercentile(double percentile) {
        int index = (int) (objects.size() * (1 - percentile / 100));
        if (index <= 0) return 0;
        if (index >= objects.size()) return Double.MAX_VALUE;
        List<CelestialObject> tempList = new ArrayList<>(getObjects());
        tempList.sort(Comparator.comparingDouble(a -> a.mass));
        CelestialObject limitObj = tempList.get(index);
        return limitObj.mass;
    }

    public List<CelestialObject> getObjects() {
        return objects;
    }

    public Map<CelestialObject, Deque<double[]>> getRecentPaths() {
        return recentPaths;
    }

    public Deque<double[]> getBarycenterPath() {
        return barycenterPath;
    }

    public Deque<double[]> getPathOf(CelestialObject object) {
        return recentPaths.get(object);
    }

    public double[] calculateGravitationalForce(double mass1, double[] pos1, double mass2, double[] pos2) {
        double sqrSum = 0;
        double[] buffer = new double[dimension];
        for (int d = 0; d < dimension; d++) {
            buffer[d] = pos2[d] - pos1[d];
            sqrSum += buffer[d] * buffer[d];
        }
        double distance = Math.sqrt(sqrSum);
        double forceMagnitude = G * mass1 * mass2 / Math.pow(distance, gravityDtPower);
        for (int d = 0; d < dimension; d++) {
            buffer[d] = forceMagnitude * buffer[d] / distance;
        }
        return buffer;
    }

    /**
     * @return the sum of all objects' transitional and rotational kinetic energy
     */
    public double calculateTotalKineticEnergy() {
        double totalKE = 0.0;
        for (CelestialObject obj : objects) {
            totalKE += obj.transitionalKineticEnergy() + obj.rotationalKineticEnergy();
        }
        return totalKE;
    }

    public double gravitationalBindingEnergyOf(CelestialObject co) {
        double shape = co.shapeFactor();
        double eqr = Math.pow(co.getEquatorialRadius(), gravityDtPower - 1);
        return -0.6 * G * co.mass * co.mass / eqr * shape;
    }

    protected double potentialEnergyBetween(CelestialObject co1, CelestialObject co2) {
        double distance = VectorOperations.distance(co1.position, co2.position);
        return potentialEnergyBetween(co1.mass, co2.mass, distance,
                G, gravityDtPower);
    }

    public static double potentialEnergyBetween(double mass1, double mass2, double r,
                                                double G, double dtPower) {
        double upper = -G * mass1 * mass2 * Math.pow(r, 1 - dtPower);
        double lower = dtPower - 1;
        return upper / lower;
    }

    public double calculateTotalPotentialEnergy() {
        double totalPE = 0.0;
        int n = objects.size();
        for (int i = 0; i < n; i++) {
            CelestialObject co1 = objects.get(i);
            for (int j = i + 1; j < n; j++) {
                CelestialObject co2 = objects.get(j);
                totalPE += potentialEnergyBetween(co1, co2);
            }
        }
        return totalPE;
    }

    public double calculateTotalInternalEnergy() {
        double totalIE = 0.0;
        int n = objects.size();
        for (CelestialObject co : objects) {
            totalIE += gravitationalBindingEnergyOf(co) + co.getInternalThermalEnergy();
        }
        return totalIE;
    }

    public void updateMasters() {
        TreeMap<CelestialObject, TreeMap<Double, CelestialObject>> gravityMap = new TreeMap<>();
        double maxMass = 0.0;
        CelestialObject mostMassive = null;

        int nObjects = objects.size();
        for (int i = 0; i < nObjects; i++) {
            CelestialObject coi = objects.get(i);
            coi.maxGravityObject = null;
            if (coi.mass > maxMass) {
                maxMass = coi.mass;
                mostMassive = coi;
            }

            TreeMap<Double, CelestialObject> iMap = gravityMap.computeIfAbsent(coi, x -> new TreeMap<>());
            for (int j = i + 1; j < nObjects; j++) {
                CelestialObject coj = objects.get(j);
                TreeMap<Double, CelestialObject> jMap = gravityMap.computeIfAbsent(coj, x -> new TreeMap<>());
                double[] f = calculateGravitationalForce(coi.mass, coi.position, coj.mass, coj.position);
                double fMag = VectorOperations.magnitude(f);
                // assume there is not identical fMags
                iMap.put(fMag, coj);
                jMap.put(fMag, coi);
            }
        }

        for (var entry : gravityMap.entrySet()) {
            CelestialObject object = entry.getKey();
            TreeMap<Double, CelestialObject> gravities = entry.getValue();
            if (!gravities.isEmpty()) {
                object.maxGravityObject = gravities.lastEntry().getValue();
            }
            while (!gravities.isEmpty()) {
                var biggest = gravities.pollLastEntry();
                if (biggest.getValue().mass > object.mass) {
                    object.gravityMaster = biggest.getValue();
                    break;
                }
            }
//            System.out.println(object + " gm: " + object.gravityMaster);
        }

        for (CelestialObject object : objects) {
            object.hillRadius = computeHillRadiusVsGravityMaster(object);  // temporary
            if (mostMassive != null &&
                    object.mass > mostMassive.mass) {
                object.hillMaster = null;
            } else {
                object.hillMaster = object.getGravityMaster();
            }
//            System.out.println(object.name + " gm: " + object.gravityMaster + " hm: " + object.hillMaster);
        }

        for (CelestialObject object : objects) {
            if (object.hillMaster == null) continue;
            double minHillPercent = Double.MAX_VALUE;
            for (CelestialObject master : objects) {
                if (object == master || object.mass >= master.mass) continue;
                if (master.hillRadius == Double.MAX_VALUE) continue;  // default hill is MAX_VALUE

                double distance = VectorOperations.distance(object.position, master.position);
                double masterHill = master.hillRadius;
                double hillPercent = distance / masterHill;
                if (hillPercent <= 1.0 && hillPercent < minHillPercent) {
                    minHillPercent = hillPercent;
                    object.hillMaster = master;
                }
            }
        }

        for (CelestialObject object : objects) {
            HieraticalSystem hs = systemMap.computeIfAbsent(object, HieraticalSystem::new);
            hs.visited = false;
        }
        // update hill radius again, versus its hill master
        for (CelestialObject object : objects) {
            if (object.hillMaster != null) {
                object.hillRadius = computeHillRadiusVsHillMaster(object);
            }
        }

        rootSystems.clear();
        for (CelestialObject object : objects) {
            HieraticalSystem hs = systemMap.get(object);  // not-null
            if (object.hillMaster == null) {
                rootSystems.add(hs);
            } else {
                HieraticalSystem parentSystem = systemMap.get(object.hillMaster);
                hs.parent = parentSystem;
                parentSystem.addChild(hs);
            }
        }

        for (HieraticalSystem root : rootSystems) {
            root.updateRecursive(0);
        }

//        for (CelestialObject object : objects) {
//            if (object.hillMaster != null) {
//                HieraticalSystem hs = getHieraticalSystem(object.hillMaster);
//                double[] barycenter2 = barycenterOf(object, object.hillMaster);
//                object.orbitBasic = OrbitCalculator.computeBasic(object,
//                        barycenter2,
//                        object.hillMaster.getMass() + object.getMass(),
//                        VectorOperations.subtract(object.getVelocity(), hs.getVelocity()),
//                        G);
//            }
//        }
    }

    public HieraticalSystem findMostProbableHillMaster(double[] position) {
        List<HieraticalSystem> probableHillMasters = new ArrayList<>();
        for (HieraticalSystem hs : rootSystems) {
            HieraticalSystem hm = hs.getDeepestHillMaster(position);
            if (hm != null) {
                if (hm != hs) {
                    // not a root object, return it directly
                    return hm;
                }
                probableHillMasters.add(hm);
            }
        }
        if (probableHillMasters.isEmpty()) {
//            throw new RuntimeException("Cannot find a hill master");
            return null;
        } else if (probableHillMasters.size() == 1) return probableHillMasters.get(0);

        // compare the position is getting more force from which root object
        double maxF = 0.0;
        HieraticalSystem best = null;
        for (HieraticalSystem hs : probableHillMasters) {
            double[] f = calculateGravitationalForce(1, position, hs.getMass(), hs.getPosition());
            double fMag = VectorOperations.magnitude(f);
            if (fMag > maxF) {
                maxF = fMag;
                best = hs;
            }
        }
        assert best != null;
        return best;
    }

    public HieraticalSystem getHieraticalSystem(CelestialObject object) {
        return systemMap.get(object);
    }

    public List<CelestialObject> getObjectsSortByHieraticalDistance() {
        if (barycenter == null) updateBarycenter();
        List<HieraticalSystem> rootDt = new ArrayList<>(rootSystems);
        rootDt.sort((a, b) -> Double.compare(
                VectorOperations.distance(a.getPosition(), barycenter),
                VectorOperations.distance(b.getPosition(), barycenter)
        ));

        List<CelestialObject> result = new ArrayList<>();
        for (HieraticalSystem system : rootDt) {
            system.sortByDistance(result);
        }
        return result;
    }

    public List<HieraticalSystem> getRootSystems() {
        return rootSystems;
    }

    public List<CelestialObject> getAndClearNewlyDestroyed() {
        List<CelestialObject> nd = new ArrayList<>(newlyDestroyed);
        newlyDestroyed.clear();
        return nd;
    }

    public CelestialObject computeGravityMaster(CelestialObject target) {
        double maxF = 0.0;
        CelestialObject dominant = null;
        for (CelestialObject object : objects) {
            if (target == object) continue;
            double[] f = calculateGravitationalForce(target.mass, target.position,
                    object.mass, object.position);
            double fMag = VectorOperations.magnitude(f);
            if (fMag > maxF && target.mass < object.mass) {
                maxF = fMag;
                dominant = object;
            }
        }
        return dominant;
    }

    public CelestialObject computeHillMaster(CelestialObject target) {
        double maxMass = 0.0;
        CelestialObject mostMassive = null;

        for (CelestialObject coi : objects) {
            if (coi.mass > maxMass) {
                maxMass = coi.mass;
                mostMassive = coi;
            }
        }

        CelestialObject hillMaster = mostMassive;

        double minHillPercent = Double.MAX_VALUE;
        for (CelestialObject master : objects) {
            if (target == master || target.mass > master.mass) continue;
            if (master == mostMassive || master.hillRadius == Double.MAX_VALUE)
                continue;  // default hill is MAX_VALUE

            double distance = VectorOperations.distance(target.position, master.position);
            double masterHill = master.hillRadius;
            double hillPercent = distance / masterHill;
            if (hillPercent <= 1.0 && hillPercent < minHillPercent) {
                minHillPercent = hillPercent;
                hillMaster = master;
            }
        }

        return hillMaster;
    }

    public static double computeMaxRocheLimit(CelestialObject master) {
        return computeRocheLimitLiquid(master, Math.min(100.0, master.getDensity()));
    }

    public static double computeRocheLimitSolid(CelestialObject master) {
        return computeRocheLimitSolid(master, 1500.0);
    }

    public static double computeRocheLimitSolid(CelestialObject master, double smallDensity) {
        double r = master.getAverageRadius();
        double masterDensity = master.getDensity();
        return r * 1.260 * Math.cbrt(masterDensity / smallDensity);
    }

    public static double computeRocheLimitLiquid(CelestialObject master) {
        return computeRocheLimitLiquid(master, 1500.0);
    }

    public static double computeRocheLimitLiquid(CelestialObject master, double smallDensity) {
        double r = master.getAverageRadius();
        double masterDensity = master.getDensity();
        return r * 2.423 * Math.cbrt(masterDensity / smallDensity);
    }

    public SortedMap<Double, CelestialObject> getForcesOfAll(double[] position) {
        SortedMap<Double, CelestialObject> forces = new TreeMap<>();
        for (CelestialObject object : objects) {
            double[] f = calculateGravitationalForce(1, position, object.mass, object.position);
            double fMag = VectorOperations.magnitude(f);
            forces.put(fMag, object);
        }
        return forces;
    }

    public double[][] computeForcesGrid(double startX, double startY,
                                        double width, double height,
                                        int nCols, int nRows) {
        if (dimension < 2) {
            throw new RuntimeException("Cannot compute contour with system with dim < 2");
        }
        double[][] result = new double[nRows][nCols];
        double xTick = width / nCols;
        double yTick = height / nRows;

        for (int r = 0; r < nRows; r++) {
            double y = startY + (r + 0.0) * yTick;
            for (int c = 0; c < nCols; c++) {
                double x = startX + (c + 0.0) * xTick;

                double[] netForce = new double[dimension];
                for (CelestialObject object : objects) {
                    // just calculate it on the surface
                    double dx = x - object.position[0];
                    double dy = y - object.position[1];
                    double distance = Math.hypot(dx, dy);

                    // Calculate the force components
                    double forceMagnitude = G * object.mass / Math.pow(distance + 1e-9, gravityDtPower);
                    double forceX = forceMagnitude * dx / distance;
                    double forceY = forceMagnitude * dy / distance;

                    // Sum the vector components
                    netForce[0] += forceX;
                    netForce[1] += forceY;
                }
                result[r][c] = VectorOperations.magnitude(netForce);
            }
        }

        return result;
    }

    /**
     * Precondition: <code>object</code> has hill master
     */
    public EffectivePotential computeEffectivePotentialGrid(CelestialObject object,
                                                            double startX, double startY,
                                                            double width, double height,
                                                            int nCols, int nRows) {
        CelestialObject master = object.hillMaster;
        HieraticalSystem masterSystem = getHieraticalSystem(master);
        double[] barycenter = OrbitCalculator.calculateBarycenter(master, object);
        double[] barycenter2d = Arrays.copyOf(barycenter, 2);

//        System.out.println(Arrays.toString(object.velocity) + " " + Arrays.toString(masterSystem.getVelocity()));

        OrbitalElements orbitalElements = OrbitCalculator.computeOrbitSpecsPlanar(
                object,
                VectorOperations.subtract(object.velocity, masterSystem.getVelocity()),
                barycenter,
                master.getMass() + object.getMass(),
                G
        );

        if (orbitalElements.eccentricity > 0.1) {
            System.out.println("Large eccentricity warning: " + orbitalElements.eccentricity);
        }

        double[][] result = new double[nRows][nCols];
        double xTick = width / nCols;
        double yTick = height / nRows;

        double[] connection = new double[]{object.getX() - barycenter[0], object.getY() - barycenter[1]};
        double currentDt = Math.hypot(connection[0], connection[1]);

        double hillRadius = computeHillRadiusVsHillMaster(object);
        // todo: per vs aph
        double aph = orbitalElements.semiMajorAxis * (1 + orbitalElements.eccentricity);
        double per = orbitalElements.semiMajorAxis * (1 - orbitalElements.eccentricity);

        System.out.println(currentDt + ", ap: " + aph + ", pe: " + per);

//        double hill

        double minDtToMaster = Math.max(1e-9, currentDt - hillRadius * 1.5);
        double minDtToMasterCut = Math.max(1e-9, per - hillRadius * 2.0);
        double[] posAtMinDtToMaster = VectorOperations.add(
                barycenter2d,
                VectorOperations.scale(connection, minDtToMaster / currentDt)
        );

        double maxDtToMaster = currentDt + hillRadius * 1.5;
        double maxDtToMasterCut = aph + hillRadius * 2.5;
        double[] posAtMaxDtToMaster = VectorOperations.add(
                barycenter2d,
                VectorOperations.scale(connection, maxDtToMaster / currentDt)
        );

        EffectivePotentialCalculator calculator = new EffectivePotentialCalculator(G,
                master,
                object,
                orbitalElements,
                barycenter);

//        double minDtToObject = hillRadius * 0.5;

        double minEPCloseToMaster = calculator.compute(
                posAtMinDtToMaster[0],
                posAtMinDtToMaster[1]);
        double minEPFarToMaster = calculator.compute(
                posAtMaxDtToMaster[0],
                posAtMaxDtToMaster[1]);

        double minEP = Math.min(minEPCloseToMaster, minEPFarToMaster);

        for (int r = 0; r < nRows; r++) {
            double y = startY + (r + 0.0) * yTick;
            for (int c = 0; c < nCols; c++) {
                double x = startX + (c + 0.0) * xTick;
                double posDt = Math.hypot(x - barycenter[0], y - barycenter[1]);
                if (posDt > minDtToMasterCut && posDt < maxDtToMasterCut) {
//                    double posDtToObj = Math.hypot(x - object.getX(), y - object.getY());
//                    if (posDtToObj > minDtToObject) {
                    double actual = calculator.compute(
                            x,
                            y);
                    if (actual > minEP) {
                        result[r][c] = actual;
                    }
//                    }
                }
            }
        }

        // get lagrange points
        double[][] lagrangePoints = getLagrangePointsGradientDescent(calculator,
                hillRadius,
                hillRadius * 0.01,
                hillRadius * 1e-3);

        return new EffectivePotential(result, minEP, lagrangePoints);
    }

    private double[][] getLagrangePointsGradientDescent(EffectivePotentialCalculator calculator,
                                                        double hillRadius,
                                                        double tolerance,
                                                        double scaleFactor) {
        double masterX = calculator.master.getX();
        double masterY = calculator.master.getY();
        double objX = calculator.object.getX();
        double objY = calculator.object.getY();

        double[] masterPos = new double[]{masterX, masterY};
        double[] objPos = new double[]{objX, objY};
        double[] connection = new double[]{objX - masterX, objY - masterY};
        double[] direction = VectorOperations.normalize(connection);

        double[] possibleL1 = VectorOperations.add(objPos, VectorOperations.scale(direction, -hillRadius));
        double[] possibleL2 = VectorOperations.add(objPos, VectorOperations.scale(direction, hillRadius));
        double[] possibleL3 = VectorOperations.add(masterPos, VectorOperations.scale(connection, -1));
        double[] possibleL4 = VectorOperations.add(masterPos, VectorOperations.rotateVector2d(connection, -60));
        double[] possibleL5 = VectorOperations.add(masterPos, VectorOperations.rotateVector2d(connection, 60));

//        System.out.println("Poses");
//        System.out.println(Arrays.toString(objPos));
//        System.out.println(Arrays.toString(possibleL1));

        // find L1
        double[] L1 = findLagrangePoint(calculator, possibleL1, tolerance, scaleFactor);
        double[] L2 = findLagrangePoint(calculator, possibleL2, tolerance, scaleFactor);
        double[] L3 = findLagrangePoint(calculator, possibleL3, tolerance, scaleFactor);
        double[] L4 = findLagrangePoint(calculator, possibleL4, tolerance, scaleFactor);
        double[] L5 = findLagrangePoint(calculator, possibleL5, tolerance, scaleFactor);

        return new double[][]{L1, L2, L3, L4, L5};
    }

    public static double[] findLagrangePoint(EffectivePotentialCalculator calculator,
                                             double[] initialGuess,
                                             double tolerance,
                                             double scaleFactor) {
        double[] position = initialGuess.clone();

        while (true) {
            double[] grad = computeGradient(calculator, position[0], position[1], scaleFactor);
            if (grad[0] == 0.0 || grad[1] == 0.0) {
                System.out.println("Grad " + Arrays.toString(grad));
                break;
            }

            double[][] hessian = computeHessian(calculator, position[0], position[1], scaleFactor);

            double[] delta = solveLinearSystem(hessian, grad);
            position[0] -= delta[0];
            position[1] -= delta[1];

            if (Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]) < tolerance) {
                break;
            }
            if (Util.containsNaN(delta)) {
                System.out.println("Grad");
                System.out.println(Arrays.toString(grad));
                System.out.println(Util.containsNaN(grad));
                System.out.println("Hessian");
                System.out.println(Arrays.deepToString(hessian));
                System.out.println(Util.containsNaN(hessian));
                System.out.println("Delta");
                System.out.println(Arrays.toString(delta));
                throw new ArithmeticException();
            }
        }

        return position;
    }

    public static double[] computeGradient(EffectivePotentialCalculator calculator,
                                           double x, double y, double scaleFactor) {
        double dx = scaleFactor;
        double dy = scaleFactor;

        double dVdx = (calculator.compute(x + dx, y) - calculator.compute(x - dx, y)) / (2 * dx);
        double dVdy = (calculator.compute(x, y + dy) - calculator.compute(x, y - dy)) / (2 * dy);

        return new double[]{dVdx, dVdy};
    }

    public static double[][] computeHessian(EffectivePotentialCalculator calculator,
                                            double x, double y, double scaleFactor) {
        double dx = scaleFactor;
        double dy = scaleFactor;

        double epXy = calculator.compute(x, y);

        double d2Vdx2 = (calculator.compute(x + dx, y) -
                2 * epXy +
                calculator.compute(x - dx, y)) / (dx * dx);
        double d2Vdy2 = (calculator.compute(x, y + dy) -
                2 * epXy +
                calculator.compute(x, y - dy)) / (dy * dy);
        double d2Vdxdy = (calculator.compute(x + dx, y + dy) -
                calculator.compute(x + dx, y - dy) -
                calculator.compute(x - dx, y + dy) +
                calculator.compute(x - dx, y - dy)) / (4 * dx * dy);

        return new double[][]{{d2Vdx2, d2Vdxdy}, {d2Vdxdy, d2Vdy2}};
    }

    public static double[] solveLinearSystem(double[][] A, double[] b) {
        double detA = A[0][0] * A[1][1] - A[0][1] * A[1][0];
        double invA00 = A[1][1] / detA;
        double invA01 = -A[0][1] / detA;
        double invA10 = -A[1][0] / detA;
        double invA11 = A[0][0] / detA;

        double x = invA00 * b[0] + invA01 * b[1];
        double y = invA10 * b[0] + invA11 * b[1];

        return new double[]{x, y};
    }

    /*
     * All the following methods returns the non-zero velocity on the first two dimensions.
     * Velocity in any other dimensions are 0.
     */

    public double[] computeOrbitVelocity(CelestialObject dominant, CelestialObject placing,
                                         double[] planeNormal) {
        return computeVelocityOfN(dominant, placing, 1, planeNormal);
    }

    public double[] computeEscapeVelocity(CelestialObject dominant, CelestialObject placing,
                                          double[] planeNormal) {
        return computeVelocityOfN(dominant, placing, 2, planeNormal);
    }

    public double[] computeVelocityOfN(AbstractObject dominant, AbstractObject placing,
                                       double speedFactor, double[] planeNormal) {
        return computeVelocityOfN(dominant.getPosition(), dominant.getMass(),
                dominant.getVelocity(),
                placing,
                speedFactor,
                planeNormal);
    }

    public double[] computeVelocityOfN(double[] dominantPos,
                                       double dominantMass,
                                       double[] dominantVelocity,
                                       AbstractObject placing,
                                       double speedFactor,
                                       double[] planeNormal) {
        return computeVelocityOfN(dominantPos,
                dominantMass,
                dominantVelocity,
                placing.getPosition(),
                placing.getMass(),
                speedFactor,
                planeNormal);
    }

    public double[] computeVelocityOfN(double[] dominantPos,
                                       double dominantMass,
                                       double[] dominantVelocity,
                                       double[] placingPos,
                                       double placingMass,
                                       double speedFactor,
                                       double[] planeNormal) {
        if (dimension < 2) {
            throw new IllegalArgumentException("In space less than 2d, these velocity does not exist.");
        }

        double totalMass = dominantMass + placingMass;
        double[] barycenter = new double[dimension];
        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] = dominantPos[dim] * dominantMass + placingPos[dim] * placingMass;
        }

        double[] connection = new double[dimension];
        double sqrSum = 0;
        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] /= totalMass;
            double dtAtD = placingPos[dim] - barycenter[dim];
            connection[dim] = dtAtD;
            sqrSum += dtAtD * dtAtD;
        }
        double distance = Math.sqrt(sqrSum);

        double[] direction;
        if (planeNormal != null) {
//            System.out.println("Before: " + Arrays.toString(planeNormal) + " " + Arrays.toString(direction));
            direction = VectorOperations.crossProduct(planeNormal, connection);
            direction = VectorOperations.normalize(direction);
        } else {
            direction = new double[dimension];
            direction[direction.length - 1] = 1;
        }

        int sign = speedFactor < 0 ? -1 : 1;
        speedFactor = Math.abs(speedFactor);

        double speedMag = sign * Math.sqrt(speedFactor * G * totalMass / Math.pow(distance, gravityDtPower - 1));

        double[] velocity = new double[dimension];
        for (int dim = 0; dim < dimension; dim++) {
            velocity[dim] = direction[dim] * speedMag;
        }

//        System.out.println("After: " + Arrays.toString(planeNormal) + " " +
//                Arrays.toString(direction) + " " +
//                Arrays.toString(velocity));

        VectorOperations.addInPlace(velocity, dominantVelocity);

        return velocity;
    }

    public FullOrbitSpec computeOrbitOf(CelestialObject object, CelestialObject parent, boolean isPrimary) {
        AbstractObject child;
        if (isPrimary) {
            child = getHieraticalSystem(object);
        } else {
            child = object;
        }

        double[] barycenter = null;
        double[] refPos = null;
        double[] refVel = null;
        double totalMass = 0;
        HieraticalSystem parentSystem = getHieraticalSystem(parent);
        if (parentSystem != null && parentSystem.nChildren() > 1) {
            double distance = VectorOperations.distance(parent.getPosition(), child.getPosition());
            double systemDeviation = VectorOperations.distance(parent.getPosition(), parentSystem.getPosition());
            if (distance > systemDeviation) {
                // seems like circling around the whole system
                double[][] refPositionAndV = parentSystem.getBarycenterAndVelocityWithout(child, this);
                if (refPositionAndV != null) {
                    barycenter = parentSystem.getPosition();
                    refPos = refPositionAndV[0];
                    refVel = refPositionAndV[1];
                    totalMass = parentSystem.getMass();
                }
            }
        }

        if (barycenter == null) {
            barycenter = OrbitCalculator.calculateBarycenter(parent, child);
            refPos = parent.getPosition();
            refVel = parent.getVelocity();
            totalMass = parent.getMass() + child.getMass();
        }

        // velocity relative to parent system's barycenter movement
        double[] velocity = VectorOperations.subtract(child.getVelocity(),
                refVel);
        double[] position = VectorOperations.subtract(child.getPosition(),
                refPos);
        OrbitalElements specs = OrbitCalculator.computeOrbitSpecs3d(position,
                velocity,
                totalMass,
                G);
        return new FullOrbitSpec(specs,
                child,
                totalMass,
                barycenter);
    }

    private void updateBarycenter() {
        this.barycenter = barycenterOf(dimension, objects);
    }

    public double[] barycenter() {
        if (barycenter == null) updateBarycenter();
        return barycenter;
    }

    public double totalMass() {
        double totalMass = 0.0;
        for (CelestialObject co : objects) {
            totalMass += co.mass;
        }
        return totalMass;
    }

    public double effectiveMassAt(double[] position, CelestialObject self) {
        return calculateEffectiveMass(objects, position, self);
    }

    public double calculateEffectiveMass(Collection<CelestialObject> bodies,
                                         double[] newPosition,
                                         CelestialObject self) {
        double totalForce = 0;

        for (CelestialObject body : bodies) {
            double distance = VectorOperations.distance(body.position, newPosition);
            if (distance != 0) {
                totalForce += body.mass / (distance * distance);
            }
        }

        // Calculate effective mass assuming distance to barycenter
        double[] barycenter = barycenter();
        double distanceToBarycenter = VectorOperations.distance(newPosition, barycenter);

        return totalForce * distanceToBarycenter * distanceToBarycenter;
    }

    private double computeHillRadiusVsGravityMaster(CelestialObject co) {
        CelestialObject gravityMaster = co.getGravityMaster();
        if (gravityMaster == null) return Double.MAX_VALUE;

        return hillRadius(co, gravityMaster, G);
    }

    private double computeHillRadiusVsHillMaster(CelestialObject co) {
        if (co.hillMaster == null) {
            if (co.hillRadius == 0.0) return Double.MAX_VALUE;
            else return co.hillRadius;
        }

        double M = co.hillMaster.mass;
        double m = co.mass;
        HieraticalSystem parentSystem = getHieraticalSystem(co.hillMaster);
        double[] barycenter = OrbitCalculator.calculateBarycenter(co, co.hillMaster);

        double[] ae = OrbitCalculator.computeBasic(co,
                barycenter,
                M + m,
                VectorOperations.subtract(co.getVelocity(), parentSystem.getVelocity()),
                G);
        double inside = m / (3 * (M + m));
        double cbrt = Math.cbrt(inside);
        return ae[0] * (1 - ae[1]) * cbrt;
    }

    public static double hillRadius(CelestialObject target, CelestialObject master, double G) {
        double m1 = master.mass;
        double m2 = target.mass;
        double[] barycenter = OrbitCalculator.calculateBarycenter(target, master);
        double[] v = VectorOperations.subtract(target.velocity, master.velocity);
        double[] ae = OrbitCalculator.computeBasic(target, barycenter, m1 + m2, v, G);
        double inside = m2 / (3 * (m1 + m2));
        double cbrt = Math.cbrt(inside);
        return ae[0] * (1 - ae[1]) * cbrt;
    }

    public CelestialObject findByName(String name) {
        for (CelestialObject co : objects) {
            if (co.id.equals(name)) return co;
        }
        return null;
    }

    public double[][] approximateLagrangePoints(CelestialObject co) {
        if (co.hillMaster == null) {
            return null;
        }

        double M = co.hillMaster.mass;
        double m = co.mass;
        double[] masterPos = co.hillMaster.position;
        HieraticalSystem parentSystem = getHieraticalSystem(co.hillMaster);
        double[] barycenter = OrbitCalculator.calculateBarycenter(co, co.hillMaster);
        double[] relativePos = VectorOperations.subtract(co.position, masterPos);

        double[] ae = OrbitCalculator.computeBasic(co,
                barycenter,
                M + m,
                VectorOperations.subtract(co.getVelocity(), parentSystem.getVelocity()),
                G);
        double a = ae[0];
//        double e = ae[1];

        double[] direction = VectorOperations.normalize(relativePos);
        double[] midPoint = VectorOperations.scale(
                VectorOperations.add(masterPos, co.position), 0.5);
        double distance = VectorOperations.distance(masterPos, co.position);

        double angle1 = Math.PI / 3;
        double[] perpendicular1 = {
                direction[0] * Math.cos(angle1) - direction[1] * Math.sin(angle1),
                direction[0] * Math.sin(angle1) + direction[1] * Math.cos(angle1),
                0
        };
        double angle2 = -Math.PI / 3;
        double[] perpendicular2 = {
                direction[0] * Math.cos(angle2) - direction[1] * Math.sin(angle2),
                direction[0] * Math.sin(angle2) + direction[1] * Math.cos(angle2),
                0
        };

        double[] L1 = new double[dimension];
        double[] L2 = new double[dimension];

        // compute L3
        double r3 = -a * (1 + 5 * m / (12 * M));

        double[] L3 = VectorOperations.add(masterPos, VectorOperations.scale(direction, r3));

        // compute L4 and L5
        double[] L4 = VectorOperations.add(barycenter, VectorOperations.scale(perpendicular1, distance));
        double[] L5 = VectorOperations.add(barycenter, VectorOperations.scale(perpendicular2, distance));

        return new double[][]{L1, L2, L3, L4, L5};
    }

    void updateIndependentStatus() {
        for (CelestialObject co : objects) {
            co.updateStatus(true);
        }
    }

    boolean updateDependentStatus(double timeStep) {
        boolean changed = false;
        List<Star> sources = getAllLightSources();
        int n = objects.size();
        for (int i = n - 1; i >= 0; i--) {
            CelestialObject co = objects.get(i);
            co.updateStatus(false);  // majorly setup for the comets
            if (co.getStatus() instanceof Comet comet) {
                double lossRate = co.vapor(comet, timeStep, sources);
                if (co.getMass() <= 0) {
                    System.out.println(co.getId() + " has vaporized");
                    objects.remove(i);
                    changed = true;
                    continue;
                }
                comet.updateTails(this, sources, lossRate, timeStep);
            }
        }
        return changed;
    }

    public List<Star> getAllLightSources() {
        List<Star> result = new ArrayList<>();
        for (CelestialObject co : objects) {
            if (co.getStatus() instanceof Star star) {
                result.add(star);
            }
        }
        return result;
    }

    protected void performTemperatureChange(double timeStep) {
        int iteration = (int) Math.min(32, timeStep);
        double each = timeStep / iteration;

        for (int i = 0; i < iteration; i++) {
            for (CelestialObject co : objects) {
                double luminosity = co.getLuminosity();
                if (luminosity > 0) {
                    // is a light source
                    double[] sourcePos = co.getPosition().clone();
                    for (CelestialObject receiver : objects) {
                        if (co != receiver) {
                            receiver.receiveLight(sourcePos, luminosity, each);
                        }
                    }
                } else {
                    co.emitThermalPower(each);
                }
            }
        }
    }

    protected void updateTidal(double timeStep) {
        for (CelestialObject object : objects) {
            // for each pair of parent-children, compute them
            if (object.hillMaster != null) {
                // both use a same set of orbit params, 
                // otherwise the hill master will have too small semi-major
                tidalBrake(object.hillMaster, object, timeStep);
                tidalBrake(object, object.hillMaster, timeStep);
            }
        }
    }

    private void tidalBrake(CelestialObject primary,
                            CelestialObject secondary,
                            double timeStep) {

        double primaryRotKE = primary.rotationalKineticEnergy();
        double secondaryTransKE = secondary.transitionalKineticEnergy();

        double[] relVel = VectorOperations.subtract(secondary.velocity, primary.velocity);
        double[] relPos = VectorOperations.subtract(secondary.position, primary.position);
        double dt = VectorOperations.magnitude(relPos);
        double[] angularV = VectorOperations.scale(primary.rotationAxis, primary.angularVelocity);
        double[] orbitAngularMomentum = VectorOperations.crossProduct(relPos, relVel);
        double[] orbitPlaneNormal = VectorOperations.normalize(orbitAngularMomentum);
//        double orbitAngularVel = Math.sqrt(G * primary.mass / Math.pow(dt, 3));
        double orbitAngularVel = VectorOperations.magnitude(relVel) / dt;

        double alignment = VectorOperations.dotProduct(angularV, orbitAngularMomentum);

//        System.out.println(secondary.name + " " + alignment + " " + orbitAngularVel + " " + primary.angularVelocity);

        // prograde or retrograde
        double sign = alignment >= 0 ? 1 : -1;

        // must be negative
        double tidalBrakingAngularVel = tidalBrakingVelocity(primary,
                secondary,
                dt) * tidalEffectFactor * timeStep;
//        double tidalSpeedChange = tidalSpeedChange(primary,
//                secondary,
//                dt) * tidalEffectFactor * timeStep;

        double w = (primary.angularVelocity - orbitAngularVel);

//        System.out.println(secondary.name + " " + dt + 
//                ", angular: " + 
//                orbitAngularVel + " " + primary.angularVelocity +
//                ", brake: " + tidalBrakingAngularVel * w + 
//                ", speed change: " + (-tidalSpeedChange * w) + 
//                ", w: " + w);

//        System.out.println(primary.name + " " + primary.angularVelocity + " " + (orbitAngularVel * sign));
        primary.angularVelocity += tidalBrakingAngularVel * w;

        // change of axis
        // Compute the angle of misalignment theta
        double cosTheta = VectorOperations.dotProduct(
                primary.rotationAxis, orbitPlaneNormal);
        double theta = Math.acos(cosTheta);
        if (theta >= 1e-5) {
            // Compute the cross product of the rotation axis and orbital normal vector
            double[] axisCross = VectorOperations.crossProduct(primary.rotationAxis, orbitPlaneNormal);
            double[] axisCrossUnit = VectorOperations.normalize(axisCross);

            double cMinusA = 0.4 * primary.mass * Math.pow(primary.getEquatorialRadius(), 2) *
                    primary.tidalLoveNumber / 3;
            // Compute the torque constant tau_0
            double tau_0 = (3 * G * secondary.mass * cMinusA) / Math.pow(dt, 3);
            double tau = tau_0 * Math.sin(theta) * (tidalEffectFactor * 3e-26);

//        // Final torque
//        double[] torque = VectorOperations.scale(axisCross, tau_0 * Math.sin(theta));
//        System.out.println(primary.name + " " + theta + " " + Arrays.toString(torque) + " " + VectorOperations.magnitude(torque));

            // Initial angular momentum
            double[] L0 = primary.angularMomentum();
            // Change in angular momentum due to torque
            double axisChange = tau / VectorOperations.magnitude(L0) * timeStep;
//        System.out.println(primary.name + " " + axisChange);

            double[] newAxis = VectorOperations.rotateVector(primary.rotationAxis, axisCrossUnit, axisChange);
            primary.updateRotationAxis(newAxis);
        }

//        double[] deltaL = VectorOperations.scale(torque, timeStep * 1e3);
        // New angular momentum
//        double[] Lnew = VectorOperations.add(L0, deltaL);
        // Compute the new axis of rotation
//        double[] newAxis = VectorOperations.normalize(Lnew);
//        primary.updateRotationAxis(newAxis);
//        System.out.println(primary.name + " " + Arrays.toString(primary.rotationAxis) + " " + Arrays.toString(newAxis));

        if (secondary.mass < primary.mass) {
            // todo: different equator, polar radius
            double t = primary.getAverageRadius() / dt;
            double[] bulgePos = VectorOperations.scale(relPos, t);  // bulge pos relative to primary's center
            double[] nextBulgePos = VectorOperations.rotateVector(bulgePos,
                    primary.getRotationAxis(),
                    primary.angularVelocity);
            double[] nextBulgePosSky = VectorOperations.scale(nextBulgePos, 1 / t);  // todo: equator, polar
            double[] nextSecondaryPos = VectorOperations.add(relPos, secondary.getVelocity());
            double[] forceDirection = VectorOperations.subtract(nextBulgePosSky, nextSecondaryPos);
            double strength = -tidalSpeedChange(primary, secondary, dt) * tidalEffectFactor * timeStep * 5e3;
            double[] tidalAcc = VectorOperations.scale(VectorOperations.normalize(forceDirection), strength);
            VectorOperations.addInPlace(secondary.velocity, tidalAcc);

            // pull to the equator
            double latitude = VectorOperations.latitudeOf(primary.getRotationAxis(), relPos);
            int latSign = latitude < 0 ? -1 : 1;
            latitude = Math.abs(latitude);
            double towardsEquatorStrength = Math.sin(latitude) * strength * primary.getOblateness() * 5e2;
            double[] towardsEquator = VectorOperations.scale(
                    VectorOperations.computeNorthVector(primary.getRotationAxis(), relPos), -latSign);
//            double[] towardsEquator = VectorOperations.scale(primary.getRotationAxis(), -1);
            towardsEquator = VectorOperations.scale(towardsEquator, towardsEquatorStrength);
//            System.out.println(Math.toDegrees(latSign * latitude) + " " + Arrays.toString(towardsEquator));
            VectorOperations.addInPlace(secondary.velocity, towardsEquator);

//            double w2 = (primary.angularVelocity * sign - orbitAngularVel);
//            double speedChange = -tidalSpeedChange * w2 * 5e6;
////            System.out.println(primary.name + " " + primary.angularVelocity * sign + " " + orbitAngularVel);
////            System.out.println(speedChange);
//            double[] velChange = VectorOperations.scale(VectorOperations.normalize(relVel),
//                    speedChange);
//            VectorOperations.addInPlace(secondary.velocity, velChange);
        }

        double newPrimaryRotKE = primary.rotationalKineticEnergy();
        double newSecondaryTransKE = secondary.transitionalKineticEnergy();

        primary.internalThermalEnergy += (primaryRotKE - newPrimaryRotKE);
        primary.internalThermalEnergy += (secondaryTransKE - newSecondaryTransKE);
    }

    /**
     * The change of angular speed to the primary body
     */
    public double tidalBrakingVelocity(CelestialObject primary,
                                       CelestialObject secondary,
                                       double distance) {

//        double up = 3 * secondary.tidalLoveNumber * G * Math.pow(primary.mass, 2) *
//                Math.pow(secondary.getEquatorialRadius(), 3);
//        double down = primary.dissipationFunction * Math.pow(distance, 6) *
//                secondary.momentOfInertiaRot();
//        return -up / down;
        double up = primary.tidalLoveNumber * Math.pow(primary.equatorialRadius, 3) * secondary.mass;
        double down = primary.dissipationFunction * primary.mass * Math.pow(distance, 6);
        return -up / down;
    }

    /**
     * The tidal speed change to the secondary body
     */
    public double tidalSpeedChange(CelestialObject primary,
                                   CelestialObject secondary,
                                   double distance) {
        double kUp = Math.pow(primary.equatorialRadius, 5) * secondary.getMass() * Math.sqrt(G * primary.getMass());
        double kDown = primary.dissipationFunction * primary.getMass() * secondary.getMass();
        double k = kUp / kDown;
        return -k / Math.pow(distance, 11.0 / 2);
    }

    public static double[] barycenterVelocityOf(AbstractObject... celestialObjects) {
        int dimension = celestialObjects[0].getVelocity().length;
        double totalMass = 0.0;
        double[] velocity = new double[dimension];
        for (AbstractObject co : celestialObjects) {
            totalMass += co.getMass();
            for (int dim = 0; dim < dimension; dim++) {
                velocity[dim] += co.getVelocity()[dim] * co.getMass();
            }
        }
        for (int dim = 0; dim < dimension; dim++) {
            velocity[dim] /= totalMass;
        }
        return velocity;
    }

    public static double[] barycenterOf(int dimension, AbstractObject... celestialObjects) {
        double totalMass = 0.0;
        double[] barycenter = new double[dimension];
        for (AbstractObject co : celestialObjects) {
            totalMass += co.getMass();
            for (int dim = 0; dim < dimension; dim++) {
                barycenter[dim] += co.getPosition()[dim] * co.getMass();
            }
        }
        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] /= totalMass;
        }
        return barycenter;
    }

    public static double[] barycenterOf(int dimension, List<? extends AbstractObject> celestialObjects) {
        double totalMass = 0.0;
        double[] barycenter = new double[dimension];
        for (AbstractObject co : celestialObjects) {
            totalMass += co.getMass();
            for (int dim = 0; dim < dimension; dim++) {
                barycenter[dim] += co.getPosition()[dim] * co.getMass();
            }
        }
        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] /= totalMass;
        }
        return barycenter;
    }

    public enum SimResult {
        NORMAL,
        NUM_CHANGED,
        TOO_FAST
    }
}
