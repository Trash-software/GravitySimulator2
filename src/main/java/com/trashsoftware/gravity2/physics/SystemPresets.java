package com.trashsoftware.gravity2.physics;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.gui.JmeApp;

import java.util.*;

public class SystemPresets {

    public static final double SOLAR_MASS = 1.989e30;
    public static final double SOLAR_DENSITY = 1410;
    public static final double SOLAR_LUMINOSITY = 3.828e26;
    public static final double JUPITER_MASS = 1.899e27;
    public static final double AU = 149598262000.0;  // 1 AU in meter

    public static final Map<String, String> TEXTURES = new HashMap<>();

    static {
        TEXTURES.put("Sun", "com/trashsoftware/gravity2/textures/sunmap.jpg");
        TEXTURES.put("Mercury", "com/trashsoftware/gravity2/textures/2k_mercury.jpg");
        TEXTURES.put("Venus", "com/trashsoftware/gravity2/textures/venusmap.jpg");

        TEXTURES.put("Earth", "com/trashsoftware/gravity2/textures/earth/earth2k.jpg");
        TEXTURES.put("Moon", "com/trashsoftware/gravity2/textures/earth/moon.png");

        TEXTURES.put("Mars", "com/trashsoftware/gravity2/textures/marsmap1k.jpg");

        TEXTURES.put("Jupiter", "com/trashsoftware/gravity2/textures/jupiter/jupiter2k.jpg");
        TEXTURES.put("Callisto", "com/trashsoftware/gravity2/textures/jupiter/callisto.jpg");
        TEXTURES.put("Europa", "com/trashsoftware/gravity2/textures/jupiter/europa.jpg");
        TEXTURES.put("Ganymede", "com/trashsoftware/gravity2/textures/jupiter/ganymede.jpg");
        TEXTURES.put("Io", "com/trashsoftware/gravity2/textures/jupiter/io.jpg");

        TEXTURES.put("Saturn", "com/trashsoftware/gravity2/textures/saturn/saturnmap.jpg");
        TEXTURES.put("Dione", "com/trashsoftware/gravity2/textures/saturn/dione-ciclops.jpg");
        TEXTURES.put("Enceladus", "com/trashsoftware/gravity2/textures/saturn/enceladus.jpg");
        TEXTURES.put("Iapetus", "com/trashsoftware/gravity2/textures/saturn/iapetus2k.jpg");
        TEXTURES.put("Rhea", "com/trashsoftware/gravity2/textures/saturn/rhea.jpg");
        TEXTURES.put("Tethys", "com/trashsoftware/gravity2/textures/saturn/tethys2k.jpg");
        TEXTURES.put("Titan", "com/trashsoftware/gravity2/textures/saturn/titan.png");

        TEXTURES.put("Uranus", "com/trashsoftware/gravity2/textures/uranus.JPG");

        TEXTURES.put("Neptune", "com/trashsoftware/gravity2/textures/neptune/neptune_current.jpg");
        TEXTURES.put("Triton", "com/trashsoftware/gravity2/textures/neptune/triton.jpg");

        TEXTURES.put("Pluto", "com/trashsoftware/gravity2/textures/pluto/pluto.jpg");
        TEXTURES.put("Charon", "com/trashsoftware/gravity2/textures/pluto/charon.jpg");

        TEXTURES.put("Ceres", "com/trashsoftware/gravity2/textures/2k_ceres_fictional.jpg");
        TEXTURES.put("Eris", "com/trashsoftware/gravity2/textures/2k_eris_fictional.jpg");
        TEXTURES.put("Haumea", "com/trashsoftware/gravity2/textures/2k_haumea_fictional.jpg");
        TEXTURES.put("Makemake", "com/trashsoftware/gravity2/textures/2k_makemake_fictional.jpg");
    }

    // Moon
    public static ObjectInfo moon = new ObjectInfo(
            "Moon", 7.342e22, 1737.1, 1738.1, 1735.6, 384400, 0.0549, 318.15, 5.145, 125.08, 135.27, 6.68, 27.322,
            "#C0C0C0", 0.024, 30
    );

    // Moons of Mars
    public static ObjectInfo phobos = new ObjectInfo(
            "Phobos", 1.0659e16, 11.1, 13, 11, 9376, 0.0151, 150.057, 1.093, 49.247, 23.51, 0.0, 0.319,
            "#704241", 0.005, 100
    );
    public static ObjectInfo deimos = new ObjectInfo(
            "Deimos", 1.4762e15, 6.2, 7.5, 6.1, 23460, 0.00033, 260.73, 0.93, 49.29, 176.12, 0.0, 1.263,
            "#A0522D", 0.005, 100
    );

    // Moons of Jupiter
    public static ObjectInfo io = new ObjectInfo(
            "Io", 8.93e22, 1821.6, 1821.6, 1821.6, 421700, 0.0041, 84.129, 0.036, 43.977, 99.5, 0.05, 1.769,
            "#FFD700", 0.015, 50
    );
    public static ObjectInfo europa = new ObjectInfo(
            "Europa", 4.8e22, 1560.8, 1560.8, 1560.8, 670900, 0.009, 88.970, 0.47, 219.106, 74.5, 0.1, 3.551,
            "#87CEEB", 0.010, 100
    );
    public static ObjectInfo ganymede = new ObjectInfo(
            "Ganymede", 1.48e23, 2634.1, 2634.1, 2634.1, 1070400, 0.0013, 192.417, 0.177, 63.552, 125.3, 0.33, 7.155,
            "#DCDCDC", 0.020, 200
    );
    public static ObjectInfo callisto = new ObjectInfo(
            "Callisto", 1.08e23, 2410.3, 2410.3, 2410.3, 1882700, 0.0074, 298.848, 0.192, 298.848, 42.9, 0.0, 16.689,
            "#708090", 0.010, 300
    );

    // Moons of Saturn
    public static ObjectInfo titan = new ObjectInfo(
            "Titan", 1.345e23, 2576, 2575.5, 2573.4, 1222000, 0.0288, 186.585, 0.348, 168.199, 132.8, 0.3, 15.945,
            "#D2B48C", 0.105, 1500
    );
    public static ObjectInfo rhea = new ObjectInfo(
            "Rhea", 2.31e21, 763.8, 764, 762, 527040, 0.001, 252.13, 0.345, 34.333, 75.9, 0.0, 4.518,
            "#F5F5F5", 0.020, 1000
    );
    public static ObjectInfo iapetus = new ObjectInfo(
            "Iapetus", 1.8e21, 734.5, 734.5, 734.5, 3561300, 0.0286, 83.01, 15.47, 79.321, 310.2, 0.0, 79.321,
            "#2F4F4F", 0.015, 1200
    );
    public static ObjectInfo dione = new ObjectInfo(
            "Dione", 1.095e21, 561.4, 561.4, 561.4, 377400, 0.0022, 292.429, 0.028, 168.766, 183.2, 0.0, 2.737,
            "#D3D3D3", 0.025, 1000
    );
    public static ObjectInfo tethys = new ObjectInfo(
            "Tethys", 6.18e20, 531.1, 531.1, 531.1, 294619, 0.0001, 243.458, 1.09, 72.366, 23.4, 0.0, 1.887,
            "#AFEEEE", 0.030, 900
    );
    public static ObjectInfo enceladus = new ObjectInfo(
            "Enceladus", 1.08e20, 252.1, 252.1, 252.1, 238041, 0.0047, 270.876, 0.009, 167.583, 102.9, 0.0, 1.370,
            "#FFFFFF", 0.020, 700
    );

