package com.trashsoftware.gravity2.physics;

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
    public static final double DISASSEMBLE_LAMBDA = 3e-5;
    public static final double MAX_TIME_AFTER_DIE = 3e5;
    
    public static final double PLANET_MAX_MASS = 0.8;

    protected double timeStep = 1;
    private transient double lastTimeStepAccumulator = 0;
    private double timeStepAccumulator = 0;
    private final int dimension;
    protected double tidalBrakeFactor = 1;

    private final double G;
    private final double gravityDtPower;
    private double epsilon = 0.0;
    private double cutOffForce;

    /**
     * All objects, always sorted from massive to light
     */
    private final List<CelestialObject> objects = new ArrayList<>();
    private final Map<CelestialObject, Deque<double[]>> recentPaths = new HashMap<>();  // [x,y,timeStep]
    private final Deque<double[]> barycenterPath = new ArrayDeque<>();
    private double[] barycenter;

    // temp buffers
    private double[][] forcesBuffer;
    private double[] dimDtBuffer;
    private final List<CelestialObject> debrisBuffer = new ArrayList<>();
    private final List<CelestialObject> newlyDestroyed = new ArrayList<>();

    private transient final Map<CelestialObject, HieraticalSystem> systemMap = new HashMap<>();
    private final transient List<HieraticalSystem> rootSystems = new ArrayList<>();
    private transient int forceCounter1, forceCounter2;

    private final ForkJoinPool forceCalculationPool = new ForkJoinPool();
    protected final Random random = new Random();

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

    public double getG() {
        return G;
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
            if (handleCollisions(objects)) {
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
                        System.out.println("Too fast: " + object.name);
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

        boolean changed = changeHappen || objects.size() != nObj;
        if (changed) {
            keepOrder();
            if (result == SimResult.NORMAL) result = SimResult.NUM_CHANGED;
        }

        if (!highPerformanceMode) {
            updateMasters();

            for (CelestialObject co : objects) {
                co.updateRotation(performedTimeSteps);
            }
            updateTidal(performedTimeSteps);
        }

        return result;
    }

    private boolean handleCollisions(List<CelestialObject> objects) {
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
//                            lighter.gainMattersFrom(this, heavier);
                            CelestialObject debris = heavier.disassemble(this, lighter, actualRoche);
                            if (debris != null) {
                                debrisBuffer.add(debris);
                            }
                        }
                    }

                    if (distance < heavier.possibleRocheLimit) {
                        // This is the most common scenario
                        // if the above happen, this will also likely to happen
                        double actualRoche = computeRocheLimitSolid(heavier, lighter.getDensity());
                        if (distance - lighter.getAverageRadius() < actualRoche) {
//                            heavier.gainMattersFrom(this, lighter);
                            CelestialObject debris = lighter.disassemble(this, heavier, actualRoche);
                            if (debris != null) {
                                debrisBuffer.add(debris);
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
            double forceMagnitude = G * coi.mass * coj.mass / Math.pow(distance, gravityDtPower);
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
                    celestialObject.name,
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
        String origName = newObject.getName();
        String name = origName;
        int counter = 0;
        while (nameConflict(name)) {
            counter += 1;
            name = origName + "-" + counter;
        }
        if (!name.equals(origName)) newObject.name = name;
    }

    private boolean nameConflict(String name) {
        for (CelestialObject object : objects) {
            if (name.equals(object.getName())) return true;
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
        double upper = -G * co1.mass * co2.mass * Math.pow(distance, 1 - gravityDtPower);
        double lower = gravityDtPower - 1;
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
            totalIE += gravitationalBindingEnergyOf(co) + co.getThermalEnergy();
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
            coi.gravityMaster = null;
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
            while (!gravities.isEmpty()) {
                var biggest = gravities.pollLastEntry();
                if (biggest.getValue().mass > object.mass) {
                    if (biggest.getValue().mass * PLANET_MAX_MASS > object.mass) {
                        object.gravityMaster = biggest.getValue();
                    }
                    break;
                }
            }
//            System.out.println(object + " gm: " + object.gravityMaster);
        }

        for (CelestialObject object : objects) {
            object.hillRadius = computeHillRadiusVsGravityMaster(object);  // temporary
            if (mostMassive != null &&
                    object.mass > mostMassive.mass * PLANET_MAX_MASS) object.hillMaster = null;
            else object.hillMaster = object.gravityMaster;

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
        if (probableHillMasters.isEmpty()) throw new RuntimeException("Cannot find a hill master");
        else if (probableHillMasters.size() == 1) return probableHillMasters.get(0);

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

    public double[] computeOrbitVelocity(CelestialObject dominant, CelestialObject placing) {
        return computeVelocityOfN(dominant, placing, 1);
    }

    public double[] computeEscapeVelocity(CelestialObject dominant, CelestialObject placing) {
        return computeVelocityOfN(dominant, placing, 2);
    }

    public double[] computeVelocityOfN(CelestialObject dominant, CelestialObject placing,
                                       double speedFactor) {
        return computeVelocityOfN(dominant.position, dominant.mass,
                dominant.velocity,
                placing,
                speedFactor);
    }

    public double[] computeVelocityOfN(double[] dominantPos,
                                       double dominantMass,
                                       double[] dominantVelocity,
                                       CelestialObject placing,
                                       double speedFactor) {
        return computeVelocityOfN(dominantPos,
                dominantMass,
                dominantVelocity,
                placing.position,
                placing.mass,
                speedFactor);
    }

    public double[] computeVelocityOfN(double[] dominantPos,
                                       double dominantMass,
                                       double[] dominantVelocity,
                                       double[] placingPos,
                                       double placingMass,
                                       double speedFactor) {
        if (dimension < 2) {
            throw new IllegalArgumentException("In space less than 2d, these velocity does not exist.");
        }

        double totalMass = dominantMass + placingMass;
        double[] barycenter = new double[dimension];
        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] = dominantPos[dim] * dominantMass + placingPos[dim] * placingMass;
        }

        for (int dim = 0; dim < dimension; dim++) {
            barycenter[dim] /= totalMass;
        }

        double sqrSum = 0;
        double[] dtAtD = new double[dimension];
        for (int d = 0; d < dimension; d++) {
            dtAtD[d] = placingPos[d] - barycenter[d];
            sqrSum += dtAtD[d] * dtAtD[d];
        }
        double distance = Math.sqrt(sqrSum);

        int sign = speedFactor < 0 ? -1 : 1;
        speedFactor = Math.abs(speedFactor);

        double speedMag = Math.sqrt(speedFactor * G * totalMass / Math.pow(distance, gravityDtPower - 1));
        double[] res = Arrays.copyOf(dominantVelocity, dimension);

        double vx = dtAtD[1] / distance * speedMag * sign;
        double vy = -dtAtD[0] / distance * speedMag * sign;
        
        res[0] += vx;
        res[1] += vy;

        return res;
    }

    private void updateBarycenter() {
        this.barycenter = barycenterOf(objects);
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
        if (co.gravityMaster == null) return Double.MAX_VALUE;

        double m1 = co.gravityMaster.mass;
        double m2 = co.mass;
        double[] barycenter = OrbitCalculator.calculateBarycenter(co, co.gravityMaster);
        double[] v = VectorOperations.subtract(co.velocity, co.gravityMaster.velocity);
        double[] ae = OrbitCalculator.computeBasic(co, barycenter, m1 + m2, v, G);
        double inside = m2 / (3 * (m1 + m2));
        double cbrt = Math.cbrt(inside);
        return ae[0] * (1 - ae[1]) * cbrt;
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

    public CelestialObject findByName(String name) {
        for (CelestialObject co : objects) {
            if (co.name.equals(name)) return co;
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
    
    protected void updateTidal(double timeStep) {
        for (CelestialObject object : objects) {
            // for each pair of parent-children, compute them
            if (object.hillMaster != null) {
                double[] bc = barycenterOf(object, object.hillMaster);
                double[] relVel = VectorOperations.subtract(object.velocity, object.hillMaster.velocity);
                double[] ae = OrbitCalculator.computeBasic(
                        object,
                        bc,
                        object.mass + object.hillMaster.mass,
                        relVel,
                        G
                );
//                double[] bcVel = barycenterVelocityOf(object, object.hillMaster);
                double[] r = VectorOperations.subtract(object.position, object.hillMaster.position);
                double dt = VectorOperations.magnitude(r);
                double[] avVector = VectorOperations.crossProduct(r, relVel);
                double orbitAngularVel = -avVector[avVector.length - 1] / (dt * dt);
                
                // both use a same set of orbit params, 
                // otherwise the hill master will have too small semi-major
                tidalBrake(object.hillMaster, object, ae[0], orbitAngularVel, timeStep);
                tidalBrake(object, object.hillMaster, ae[0], orbitAngularVel, timeStep);
            }
        }
    }
    
    private void tidalBrake(CelestialObject primary, 
                            CelestialObject secondary, 
                            double semiMajor,
                            double orbitAngularVel,
                            double timeStep) {
        
        double rotationKE = secondary.rotationalKineticEnergy();
        
        // must be negative
        double tidalBrakingAngularVel = tidalBrakingVelocity(primary,
                secondary,
                semiMajor) * tidalBrakeFactor * timeStep;
//        System.out.println(secondary.name + " " + semiMajor + " " + orbitAngularVel + " " + tidalBrakingAngularVel);
        // todo: rotation axis
        if (secondary.angularVelocity > orbitAngularVel) {
            secondary.angularVelocity += tidalBrakingAngularVel;
            if (secondary.angularVelocity < orbitAngularVel) {
                // reduced too much
                secondary.angularVelocity = orbitAngularVel;
            }
        } else if (secondary.angularVelocity < orbitAngularVel) {
            secondary.angularVelocity -= tidalBrakingAngularVel;
            if (secondary.angularVelocity > orbitAngularVel) {
                // reduced too much
                secondary.angularVelocity = orbitAngularVel;
            }
        }
        
        double newRotationKE = secondary.rotationalKineticEnergy();
        secondary.thermalEnergy += (rotationKE - newRotationKE);
    }

    public double tidalBrakingVelocity(CelestialObject primary, 
                                       CelestialObject secondary,
                                       double semiMajor) {
        
        double up = 3 * secondary.tidalLoveNumber * G * Math.pow(primary.mass, 2) *
                Math.pow(secondary.getEquatorialRadius(), 3);
        double down = secondary.dissipationFunction * Math.pow(semiMajor, 6) *
                secondary.momentOfInertiaRot();
        return -up / down;
    }
    
    public double tidalOrbitalDecay(CelestialObject primary, 
                                    CelestialObject secondary,
                                    double semiMajor) {
        double part1 = -9.0 / 2.0 * secondary.tidalLoveNumber * 
                Math.pow(secondary.getEquatorialRadius(), 5) / secondary.dissipationFunction;
        double part2 = primary.mass / secondary.mass;
        double part3 = Math.sqrt(G * primary.mass / Math.pow(semiMajor, 7));
        return part1 * part2 * part3;
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

    public static double[] barycenterOf(AbstractObject... celestialObjects) {
        int dimension = celestialObjects[0].getPosition().length;
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

    public static double[] barycenterOf(List<? extends AbstractObject> celestialObjects) {
        int dimension = celestialObjects.get(0).getPosition().length;
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