    // Moons of Uranus
    public static ObjectInfo titania = new ObjectInfo(
            "Titania", 3.42e21, 788.4, 788.4, 788.4, 436300, 0.0011, 77.874, 0.34, 99.771, 259.8, 0.0, 8.706,
            "#98FB98", 0.025, 1100
    );
    public static ObjectInfo oberon = new ObjectInfo(
            "Oberon", 3.01e21, 761.4, 761.4, 761.4, 583500, 0.0014, 77.742, 0.07, 103.537, 172.1, 0.0, 13.463,
            "#556B2F", 0.022, 1050
    );
    public static ObjectInfo umbriel = new ObjectInfo(
            "Umbriel", 1.27e21, 584.7, 584.7, 584.7, 266000, 0.0039, 84.709, 0.128, 118.364, 10.3, 0.0, 4.144,
            "#008080", 0.020, 1000
    );
    public static ObjectInfo ariel = new ObjectInfo(
            "Ariel", 1.29e21, 578.9, 578.9, 578.9, 191020, 0.0012, 84.334, 0.041, 171.344, 216.7, 0.0, 2.520,
            "#F0FFF0", 0.023, 980
    );
    public static ObjectInfo miranda = new ObjectInfo(
            "Miranda", 6.6e19, 235.8, 235.8, 235.8, 129390, 0.0013, 84.548, 4.338, 65.963, 354.2, 0.0, 1.413,
            "#00FA9A", 0.018, 960
    );

    // Moons of Neptune
    public static ObjectInfo triton = new ObjectInfo(
            "Triton", 2.14e22, 1353.4, 1353.4, 1353.4, 354760, 0.000016, 254.168, 156.865, 298.848, 139.5, 0.0, 5.877,
            "#FFB6C1", 0.030, 750
    );

    // Moons of Pluto
    public static ObjectInfo charon = new ObjectInfo(
            "Charon", 1.52e21, 606, 606, 606, 19591, 0.0002, 96.108, 0.001, 209.07, 92.5, 0.0, 6.387,
            "#EEDC82", 0.020, 100
    );

    // Dwarf Planets
    public static ObjectInfo eris = new ObjectInfo(
            "Eris", 1.66e22, 1163, 1163, 1163, 10125000000L, 0.44, 151.47, 44.187, 35.95, 78.6, 0.0, 25.9,
            "#8B4513", 0.040, 500
    );
    public static ObjectInfo haumea = new ObjectInfo(
            "Haumea", 4.01e21, 816, 870, 498, 6453150000L, 0.1912, 240.11, 28.19, 121.91, 63.4, 0.0, 3.9,
            "#FF69B4", 0.035, 600
    );
    public static ObjectInfo makemake = new ObjectInfo(
            "Makemake", 3.1e21, 715, 715, 715, 6850000000L, 0.159, 298.52, 29.0, 79.35, 119.2, 0.0, 22.5,
            "#B22222", 0.030, 550
    );
    public static ObjectInfo ceres = new ObjectInfo(
            "Ceres", 9.39e20, 473, 473, 473, 413700000, 0.075, 73.597, 10.593, 80.39, 5.8, 0.0, 9.07,
            "#FF7F50", 0.045, 450
    );

    // Asteroids
    public static ObjectInfo pallas = new ObjectInfo(
            "Pallas", 2.04e20, 273, 275, 263, 414500000, 0.231, 310.07, 34.837, 173.085, 83.6, 3.0, 7.813,
            "#4169E1", 0.040, 400
    );
    public static ObjectInfo vesta = new ObjectInfo(
            "Vesta", 2.59e20, 262.7, 285, 229, 353400000, 0.089, 151.198, 7.140, 103.851, 42.3, 4.0, 5.342,
            "#FFD700", 0.038, 420
    );
    public static ObjectInfo hygiea = new ObjectInfo(
            "Hygiea", 8.67e19, 216, 222, 207, 470300000, 0.117, 312.5, 3.841, 283.2, 147.2, 3.0, 13.825,
            "#A9A9A9", 0.036, 380
    );

    // Planets and the Sun
    public static ObjectInfo mercury = new ObjectInfo(
            "Mercury", 3.301e23, 2439.7, 2439.7, 2439.7, 57909227, 0.2056, 29.124, 7.004, 48.331, 174.8, 0.034, 58.646,
            "#8B8B83", 0.025, 100
    );
    public static ObjectInfo venus = new ObjectInfo(
            "Venus", 4.867e24, 6051.8, 6051.8, 6051.8, 108209475, 0.0067, 54.852, 3.394, 76.68, 131.1, 177.36, -243.025,
            "#EED2B3", 0.295, 150
    );
    public static ObjectInfo earth = new ObjectInfo(
            "Earth", 5.972e24, 6371.0, 6378.1, 6356.8, 149598262, 0.0167, 114.207, 0.00005, -11.26064, 120.2, 23.44, 0.997,
            "#4682B4", 0.299, 12, moon
    );
    public static ObjectInfo mars = new ObjectInfo(
            "Mars", 6.417e23, 3389.5, 3396.2, 3376.2, 227943824, 0.0934, 286.502, 1.850, 49.558, 250.1, 25.19, 1.026,
            "#A52A2A", 0.140, 92, phobos, deimos
    );
    public static ObjectInfo jupiter = new ObjectInfo(
            "Jupiter", JUPITER_MASS, 69911, 71492, 66854, 778340821, 0.0489, 273.867, 1.305, 100.464, 34.5, 3.13, 0.4135,
            "#F4A460", 0.589, 10500, io, europa, ganymede, callisto
    );
    public static ObjectInfo saturn = new ObjectInfo(
            "Saturn", 5.685e26, 58232, 60268, 54364, 1426666422, 0.0565, 336.013, 2.484, 113.665, 205.4, 26.73, 0.444,
            "#DAA520", 0.341, 18000, titan, rhea, iapetus, dione, tethys, enceladus
    );
    public static ObjectInfo uranus = new ObjectInfo(
            "Uranus", 8.682e25, 25362, 25559, 24973, 2870658186L, 0.0457, 96.998857, 0.769, 74.006, 88.7, 97.77, -0.718,
            "#40E0D0", 0.104, 3000, titania, oberon, umbriel, ariel, miranda
    );
    public static ObjectInfo neptune = new ObjectInfo(
            "Neptune", 1.024e26, 24622, 24764, 24341, 4498396441L, 0.0113, 276.336, 1.769, 131.784, 342.1, 28.32, 0.671,
            "#4169E1", 0.122, 4000, triton
    );
    public static ObjectInfo pluto = new ObjectInfo(
            "Pluto", 1.309e22, 1188.3, 1188.3, 1188.3, 5906376272L, 0.2488, 113.834, 17.16, 110.299, 54.3, 122.53, 6.387,
            "#708090", 0.058, 150, charon
    );

    static {
        mercury.setRelativeToParentEquator(false);
        venus.setRelativeToParentEquator(false);
        earth.setRelativeToParentEquator(false);
        moon.setRelativeToParentEquator(false);

        mars.setRelativeToParentEquator(false);
        jupiter.setRelativeToParentEquator(false);
        saturn.setRelativeToParentEquator(false);
        uranus.setRelativeToParentEquator(false);
        neptune.setRelativeToParentEquator(false);
        triton.setRelativeToParentEquator(false);

        pluto.setRelativeToParentEquator(false);
        eris.setRelativeToParentEquator(false);
        haumea.setRelativeToParentEquator(false);
        makemake.setRelativeToParentEquator(false);
        ceres.setRelativeToParentEquator(false);

        pallas.setRelativeToParentEquator(false);
        vesta.setRelativeToParentEquator(false);
        hygiea.setRelativeToParentEquator(false);
    }

    // Sun with all its planets and dwarf planets
    public static ObjectInfo sun = new ObjectInfo(
            "Sun", SOLAR_MASS, 696340, 696340, 696340, 0, 0, 0, 0, 0, 7.25, 0.0, 25.38,
            "#FFD700", 0.03, 20000,
            mercury, venus, earth, mars, jupiter, saturn, uranus, neptune,
            pluto, eris, haumea, makemake,
            ceres, pallas, vesta, hygiea
    );

    public static List<ObjectInfo> PRESET_OBJECTS = List.of(
            sun, mercury, venus, earth, mars, jupiter, saturn, uranus, neptune, pluto,
            moon
    );

    public static double solarSystem(Simulator simulator) {
        makeSystem(simulator, sun, 1, 1e3, 1e3);

        return 0.005 * 1e-7;
    }

    public static double smallSolarSystem(Simulator simulator) {
        makeSystem(simulator, sun, 1, 1e3, 1e2);

        return 0.005 * 1e-6;
    }

    public static double solarSystemWithComets(Simulator simulator) {
        makeSystem(simulator, sun, 1, 1e3, 1e3);

        CelestialObject sun = simulator.findByName("Sun");

        double mean = neptune.semiMajorAxis * 1.5 * 1e3;
        double std = mean * 0.25;

        addComets(simulator, sun, 80, mean, std);

        return 0.005 * 1e-7;
    }

    public static void jupyterSystem() {

    }

    private static void addComets(Simulator simulator,
                                  CelestialObject sun,
                                  int nObjects,
                                  double dtMean,
                                  double dtSd) {

        Random random = new Random();

        for (int i = 0; i < nObjects; i++) {
            double dt = random.nextGaussian(dtMean, dtSd);
            // Randomly generate theta (angle from z-axis, polar angle) between 0 and π
            double theta = Math.acos(2 * random.nextDouble() - 1);  // theta is in [0, π]

            // Randomly generate phi (angle in x-y plane, azimuthal angle) between 0 and 2π
            double phi = 2 * Math.PI * random.nextDouble();  // phi is in [0, 2π]

            // Convert spherical coordinates to Cartesian coordinates
            double x = dt * Math.sin(theta) * Math.cos(phi);
            double y = dt * Math.sin(theta) * Math.sin(phi);
            double z = dt * Math.cos(theta);

            double mass = random.nextDouble(1e12, 1e20);
            double density = random.nextDouble(500, 6000);
            double radius = CelestialObject.radiusOf(mass, density);
            String color = Util.randomCelestialColorCode();

            CelestialObject co = CelestialObject.createNd(
                    "Comet" + i,
                    mass,
                    radius,
                    3,
                    new double[]{x, y, z},
                    new double[3],
                    color
            );
            simulator.addObject(co);

            double[] vel = simulator.computeVelocityOfN(sun,
                    co,
                    random.nextDouble(0.01, 0.8),
                    new double[]{0, 0, 1});
            co.setVelocity(vel);
        }
    }

    private static void addAsteroidsTo(Simulator simulator,
                                       CelestialObject sun,
                                       CelestialObject planet,
                                       int nObjects,
                                       double dtSd) {

        // sun's mass is very big, just ignore others
        double[] ae = OrbitCalculator.computeBasic(
                planet,
                sun.position,
                sun.getMass() + planet.getMass(),
                VectorOperations.subtract(planet.velocity, sun.velocity),
                simulator.getG()
        );
        double a = ae[0];

        Random random = new Random();

        for (int i = 0; i < nObjects; i++) {
            double dt = random.nextGaussian(a, dtSd);
            double radPos = random.nextDouble(2 * Math.PI);
            double x = Math.cos(radPos) * dt;
            double y = Math.sin(radPos) * dt;

            double mass = random.nextDouble(1e12, 1e20);
            double density = random.nextDouble(500, 6000);
            double radius = CelestialObject.radiusOf(mass, density);
            String color = Util.randomCelestialColorCode();

            CelestialObject co = CelestialObject.create2d(
                    "Trojan-" + planet.name + i,
                    mass,
                    radius,
                    x,
                    y,
                    color
            );
            simulator.addObject(co);
            co.setVelocity(simulator.computeVelocityOfN(sun, co, 1.0, new double[]{0, 0, 1}));
        }
    }

    public static double solarSystemWithRandomAsteroids(Simulator simulator) {
        double scale = solarSystem(simulator);

        simulator.simulate(10, false);
        simulator.updateMasters();

        CelestialObject sun = simulator.findByName("Sun");

        // jupiter around
        CelestialObject jupiter = simulator.findByName("Jupiter");
        addAsteroidsTo(simulator, sun, jupiter, 35, 5e9);

        // asteroid belt
        CelestialObject ceres = simulator.findByName("Ceres");
        addAsteroidsTo(simulator, sun, ceres, 50, 5e10);

        // earth belt
        CelestialObject earth = simulator.findByName("Earth");
        addAsteroidsTo(simulator, sun, earth, 25, 5e8);

        // kuiper belt
        CelestialObject kuiper = simulator.findByName("Makemake");
        addAsteroidsTo(simulator, sun, kuiper, 40, 5e11);

        return scale;
    }

    public static double twoSolarSystems(Simulator simulator) {
        double baseScale = solarSystem(simulator);
        simulator.shiftWholeSystem(new double[]{2e13, 0, 0});
        simulator.accelerateWholeSystem(new double[]{0.0, -1e3, 0.0});
        solarSystem(simulator);  // 2nd solar system

        return baseScale * 1e-1;
    }

    private static void makeSystem(Simulator simulator,
                                   ObjectInfo root,
                                   double massMul,
                                   double radiusMul,
                                   double distanceMul) {
        if (true) {
            addObject3d(simulator, root, null, massMul, radiusMul, distanceMul, null);
        } else {
            addObject2d(simulator, root, null, massMul, radiusMul, distanceMul);
        }
    }

    /**
     * Create and return, not add to the simulator
     */
    public static CelestialObject createObjectPreset(Simulator simulator,
                                                     ObjectInfo objectInfo,
                                                     double[] position,
                                                     double[] velocity,
                                                     double scale) {

        return createObject(simulator,
                objectInfo,
                position,
                velocity,
                randomAxisToZ(objectInfo.tilt),
                1 * scale,
                1e3 * scale);
    }

    private static void addObject3d(Simulator simulator,
                                    ObjectInfo info,
                                    CelestialObject parent,
                                    double massMul,
                                    double radiusMul,
                                    double distanceMul,
                                    Vector3f parentOrbitPlaneNormal) {
        double[] position;
        double[] velocity;
        double[] localAxis = randomAxisToZ(info.tilt);
        double[] axisD;
        Vector3f localEclipticPlaneNormal = calculateOrbitalPlaneNormal(
                (float) Math.toRadians(info.inclination),
                (float) Math.toRadians(info.argumentOfPeriapsis),
                (float) Math.toRadians(info.ascendingNode));
        double[] eclipticPlaneNormal;
        if (parent != null) {
            // todo: check this correctness for moons
            // position and velocity respect to local plane
            double[] relPos = calculatePosition(
                    info.semiMajorAxis * distanceMul,
                    Math.toRadians(info.eccentricity),
                    Math.toRadians(info.inclination),
                    Math.toRadians(info.argumentOfPeriapsis),
                    Math.toRadians(info.ascendingNode),
                    Math.toRadians(info.trueAnomaly)
            );
            double mu = simulator.getG() * (parent.getMass() + info.mass * massMul);
            double[] relVel = calculateVelocity(
                    info.semiMajorAxis * distanceMul,
                    Math.toRadians(info.eccentricity),
                    Math.toRadians(info.inclination),
                    Math.toRadians(info.argumentOfPeriapsis),
                    Math.toRadians(info.ascendingNode),
                    Math.toRadians(info.trueAnomaly),
                    mu
            );

            double[] parentEclipticNormal = GuiUtils.toDoubleArray(parentOrbitPlaneNormal);

//            double[] refPlaneNormal;
//            Vector3f axisPlaneNormal;
            if (info.relativeToParentEquator) {
//                refPlaneNormal = parent.rotationAxis;

                relPos = rotateToParentEclipticPlane(relPos, parent.rotationAxis);
                position = rotateToXYPlane(relPos, parentEclipticNormal);
                relVel = rotateToParentEclipticPlane(relVel, parent.rotationAxis);
                velocity = rotateToXYPlane(relVel, parentEclipticNormal);

                eclipticPlaneNormal = rotateToParentEclipticPlane(GuiUtils.toDoubleArray(localEclipticPlaneNormal), parent.rotationAxis);
                eclipticPlaneNormal = rotateToXYPlane(eclipticPlaneNormal, parentEclipticNormal);

                axisD = rotateToParentEclipticPlane(localAxis, parent.rotationAxis);
                axisD = rotateToXYPlane(axisD, parentEclipticNormal);

//                System.out.println(info.name);
//                System.out.println(Arrays.toString(position));
//                System.out.println(Arrays.toString(velocity));

//                axisPlaneNormal = GuiUtils.fromDoubleArray(refPlaneNormal);
                if (info.name.equals("Charon")) {
                    System.out.println("Charon planes:");
                    System.out.println(Arrays.toString(axisD));
                    System.out.println(Arrays.toString(eclipticPlaneNormal));
                }
            } else {
//                axisPlaneNormal = parentOrbitPlaneNormal;

                position = rotateToXYPlane(relPos, parentEclipticNormal);
                velocity = rotateToXYPlane(relVel, parentEclipticNormal);
                eclipticPlaneNormal = rotateToXYPlane(GuiUtils.toDoubleArray(localEclipticPlaneNormal), parentEclipticNormal);

                double[] revEPN = VectorOperations.scale(eclipticPlaneNormal, 1);
                axisD = rotateToParentEclipticPlane(localAxis, revEPN);
                axisD = rotateToXYPlane(axisD, parentEclipticNormal);
            }

            position = VectorOperations.add(position, parent.position);

            velocity = VectorOperations.add(velocity, parent.velocity);

//            axis = calculateRotationAxis(
//                    (float) Math.toRadians(info.eccentricity),
//                    (float) Math.toRadians(info.inclination),
//                    (float) Math.toRadians(info.argumentOfPeriapsis),
//                    (float) Math.toRadians(info.tilt),
//                    GuiUtils.fromDoubleArray(eclipticPlaneNormal)
//            );
        } else {
            position = new double[3];
            velocity = new double[3];
            eclipticPlaneNormal = new double[]{0, 0, 1};
            axisD = localAxis;
        }

        if (info.name.equals("Earth")) {
            System.out.println("This is earth");
            System.out.println(info.inclination + " " + info.ascendingNode);
            System.out.println(Arrays.toString(position) + Arrays.toString(velocity));
        }

        CelestialObject co = createObject(simulator,
                info,
                position,
                velocity,
                axisD,
//                new double[]{axis.x, axis.y, axis.z},
                massMul,
                radiusMul);

        simulator.addObject(co);

        for (ObjectInfo moon : info.children) {
            addObject3d(simulator,
                    moon,
                    co,
                    massMul,
                    radiusMul,
                    distanceMul,
                    GuiUtils.fromDoubleArray(eclipticPlaneNormal)
            );
        }
    }

    private static void addObject2d(Simulator simulator,
                                    ObjectInfo info,
                                    CelestialObject parent,
                                    double massMul,
                                    double radiusMul,
                                    double distanceMul) {
        double x, y, ecc;
        if (parent == null) {
            x = 0.0;
            y = 0.0;
            ecc = 0.0;
        } else {
            ecc = info.eccentricity;
            double semiMajor = info.semiMajorAxis;
            double aphelion = semiMajor * (1 + ecc);
//            System.out.println(names[i] + " " + semiMajor);

            double radPos = Math.random() * 2 * Math.PI;
            x = Math.cos(radPos) * aphelion + parent.getX();
            y = Math.sin(radPos) * aphelion + parent.getY();
        }

        double[] pos = new double[simulator.getDimension()];
        pos[0] = x;
        pos[1] = y;

        double[] initVel;
        if (parent != null) {
            double sf = 1.0 - ecc;
            if (info.inclination > 90 && info.inclination < 270) sf = -sf;
            initVel = simulator.computeVelocityOfN(parent.position,
                    parent.mass,
                    parent.velocity,
                    pos,
                    info.mass * massMul,
                    sf,
                    null);
        } else {
            initVel = new double[simulator.getDimension()];
        }

        CelestialObject co = createObject(simulator,
                info,
                new double[]{x, y},
                initVel,
                randomAxisToZ(info.tilt),
                massMul,
                radiusMul);
        simulator.addObject(co);

        for (ObjectInfo moon : info.children) {
            addObject2d(simulator,
                    moon,
                    co,
                    massMul,
                    radiusMul,
                    distanceMul
            );
        }
    }

    private static CelestialObject createObject(Simulator simulator,
                                                ObjectInfo info,
                                                double[] position,
                                                double[] initVel,
                                                double[] axis,
                                                double massMul,
                                                double radiusMul) {
//        double[] axis = randomAxisToZ(info.tilt);

        String textureFile = TEXTURES.get(info.name);
        Texture diffuseMap = null;
        if (textureFile != null) {
            try {
                diffuseMap = JmeApp.getInstance().getAssetManager().loadTexture(textureFile);
            } catch (IllegalArgumentException iae) {
                System.err.println("Cannot read " + textureFile);
                iae.printStackTrace();
            }
//            if (diffuseMap.isError()) {
//                System.out.println(diffuseMap.getUrl());
//            }
        }

        double rp = info.rotationPeriod * 24 * 60 * 60;
        double av = CelestialObject.angularVelocityOf(rp);

        CelestialObject co = CelestialObject.createReal(
                info.name,
                info.mass * massMul,
                info.equatorialRadius * radiusMul,
                info.polarRadius * radiusMul,
                simulator.getDimension(),
                position,
                initVel,
                axis,
                av,
                info.colorCode,
                diffuseMap,
                273.15
        );
        co.setTidalConstants(info.loveNumber, info.dissipationFunction);
        return co;
    }

    private static double[] randomAxisToZ(double tiltAngle) {
        double randomAngle = Math.random() * 360; // Angle in degrees

        // Calculate the components of the random axis in the XY-plane
        double x = Math.cos(Math.toRadians(randomAngle));
        double y = Math.sin(Math.toRadians(randomAngle));

        // Calculate the direction vector that is 'tiltAngle' degrees from the Z-axis
        double z = Math.cos(Math.toRadians(tiltAngle));

        // Adjust the x and y components to reflect the tilt
        double scaleXY = Math.sin(Math.toRadians(tiltAngle));
        x *= scaleXY;
        y *= scaleXY;

        return new double[]{x, y, z};
    }

    private static CelestialObject addObjectTo(Simulator simulator,
                                               ObjectInfo system,
                                               CelestialObject parent) {
        double ecc = system.eccentricity;
        double semiMajor = system.semiMajorAxis;
        double aphelion = semiMajor * (1 + ecc);
//            System.out.println(names[i] + " " + semiMajor);

        double radPos = Math.random() * 2 * Math.PI;
        double x = Math.cos(radPos) * aphelion + parent.getX();
        double y = Math.sin(radPos) * aphelion + parent.getY();

        CelestialObject planet = CelestialObject.create2d(
                system.name,
                system.mass,
                system.radius,
                x,
                y,
                system.colorCode
        );
        simulator.addObject(planet);
        double[] initVel = simulator.computeVelocityOfN(parent, planet, 1.0 - ecc, new double[]{0, 0, 1});
        planet.setVelocity(initVel);

        return planet;
    }

    public static double random3StarSystem(Simulator simulator) {
        double baseShift = 3e12;
        double baseSpeed = 1e3;
        Random random = new Random();
        double scale1 = randomStarSystem(simulator,
                "Alpha",
                random.nextInt(5, 10),
                random.nextDouble(0.5, 2.0),
                random.nextDouble(4.0, 6.0),
                new double[]{baseShift * 2, 0.0, 0.0},
                new double[]{0, 0, 0});

        double scale2 = randomStarSystem(simulator,
                "Beta",
                random.nextInt(6, 11),
                random.nextDouble(0.5, 2.0),
                random.nextDouble(2, 4),
                new double[]{baseShift, -baseShift * Math.sqrt(3), 0.0},
                new double[]{baseSpeed, 0, 0});

        double scale3 = randomStarSystem(simulator,
                "Theta",
                random.nextInt(8, 12),
                random.nextDouble(0.5, 2.0),
                random.nextDouble(1, 2),
                new double[]{0, 0, 0},
                new double[]{0, -baseSpeed, 0});

        return scale1 * 0.2;
    }

    public static double randomStarSystem(Simulator simulator,
                                          String starName,
                                          int n,
                                          double scaleOfSolarSystem,
                                          double denseFactor) {
        return randomStarSystem(simulator,
                starName,
                n,
                scaleOfSolarSystem,
                denseFactor,
                new double[simulator.getDimension()],
                new double[simulator.getDimension()]);
    }

    private static double randomStarSystem(Simulator simulator,
                                           String starName,
                                           int n,
                                           double scaleOfSolarSystem,
                                           double denseFactor,
                                           double[] position,
                                           double[] velocity) {

        double massScale = Math.pow(scaleOfSolarSystem, 3);
        double dtScale = Math.pow(scaleOfSolarSystem, 2) / denseFactor;
        double stdDt = dtScale * AU;

        Random random = new Random();

        double starMass = random.nextDouble(1e30, 3e30) * massScale;
        double starDensity = starDensity(starMass);
        double starRadius = CelestialObject.radiusOf(starMass, starDensity);
        CelestialObject star = CelestialObject.createNd(
                starName,
                starMass,
                starRadius,
                simulator.getDimension(),
                position,
                velocity,
                "#ffff00"
        );
        simulator.addObject(star);

        for (int i = 0; i < n; i++) {
            double dtAu = refinedTitiusBode(i);
            double deviation = random.nextGaussian(1.0, 0.1);
            double dt = dtAu * deviation * stdDt;
            double ecc = random.nextDouble(0, 0.15);

            double semiMajor = dt / (1 - Math.pow(ecc, 2) / 2);
            double aphelion = semiMajor * (1 + ecc);

            double radPos = random.nextDouble(2 * Math.PI);
            double x = Math.cos(radPos) * aphelion;
            double y = Math.sin(radPos) * aphelion;
            double[] pos = Arrays.copyOf(position, position.length);
            pos[0] += x;
            pos[1] += y;

            ColorRGBA color = new ColorRGBA(random.nextFloat(0.25f, 1.0f),
                    random.nextFloat(0.25f, 1.0f),
                    random.nextFloat(0.25f, 1.0f), 1);

            double mass = random.nextDouble(2e23, 2e27) * massScale;
            double density = random.nextDouble(500, 6000);
            double radius = CelestialObject.radiusOf(mass, density);

            String name = starName + " " + (char) (i + 65);

            double[] vel = simulator.computeVelocityOfN(
                    star.position,
                    star.mass,
                    star.velocity,
                    pos,
                    mass,
                    1.0 - ecc,
                    new double[]{0, 0, 1}
            );

            CelestialObject planet = CelestialObject.createNd(
                    name,
                    mass,
                    radius,
                    simulator.getDimension(),
                    pos,
                    vel,
                    GuiUtils.colorToHex(color)
            );
            simulator.addObject(planet);
        }

        return 20 / stdDt;
    }

    public static double ellipseCluster(Simulator simulator, int n) {
        double a = 1e10;
        double b = 1e10;
        double c = 1e10;

        double speed = 1e3;

        double centroidMass = 1e26;
        CelestialObject centroid = CelestialObject.create3d(
                "Centroid",
                centroidMass,
                CelestialObject.radiusOf(centroidMass, 1e5),
                new double[3],
                new double[3],
                "#ffff00"
        );
        centroid.forcedSetRotation(new double[]{0, 0, 1}, 1e-3);
        simulator.addObject(centroid);

        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            double mass = rand.nextDouble(1e22, 1e25);
            double density = rand.nextDouble(500, 6000);
            double radius = CelestialObject.radiusOf(mass, density);

            double x, y, z;

            // Generate random point within a unit sphere using the rejection method
            do {
                x = 2 * rand.nextDouble() - 1;
                y = 2 * rand.nextDouble() - 1;
                z = 2 * rand.nextDouble() - 1;
            } while (x * x + y * y + z * z > 1);

            // Scale the point to the ellipsoid
            String cc = Util.randomCelestialColorCode();
            x = x * a;
            y = y * b;
            z = z * c;
            CelestialObject co = CelestialObject.create3d(
                    "Star" + i,
                    mass,
                    radius,
                    new double[]{x, y, z},
                    new double[]{
                            rand.nextDouble(-speed, speed),
                            rand.nextDouble(-speed, speed),
                            rand.nextDouble(-speed, speed)
                    },
                    cc
            );
            double[] axis = new double[]{rand.nextDouble(), rand.nextDouble(), rand.nextDouble()};
            axis = VectorOperations.normalize(axis);
            double angVel = rand.nextDouble(1e-8, 1e-1);
            co.forcedSetRotation(axis, angVel);
            simulator.addObject(co);
        }

        return 10 / c;
    }
    
    public static double twoRandomStarSystems(Simulator simulator) {
        double scale = randomStarSystem(simulator, 90, 0.75);
        simulator.rotateWholeSystem(new double[]{0, 0.33, 0.33});
        simulator.accelerateWholeSystem(new double[]{-1e4, 5e3, -5e3});
        simulator.shiftWholeSystem(new double[]{1e11, 0, 1e10});
        
        randomStarSystem(simulator, 90, 1);
        
        return scale;
    }

    public static double randomStarSystem(Simulator simulator, int n, double scale) {
        double a = 2e10 * scale;
        double b = 2e10 * scale;
        double c = 1e9 * scale;

        double flatRatio = c / (a + b) * 2;

        double centroidMass = 5e29 * scale;
        double starDensity = starDensity(centroidMass);
        double starRadius = CelestialObject.radiusOf(centroidMass, starDensity);

        String colorCode = GuiUtils.temperatureToRGBString(
                CelestialObject.approxColorTemperatureOfStar(
                        CelestialObject.approxLuminosityOfStar(centroidMass),
                        starRadius
                )
        );

        CelestialObject centroid = CelestialObject.create3d(
                "Star",
                centroidMass,
                starRadius,
                new double[3],
                new double[3],
                colorCode
        );

        centroid.forcedSetRotation(new double[]{0, 0, 1}, 1e-4);
        simulator.addObject(centroid);

        ObjectInfo[] presets = new ObjectInfo[]{
                mercury, venus, earth, mars, jupiter, saturn, uranus, neptune
        };

        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            double x, y;

            // Generate random point within a unit sphere using the rejection method
            do {
                x = 2 * rand.nextDouble() - 1;
                y = 2 * rand.nextDouble() - 1;
            } while (x * x + y * y > 1);

            // Scale the point to the ellipsoid
            String cc = Util.randomCelestialColorCode();
            x = x * a;
            y = y * b;
            double dtToCenter = Math.sqrt(x * x + y * y);
            double z = rand.nextDouble(-dtToCenter, dtToCenter) * flatRatio;

            CelestialObject co;
            if (i < presets.length) {
                co = createObject(
                        simulator,
                        presets[i],
                        new double[]{x, y, z},
                        new double[3],
                        new double[]{0, 0, 1},
                        1,
                        1e3
                );
            } else {
                double mass = rand.nextDouble(1e21, 1e24);
                double density = rand.nextDouble(500, 6000);
                double radius = CelestialObject.radiusOf(mass, density);

                co = CelestialObject.create3d(
                        "Planet" + i,
                        mass,
                        radius,
                        new double[]{x, y, z},
                        new double[3],
                        cc
                );
            }
            double[] velocity = simulator.computeVelocityOfN(centroid,
                    co,
                    rand.nextDouble(0.75, 0.99),
                    new double[]{0, 0, 1});
            co.setVelocity(velocity);

//            double[] axis = new double[]{rand.nextDouble(0.0, 0.25), 
//                    rand.nextDouble(0.0, 0.5), 
//                    rand.nextDouble(0.5, 1.0)};
//            axis = VectorOperations.normalize(axis);
            double[] axis = new double[]{0, 0, 1};
            double angVel = rand.nextDouble(1e-8, 1e-3);
//            if (rand.nextDouble() < 0.1) {
//                axis = VectorOperations.reverseVector(axis);
//            }
            co.forcedSetRotation(axis, angVel);
            simulator.addObject(co);
        }

        return 30 / (a + b + c);
    }

    public static double testCollision(Simulator simulator) {

        makeSystem(simulator, jupiter, 1, 1e3, 1e3);

        return 1e-7;
    }

    public static double saturnRingTest(Simulator simulator, int n) {
        CelestialObject saturnObj = createObjectPreset(
                simulator,
                saturn,
                new double[3],
                new double[3],
                1.0
        );
        saturnObj.forcedSetRotation(new double[]{0, 0, 1}, CelestialObject.angularVelocityOf(saturn.rotationPeriod));

        simulator.addObject(saturnObj);

        addRingTo("B", simulator, saturnObj, 92000, 117580, 10, 90, 6e18);  // B ring
        addRingTo("A", simulator, saturnObj, 122170, 136775, 20, 90, 1.1e19);  // A ring

        return 1e-7;
    }

    private static void addRingTo(String baseName, Simulator simulator, CelestialObject parent,
                                  double near, double far, double thickness, int n,
                                  double approxTotalMass) {
        Random random = new Random();
        double meanMass = approxTotalMass / n;
        for (int i = 0; i < n; i++) {
            // Generate a uniformly distributed radius (use sqrt to ensure uniform density)
            double radius = Math.sqrt(random.nextDouble() * (far * far - near * near) + near * near);
            radius *= 1e3;  // km to m

            // Generate a random angle from 0 to 2 * pi
            double theta = random.nextDouble() * (2 * Math.PI);

            // Convert polar coordinates (r, theta) to Cartesian coordinates (x, y)
            double x = radius * Math.cos(theta);
            double y = radius * Math.sin(theta);
            double z = random.nextDouble(-thickness / 2, thickness / 2);

            double mass = random.nextDouble(meanMass * 0.5, meanMass * 1.5);

            double[] pos = new double[]{x, y, z};
//            pos = SystemPresets.rotateToParentEclipticPlane(pos, parent.getRotationAxis());

            CelestialObject small = CelestialObject.create3d(
                    baseName + " Ring-" + i,
                    mass,
//                    1e4,
                    CelestialObject.radiusOf(mass, random.nextDouble(500, 1000)),
                    pos,
                    new double[]{0, 0, 0},
                    "#ffffff"
            );

            small.forcedSetRotation(parent.getRotationAxis().clone(), 1e-8);

            double[] velocity = simulator.computeVelocityOfN(parent,
                    small,
                    1.0,
//                    parent.getRotationAxis().clone()
                    new double[]{0, 0, 1}
            );

            small.setVelocity(velocity);
            simulator.addObject(small);
        }
    }

    public static double starDensity(double mass) {
        double relMass = mass / SOLAR_MASS;
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
        return Math.pow(relMass, 1 - alpha * 3) * SOLAR_DENSITY;
    }

    private static double titiusBode(int x) {
        int n;
        if (x == 0) n = 0;
        else n = (int) Math.pow(2, x - 1);

        return 0.4 + 0.3 * n;
    }

    private static double refinedTitiusBode(int x) {
        int n;
        if (x == 0) n = 0;
        else if (x < 8) n = (int) Math.pow(2, x - 1);
        else n = (int) (Math.pow(2, 6) + (x - 7) * 32);

        return 0.4 + 0.3 * n;
    }

    // Calculate 3D position from orbital elements relative to Saturn's equatorial plane
    public static double[] calculatePosition(double a,
                                             double e,
                                             double i,
                                             double omega,
                                             double omegaBig,
                                             double nu) {
        // Step 1: Calculate the distance from the focus to the object (r)
        double r = (a * (1 - e * e)) / (1 + e * Math.cos(nu));

        // Step 2: Calculate the position in the orbital plane (x', y', z') relative to Saturn's equator
        double xPrime = r * Math.cos(nu);
        double yPrime = r * Math.sin(nu);
        double zPrime = 0; // This is the orbital plane, z' is zero

        // Step 3: Apply the standard rotations for orbital elements
        double[] position = {xPrime, yPrime, zPrime};

        // Rotate by -ω (argument of periapsis) around Z-axis (Saturn's equatorial)
        position = rotateAroundZAxis(position, omega);

        // Rotate by -i (inclination, with respect to Saturn's equatorial plane) around X-axis
        position = rotateAroundXAxis(position, i);

        // Rotate by -Ω (longitude of ascending node) around Z-axis
        position = rotateAroundZAxis(position, omegaBig);

        return position; // This is the position relative to Saturn's equatorial plane
    }

    // Calculate 3D velocity from orbital elements relative to Saturn's equatorial plane
    public static double[] calculateVelocity(double a,
                                             double e,
                                             double i,
                                             double omega,
                                             double omegaBig,
                                             double nu,
                                             double mu) {
        // Step 1: Calculate the semi-latus rectum
        double p = a * (1 - e * e);

        // Step 2: Calculate radial and tangential velocities in the orbital plane
        double Vr = Math.sqrt(mu / p) * e * Math.sin(nu);
        double Vtheta = Math.sqrt(mu / p) * (1 + e * Math.cos(nu));

        // Step 3: Calculate velocity components in the orbital plane (Vx', Vy', Vz') relative to Saturn's equator
        double VxPrime = Vr * Math.cos(nu) - Vtheta * Math.sin(nu);
        double VyPrime = Vr * Math.sin(nu) + Vtheta * Math.cos(nu);
        double VzPrime = 0;

        // Step 4: Apply the same rotations for orbital elements
        double[] velocity = {VxPrime, VyPrime, VzPrime};

        // Rotate by -ω (argument of periapsis) around Z-axis (Saturn's equatorial)
        velocity = rotateAroundZAxis(velocity, omega);

        // Rotate by -i (inclination, with respect to Saturn's equatorial plane) around X-axis
        velocity = rotateAroundXAxis(velocity, i);

        // Rotate by -Ω (longitude of ascending node) around Z-axis
        velocity = rotateAroundZAxis(velocity, omegaBig);

        return velocity; // This is the velocity relative to Saturn's equatorial plane
    }

    // Rotate the position and velocity from Planet's equatorial plane to Planet's ecliptic plane
    public static double[] rotateToParentEclipticPlane(double[] vector, double[] parentRotationAxis) {
        // Use Saturn's rotation axis to rotate the vector from the equatorial plane to the ecliptic plane
        double[] rotationAxis = VectorOperations.crossProduct(parentRotationAxis, new double[]{0, 0, 1}); // Cross product with Z-axis
        if (VectorOperations.magnitude(rotationAxis) == 0) return vector;
        double angle = -Math.acos(VectorOperations.dotProduct(parentRotationAxis, new double[]{0, 0, 1}));
        return VectorOperations.rotateVector(vector, rotationAxis, angle);
    }

    // Rotate the position and velocity from Planet's ecliptic plane to XY-plane
    public static double[] rotateToXYPlane(double[] vector, double[] parentEclipticNormal) {
        // Use Saturn's ecliptic normal vector to rotate the vector to align with Earth's ecliptic plane
        double[] rotationAxis = VectorOperations.crossProduct(parentEclipticNormal, new double[]{0, 0, 1}); // Cross product with Z-axis
        if (VectorOperations.magnitude(rotationAxis) == 0) return vector;
        double angle = Math.acos(VectorOperations.dotProduct(parentEclipticNormal, new double[]{0, 0, 1}));
        return VectorOperations.rotateVector(vector, rotationAxis, angle);
    }

    public static double[] rotateFromXYPlaneToPlanetEclipticPlane(double[] vector, double[] parentEclipticNormal) {
        // The rotation axis should be the same as in rotateToXYPlane but with the opposite angle
        double[] rotationAxis = VectorOperations.crossProduct(parentEclipticNormal, new double[]{0, 0, 1}); // Cross product with Z-axis
        if (VectorOperations.magnitude(rotationAxis) == 0)
            return vector; // No rotation needed if already aligned

        // The angle is the same as in rotateToXYPlane but with a negative sign
        double angle = -Math.acos(VectorOperations.dotProduct(parentEclipticNormal, new double[]{0, 0, 1}));

        // Apply the rotation in the opposite direction
        return VectorOperations.rotateVector(vector, rotationAxis, angle);
    }

    public static double[] rotateFromPlanetEclipticPlaneToEquatorialPlane(double[] vector, double[] parentRotationAxis) {
        // The rotation axis should be the same as in rotateToParentEclipticPlane but with the opposite angle
        double[] rotationAxis = VectorOperations.crossProduct(parentRotationAxis, new double[]{0, 0, 1}); // Cross product with Z-axis
        if (VectorOperations.magnitude(rotationAxis) == 0)
            return vector; // No rotation needed if already aligned

        // The angle is the same as in rotateToParentEclipticPlane but with a negative sign
        double angle = Math.acos(VectorOperations.dotProduct(parentRotationAxis, new double[]{0, 0, 1}));

        // Apply the rotation in the opposite direction
        return VectorOperations.rotateVector(vector, rotationAxis, angle);
    }

    public static Vector3f calculateRotationAxis(float i,
                                                 float omega,
                                                 float omegaBig,
                                                 float tilt,
                                                 Vector3f referencePlaneNormal) {
        Vector3f axis;
        if (referencePlaneNormal == null) {
            axis = planetRotationAxis(i, omega, omegaBig, tilt);
        } else {
            axis = moonRotationAxis(i, omega, omegaBig, tilt, referencePlaneNormal);
        }

        return axis;
    }

    private static double[] localRotationAxis(double tilt) {
        double radTilt = Math.toRadians(tilt);
        double[] axis = new double[]{0, 0, 1};
        axis = VectorOperations.rotateVector(axis, new double[]{1, 0, 0}, radTilt);
        return axis;
    }

    private static Vector3f planetRotationAxis(float i,
                                               float omega,
                                               float omegaBig,
                                               float tilt) {
        // Step 1: Start with the axis in the orbital plane's coordinate system
        Vector3f axis = new Vector3f(0, 0, 1);  // Pointing along the Z-axis (perpendicular to the plane)

        // Step 2: Apply the tilt (rotate around the X-axis in the orbital plane)
        Matrix3f tiltRotation = new Matrix3f();
        tiltRotation.fromAngleAxis(tilt, Vector3f.UNIT_X);
        axis = tiltRotation.mult(axis);

        // Step 3: Rotate by -ω around Z-axis (argument of periapsis)
        Matrix3f rotOmega = new Matrix3f();
        rotOmega.fromAngleAxis(-omega, Vector3f.UNIT_Z);
        axis = rotOmega.mult(axis);

        // Step 4: Rotate by -i around X-axis (inclination)
        Matrix3f rotInclination = new Matrix3f();
        rotInclination.fromAngleAxis(-i, Vector3f.UNIT_X);
        axis = rotInclination.mult(axis);

        // Step 5: Rotate by -Ω around Z-axis (longitude of ascending node)
        Matrix3f rotOmegaBig = new Matrix3f();
        rotOmegaBig.fromAngleAxis(-omegaBig, Vector3f.UNIT_Z);
        return rotOmegaBig.mult(axis);
    }

    private static Vector3f calculateOrbitalPlaneNormal(float i, float omega, float omegaBig) {
        // Start with the unit vector (0, 0, 1) representing the normal to the orbital plane
        Vector3f normal = new Vector3f(0, 0, 1);

        // Apply the planet's orbital elements to rotate the plane's normal into the global reference frame
        Matrix3f rotOmega = new Matrix3f();
        rotOmega.fromAngleAxis(-omega, Vector3f.UNIT_Z);
        normal = rotOmega.mult(normal);

        Matrix3f rotInclination = new Matrix3f();
        rotInclination.fromAngleAxis(-i, Vector3f.UNIT_X);
        normal = rotInclination.mult(normal);

        Matrix3f rotOmegaBig = new Matrix3f();
        rotOmegaBig.fromAngleAxis(-omegaBig, Vector3f.UNIT_Z);
        normal = rotOmegaBig.mult(normal);

        return normal;
    }

    private static Vector3f moonRotationAxis(float i,
                                             float omega,
                                             float omegaBig,
                                             float tilt,
                                             Vector3f planetOrbitalPlaneNormal) {
        // Step 1: Start with the axis in the moon's orbital plane (relative to the planet's plane)
        Vector3f axis = new Vector3f(0, 0, 1);  // Pointing along the Z-axis in the moon's orbital plane

        // Step 2: Apply the tilt (rotate around the X-axis in the moon's orbital plane)
        Matrix3f tiltRotation = new Matrix3f();
        tiltRotation.fromAngleAxis(tilt, Vector3f.UNIT_X);
        axis = tiltRotation.mult(axis);

        // Step 3: Rotate by -ω around Z-axis (argument of periapsis for moon)
        Matrix3f rotOmega = new Matrix3f();
        rotOmega.fromAngleAxis(-omega, Vector3f.UNIT_Z);
        axis = rotOmega.mult(axis);

        // Step 4: Rotate by -i around X-axis (inclination for moon)
        Matrix3f rotInclination = new Matrix3f();
        rotInclination.fromAngleAxis(-i, Vector3f.UNIT_X);
        axis = rotInclination.mult(axis);

        // Step 5: Rotate by -Ω around Z-axis (longitude of ascending node for moon)
        Matrix3f rotOmegaBig = new Matrix3f();
        rotOmegaBig.fromAngleAxis(-omegaBig, Vector3f.UNIT_Z);
        axis = rotOmegaBig.mult(axis);

        // Step 6: Rotate the final axis by the planet's orbital plane normal
        // Convert planetOrbitalPlaneNormal to a rotation matrix and apply it to the axis
        Matrix3f planetRotationMatrix = new Matrix3f();
        planetRotationMatrix.fromAngleNormalAxis(0, planetOrbitalPlaneNormal);
        axis = planetRotationMatrix.mult(axis);

        return axis;
    }

    private static double[] rotateAroundZAxis(double[] point, double angle) {
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        double xNew = cosAngle * point[0] - sinAngle * point[1];
        double yNew = sinAngle * point[0] + cosAngle * point[1];
        return new double[]{xNew, yNew, point[2]};
    }

    private static double[] rotateAroundXAxis(double[] point, double angle) {
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        double yNew = cosAngle * point[1] - sinAngle * point[2];
        double zNew = sinAngle * point[1] + cosAngle * point[2];
        return new double[]{point[0], yNew, zNew};
    }

    public static void main(String[] args) {
        for (int i = 0; i < 12; i++) System.out.println(refinedTitiusBode(i));
    }

    public static class ObjectInfo {
        public final String name;
        public final double mass;
        public final double radius;
        public final double equatorialRadius;
        public final double polarRadius;
        public final double semiMajorAxis;
        public final double eccentricity;
        public final double argumentOfPeriapsis;
        public final double inclination;
        public final double ascendingNode;
        public final double trueAnomaly;
        public final double tilt;
        public final double rotationPeriod; // Rotation period in Earth days
        public final String colorCode;
        public final double loveNumber;
        public final double dissipationFunction;
        public final ObjectInfo[] children;

        /**
         * Whether the orbital elements are related to parent's equatorial plane or ecliptic plane.
         * If true and parent exists, the inclination, etc., are respected to parent's equatorial plane,
         * Otherwise, respected to the absolute xy-plane (earth's ecliptic plane)
         */
        private boolean relativeToParentEquator = true;

        public ObjectInfo(String name, double mass, double radius, double equatorialRadius, double polarRadius,
                          double semiMajorAxis, double eccentricity, double argumentOfPeriapsis, double inclination,
                          double ascendingNode, double trueAnomaly, double tilt, double rotationPeriod,
                          String colorCode, double loveNumber, double dissipationFunction, ObjectInfo... children) {
            this.name = name;
            this.mass = mass;
            this.radius = radius;
            this.equatorialRadius = equatorialRadius;
            this.polarRadius = polarRadius;
            this.semiMajorAxis = semiMajorAxis;
            this.eccentricity = eccentricity;
            this.argumentOfPeriapsis = argumentOfPeriapsis;
            this.inclination = inclination;
            this.ascendingNode = ascendingNode;
            this.trueAnomaly = trueAnomaly;
            this.tilt = tilt;
            this.rotationPeriod = rotationPeriod;
            this.colorCode = colorCode;
            this.loveNumber = loveNumber;
            this.dissipationFunction = dissipationFunction;
            this.children = children;
        }

        public void setRelativeToParentEquator(boolean relativeToParentEquator) {
            this.relativeToParentEquator = relativeToParentEquator;
        }

        public boolean isRelativeToParentEquator() {
            return relativeToParentEquator;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
