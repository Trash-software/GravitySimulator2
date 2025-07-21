package com.trashsoftware.gravity2.presets;

import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.gui.Vector3d;
import com.trashsoftware.gravity2.physics.*;
import com.trashsoftware.gravity2.utils.Util;

import javax.annotation.Nullable;
import java.util.*;

import static com.trashsoftware.gravity2.presets.SystemPresets.*;

public abstract class Preset {

    public final String name;
    public final int nObjects;

    private static final Random defaultGenerator = new Random();

    protected Preset(String name, int nObjects) {
        this.name = name;
        this.nObjects = nObjects;
    }

    /**
     * Creates the presets on the simulator.
     *
     * @return the preferred scale
     */
    public abstract double instantiate(Simulator simulator);

    static double randomDiskGalaxy(Simulator simulator,
                                   int n,
                                   double stdRadius,
                                   double stdMass,
                                   double massDeviation,
                                   double zScale,
                                   double coreMassRatio,
                                   double coreRadiusRatio) {

        Random rand = new Random();

        int coreN = (int) Math.round(n * coreMassRatio);
        double bulgeMass = 0;
        double bulgeR = stdRadius * coreRadiusRatio;
        Set<CelestialObject> bulgeStars = new HashSet<>();

        for (int i = 0; i < n; i++) {
            double mass = rand.nextGaussian(stdMass, massDeviation);
            mass = Math.max(mass, massDeviation);
            double density = rand.nextDouble(500, 6000);
            double radius = CelestialObject.radiusOf(mass, density);
            String colorCode = Util.randomColorCode();

            double x, y, z;
            boolean bulge = i < coreN;
            if (bulge) {
                // bulge
                bulgeMass += mass;
                x = rand.nextDouble();
                double r = bulgeR / Math.sqrt(Math.pow(x, -2.0 / 3.0) - 1);
                double theta = Math.acos(2 * rand.nextDouble() - 1); // [0, pi]
                double phi = 2 * Math.PI * rand.nextDouble();        // [0, 2pi]

                x = r * Math.sin(theta) * Math.cos(phi);
                y = r * Math.sin(theta) * Math.sin(phi);
                z = r * Math.cos(theta);
            } else {
                // disk
                // Sample radius using inverse CDF of exponential distribution
//                double r = -stdRadius * Math.log(1 - rand.nextDouble());
                double r = -stdRadius * Math.log(rand.nextDouble())
                        - stdRadius * Math.log(rand.nextDouble()) + bulgeR;

                // Uniform angle for circular distribution
                double theta = 2 * Math.PI * rand.nextDouble();
                x = r * Math.cos(theta);
                y = r * Math.sin(theta);

                // Add small vertical thickness
                z = rand.nextGaussian() * zScale * stdRadius;
            }

            CelestialObject co = CelestialObject.create3d(
                    "Object" + i,
                    mass,
                    radius,
                    new double[]{x, y, z},
                    new double[3],
                    colorCode
            );
            simulator.addObject(co);
            if (bulge) bulgeStars.add(co);
        }
        double[] barycenter = simulator.barycenter();

        for (CelestialObject co : simulator.getObjects()) {
            double[] pos = co.getPosition();
            double dtToCenter = VectorOperations.distance(pos, barycenter);
            if (dtToCenter == 0) continue;

            double vx, vy, vz;
            if (bulgeStars.contains(co)) {
                double phi_r = -simulator.getG() * bulgeMass / Math.sqrt(dtToCenter * dtToCenter + bulgeR * bulgeR);
                double v_esc = Math.sqrt(-2 * phi_r);

                double v;

                // Rejection sampling
                while (true) {
                    double x = rand.nextDouble();
                    double y = rand.nextDouble();

                    double testV = x * v_esc;
                    double f = Math.pow(testV, 2) * Math.pow(1 - Math.pow(testV / v_esc, 2), 3.5);

                    if (y < f) {
                        v = testV;
                        break;
                    }
                }

                // Random velocity direction
                double theta = Math.acos(2 * rand.nextDouble() - 1);
                double phi = 2 * Math.PI * rand.nextDouble();

                vx = v * Math.sin(theta) * Math.cos(phi);
                vy = v * Math.sin(theta) * Math.sin(phi);
                vz = v * Math.cos(theta);
            } else {
                double massInside = 0;
                for (CelestialObject other : simulator.getObjects()) {
                    if (co != other) {
                        double otherDt = VectorOperations.distance(other.getPosition(), barycenter);
                        if (otherDt < dtToCenter) massInside += other.getMass();
                    }
                }

                // Compute circular velocity
                double vc = Math.sqrt(simulator.getG() * massInside / dtToCenter);

                // Direction: perpendicular to radius vector
                vx = -vc * co.getY() / dtToCenter;
                vy = vc * co.getX() / dtToCenter;

                // Add small perturbations
                vx = vx + 0.05 * vc * rand.nextGaussian();
                vy = vy + 0.05 * vc * rand.nextGaussian();
                vz = 0.05 * vc * rand.nextGaussian();
            }
            co.setVelocity(new double[]{vx, vy, vz});
        }
        return 30 / stdRadius;
    }

    static double randomStarSystem(Simulator simulator, int n,
                                   double sizeScale, double starMass,
                                   double planetMass, double planetMassDeviation,
                                   double zScale) {
        double a = 2e10 * sizeScale;
        double b = 2e10 * sizeScale;
        double c = 2e10 * zScale * sizeScale;

        double flatRatio = c / (a + b) * 2;

        double centroidMass = starMass;
        double starDensity = BodyType.massiveObjectDensity(centroidMass);
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

        SystemPresets.ObjectInfo[] presets = new SystemPresets.ObjectInfo[]{
                mercury, venus, earth, mars,
                jupiter, saturn, uranus, neptune
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
                co = SystemPresets.createObject(
                        simulator,
                        presets[i],
                        new double[]{x, y, z},
                        new double[3],
                        new double[]{0, 0, 1},
                        1,
                        1e3
                );
            } else {
                double mass = rand.nextDouble(planetMass / planetMassDeviation, planetMass * planetMassDeviation);
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
                    rand.nextDouble(0.75, 1.25),
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

        SystemPresets.setTemperatureToSystem(simulator);

        return 30 / (a + b + c);
    }

    private static CelestialObject addPlanetRandomPosition(Simulator simulator,
                                                           CelestialObject parent,
                                                           ObjectInfo info,
                                                           @Nullable String showName,
                                                           double scale,
                                                           double apDistance,
                                                           double ecc,
                                                           double inclinationRangeDeg) {
        double theta = defaultGenerator.nextDouble(0, Math.PI * 2);
        double x = Math.cos(theta) * apDistance;
        double y = Math.sin(theta) * apDistance;
        double inclination = Math.toRadians(defaultGenerator.nextDouble(-inclinationRangeDeg, inclinationRangeDeg));
        double z = Math.sin(inclination) * apDistance;
        double[] pos = new double[]{x, y, z};
        pos = VectorOperations.add(pos, parent.getPosition());

        CelestialObject co = SystemPresets.createObjectPreset(
                simulator,
                info,
                pos,
                new double[3],
                scale
        );
        if (showName != null) co.setShownName(showName);

        double[] vel = simulator.computeVelocityOfN(parent, co, 1 - ecc, new double[]{0, 0, 1});
        co.setVelocity(vel);
        simulator.addObject(co);

        return co;
    }

    private static double[] distanceMultipliers(double[] periods) {
        double[] dt = new double[periods.length];
        for (int i = 0; i < dt.length; i++) {
            double tSqr = periods[i];
            double a = Math.pow(tSqr, 2.0 / 3);
            dt[i] = a;
        }
        return dt;
    }

    public static Preset EARTH_MOON_SYSTEM = new Preset("EarthMoonSystem", 2) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject earth = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.earth,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(earth);

            SystemPresets.addObject3d(
                    simulator,
                    SystemPresets.moon,
                    earth,
                    1, 1e3, 1e3,
                    Vector3d.UNIT_Z,
                    true
            );

            return 1e-7;
        }
    };

    public static Preset TOY_STAR_SYSTEM = new Preset("ToyStarSystem", 20) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject star = SystemPresets.createMainSequenceStar(
                    "Star",
                    SOLAR_MASS * 0.25
            );
            simulator.addObject(star);

            ObjectInfo[] infos = {
                    mercury, venus, helloKitty, mars, pinkGasGiant, saturn, uranus, neptune
            };
            double[] distancesAu = {0.025, 0.04, 0.07, 0.12, 0.2, 0.28, 0.36, 0.5};

            Map<ObjectInfo, String> names = Map.of(
                    helloKitty, "uiukitty"
            );

            for (int i = 0; i < infos.length; i++) {
//                double ecc = random.nextDouble(0, 0.18);
                double ecc = 0;
                double ap = distancesAu[i] * AU;

                CelestialObject planet = Preset.addPlanetRandomPosition(simulator,
                        star,
                        infos[i],
                        names.get(infos[i]),
                        1,
                        ap,
                        ecc,
                        5);
            }

            setTemperatureToSystem(simulator);

            return 1e-9;
        }
    };

    public static Preset HARMONIC_KITTY_SYSTEM = new Preset("HarmonicKittySystem", 10) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject star = SystemPresets.createMainSequenceStar(
                    "Star",
                    SOLAR_MASS * 0.25
            );
            simulator.addObject(star);

            ObjectInfo[] infos = {
                    mercury, pinkGasGiant, venus, helloKitty, superPinkGasGiant
            };
            String[] names = {null, "chuifengji", null, "uiukitty", "peppapig"};

            double baseLine = 0.025;
            double[] periods = new double[infos.length];
            for (int i = 0; i < infos.length; i++) {
                periods[i] = Math.pow(2, i);
            }
            double[] distancesAu = Preset.distanceMultipliers(periods);
            for (int i = 0; i < infos.length; i++) {
                distancesAu[i] *= baseLine;
            }
            System.out.println(Arrays.toString(distancesAu));

            for (int i = 0; i < infos.length; i++) {
//                double ecc = random.nextDouble(0, 0.18);
                double ecc = 0;
                double ap = distancesAu[i] * AU;

                CelestialObject planet = Preset.addPlanetRandomPosition(simulator,
                        star,
                        infos[i],
                        names[i],
                        1,
                        ap,
                        ecc,
                        1);

                if ("uiukitty".equals(names[i])) {
                    double hill = Simulator.hillRadius(planet, star, simulator.getG());
                    CelestialObject moon = Preset.addPlanetRandomPosition(simulator,
                            planet,
                            smallHelloKitty,
                            "uiulucky",
                            1,
                            hill * 0.33,
                            0,
                            20);
                } else if ("peppapig".equals(names[i])) {
                    double hill = Simulator.hillRadius(planet, star, simulator.getG());
//                    double[] peppaPeriods = new double[]{1, 1.33333, 1.77777, 2.37037};
                    double[] peppaPeriods = new double[]{1, 2, 4, 8};
                    double[] peppaDts = Preset.distanceMultipliers(peppaPeriods);
                    double[] massScaleMul = new double[]{0.6, 0.7, 0.8, 0.9};

                    for (int j = 0; j < peppaPeriods.length; j++) {
                        CelestialObject moon = Preset.addPlanetRandomPosition(simulator,
                                planet,
                                smallHelloKitty,
                                "pig" + (j + 1),
                                massScaleMul[j],
                                hill * 0.05 * peppaDts[j],
                                0,
                                0.1);
                    }
                }
            }

            setTemperatureToSystem(simulator);

            return 1e-9;
        }
    };

    public static Preset JUPITER_LAGRANGE = new Preset("JupiterLagrange", 101) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject sun = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.sun,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(sun);

            double r = 1e11;

            CelestialObject jupiter = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.jupiter,
                    new double[]{r, 0, 0},
                    new double[3],
                    1
            );
            simulator.addObject(jupiter);
            jupiter.setVelocity(simulator.computeVelocityOfN(sun, jupiter, 1.0,
                    sun.getRotationAxis()));

            int n = 99;
            double tick = 360.0 / (n + 1);

            for (int i = 1; i < n + 1; i++) {
                double rad = Math.toRadians(i * tick);
                double x = r * Math.cos(rad);
                double y = r * Math.sin(rad);

                double mass = 1e20;
                double density = 2000;
                double radius = CelestialObject.radiusOf(mass, density);
                String color = Util.randomCelestialColorCode();

                CelestialObject co = CelestialObject.create3d(
                        "Trojan-" + i,
                        mass,
                        radius,
                        new double[]{x, y, 0},
                        new double[3],
                        color
                );
                simulator.addObject(co);
                co.setVelocity(simulator.computeVelocityOfN(sun, co, 1.0, new double[]{0, 0, 1}));
            }

            return 0.005 * 1e-7;
        }
    };

    public static Preset SOLAR_SYSTEM = new Preset("SolarSystem", SystemPresets.sun.getNumChildrenIncludeSelf()) {
        @Override
        public double instantiate(Simulator simulator) {
            SystemPresets.makeSystem(simulator, SystemPresets.sun, 1, 1e3, 1e3);

            SystemPresets.setTemperatureToSystem(simulator);
            return 0.005 * 1e-7;
        }
    };

    public static Preset SOLAR_SYSTEM_NO_MOONS = new Preset("SolarSystemNoMoons",
            SystemPresets.sun.getNumDirectChildrenIncludeSelf()) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject sun = SystemPresets.addObject3d(simulator, SystemPresets.sun,
                    null, 1, 1e3, 1e3,
                    null, false);
            
            for (ObjectInfo planetLike : SystemPresets.sun.children) {
                SystemPresets.addObject3d(simulator, planetLike,
                        sun, 1, 1e3, 1e3,
                        sun.getRotationAxisVector(), false);
            }

            SystemPresets.setTemperatureToSystem(simulator);
            return 0.005 * 1e-7;
        }
    };

    public static Preset SOLAR_SYSTEM_WITH_ASTEROIDS = new Preset("SolarSystemWithAsteroids",
            SystemPresets.sun.getNumChildrenIncludeSelf() + 35 + 50 + 25 + 40) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = SOLAR_SYSTEM.instantiate(simulator);

            simulator.simulate(10);
            simulator.updateMasters();

            CelestialObject sun = simulator.findByName("Sun");

            // jupiter around
            CelestialObject jupiter = simulator.findByName("Jupiter");
            SystemPresets.addAsteroidsTo(simulator, sun, jupiter, 35, 5e9);

            // asteroid belt
            CelestialObject ceres = simulator.findByName("Ceres");
            SystemPresets.addAsteroidsTo(simulator, sun, ceres, 50, 5e10);

            // earth belt
            CelestialObject earth = simulator.findByName("Earth");
            SystemPresets.addAsteroidsTo(simulator, sun, earth, 25, 5e8);

            // kuiper belt
            CelestialObject kuiper = simulator.findByName("Makemake");
            SystemPresets.addAsteroidsTo(simulator, sun, kuiper, 40, 5e11);

            return scale;
        }
    };

    public static Preset TWO_SOLAR_SYSTEMS = new Preset("TwoSolarSystems", SystemPresets.sun.getNumChildrenIncludeSelf() * 2) {
        @Override
        public double instantiate(Simulator simulator) {
            double baseScale = SOLAR_SYSTEM.instantiate(simulator);
            simulator.shiftWholeSystem(new double[]{2e13, 0, 0});
            simulator.accelerateWholeSystem(new double[]{0.0, -1e3, 0.0});
            SOLAR_SYSTEM.instantiate(simulator);  // 2nd solar system

            return baseScale * 1e-1;
        }
    };
    
    public static Preset NESTED_PLANET = new Preset("NestedPlanet", 6) {
        @Override
        public double instantiate(Simulator simulator) {
            double baseScale = 1.6;
            CelestialObject sirius = SystemPresets.createObjectPreset(
                    simulator,
                    siriusA,
                    new double[3],
                    new double[3],
                    1.0
            );
            CelestialObject pc = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.proximaCentauri,
                    new double[]{-1.2e11 * baseScale, 0, 8e10 * baseScale},
                    new double[3],
                    1.0
            );
            CelestialObject sun = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.sun,
                    new double[]{5e11 * baseScale, 0, 0},
                    new double[3],
                    1.0
            );
            
            Vector3d pcVel = simulator.computeVelocityOfN(sirius, pc, 1.0, Vector3d.UNIT_Z);
            pc.setVelocity(pcVel.mult(sirius.getMass() / (sirius.getMass() + pc.getMass())));
            sirius.setVelocity(pcVel.mult(-pc.getMass() / (sirius.getMass() + pc.getMass())));
            
            simulator.addObject(sirius);
            simulator.addObject(pc);
            simulator.simulate(1);
            HieraticalSystem siriusHs = simulator.getHieraticalSystem(sirius);
            
            Vector3d sunVel = simulator.computeVelocityOfN(siriusHs, sun, 0.8, Vector3d.UNIT_Z);
            siriusHs.accelerate(sunVel.mult(-sun.getMass() / (siriusHs.getMass() + sun.getMass())));
            sun.setVelocity(sunVel.mult(siriusHs.getMass() / (siriusHs.getMass() + sun.getMass())));
            
            simulator.addObject(sun);
            
            CelestialObject hostStar = sun;
            
            CelestialObject jup = SystemPresets.createObjectPreset(
                    simulator,
                    jupiter,
                    VectorOperations.add(hostStar.getPosition(), new double[]{1e11 * baseScale, 0, 0}),
                    new double[3],
                    1.0
            );
            jup.setVelocity(simulator.computeVelocityOfN(hostStar, jup, 1.0, hostStar.getRotationAxis()));
            simulator.addObject(jup);

            CelestialObject ear = SystemPresets.createObjectPreset(
                    simulator,
                    earth,
                    VectorOperations.add(jup.getPosition(), new double[]{2e9 * baseScale, 0, 0}),
                    new double[3],
                    1.0
            );
            ear.setVelocity(simulator.computeVelocityOfN(jup, ear, 1.0, jup.getRotationAxis()));
            simulator.addObject(ear);

            CelestialObject mon = SystemPresets.createObjectPreset(
                    simulator,
                    moon,
                    VectorOperations.add(ear.getPosition(), new double[]{-7e7 * baseScale, 0, 1e7 * baseScale}),
                    new double[3],
                    1.0
            );
            mon.setVelocity(simulator.computeVelocityOfN(ear, mon, 1.0, ear.getRotationAxis()));
            simulator.addObject(mon);
            
            return 1e-10;
        }
    };

    public static Preset SIMPLE_THREE_BODY = new Preset("SimpleThreeBody", 3) {
        @Override
        public double instantiate(Simulator simulator) {
            // Star 1
            double star1Mass = 1.989e30;               // Mass in kg (1 Solar mass)
            double star1Radius = 6.957e8;              // Radius in meters (Sun's radius)
            double[] star1Position = new double[]{     // Position in meters
                    0, // x-coordinate
                    0,         // y-coordinate
                    0          // z-coordinate
            };
            double[] star1Velocity = new double[]{     // Velocity in meters per second
                    0,          // x-component
                    0,    // y-component
                    0           // z-component
            };
            String star1Color = GuiUtils.temperatureToRGBString(
                    CelestialObject.approxColorTemperatureOfStar(
                            CelestialObject.approxLuminosityOfStar(star1Mass),
                            star1Radius
                    )
            );
            CelestialObject star1 = CelestialObject.create3d(
                    "Star1",
                    star1Mass,
                    star1Radius,
                    star1Position,
                    star1Velocity,
                    star1Color
            );
            simulator.addObject(star1);

            // Star 2
            double star2Mass = 9.945e29;               // Mass in kg (0.5 Solar masses)
            double star2Radius = 3.4785e8;             // Radius in meters (approx. 0.5 Sun's radius)
            double[] star2Position = new double[]{     // Position in meters
                    1e11,  // x-coordinate
                    1e2,         // y-coordinate
                    1e2          // z-coordinate
            };
            double[] star2Velocity = new double[]{     // Velocity in meters per second
                    1e2,           // x-component
                    3e4,           // y-component
                    1e2            // z-component
            };
            String star2Color = GuiUtils.temperatureToRGBString(
                    CelestialObject.approxColorTemperatureOfStar(
                            CelestialObject.approxLuminosityOfStar(star2Mass),
                            star2Radius
                    )
            );
            CelestialObject star2 = CelestialObject.create3d(
                    "Star2",
                    star2Mass,
                    star2Radius,
                    star2Position,
                    star2Velocity,
                    star2Color
            );
            simulator.addObject(star2);

            // Planet
            double[] planetPosition = new double[]{    // Position in meters
                    1e2,            // x-coordinate
                    -5e10,          // y-coordinate (5 AU from the center of mass)
                    1e2             // z-coordinate
            };
            double[] planetVelocity = new double[]{    // Velocity in meters per second
                    5e4,    // x-component
                    1e2,          // y-component
                    1e2           // z-component
            };
            CelestialObject earthObj = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.earth,
                    planetPosition,
                    planetVelocity,
                    1
            );
            simulator.addObject(earthObj);

            // Moon
            double moonOrbitalRadius = 1e8;        // Orbital radius in meters (distance from Earth to Moon)

            // Calculate the moon's orbital velocity around the planet
            double moonOrbitalVelocity = Math.sqrt(simulator.getG() * earthObj.getMass() / moonOrbitalRadius); // In m/s

            // Moon's position relative to the planet
            double[] moonPositionRelative = new double[]{
                    moonOrbitalRadius,  // x-coordinate
                    1e2,                  // y-coordinate
                    1e2                   // z-coordinate
            };

            // Moon's absolute position
            double[] moonPosition = new double[]{
                    planetPosition[0] + moonPositionRelative[0],
                    planetPosition[1] + moonPositionRelative[1],
                    planetPosition[2] + moonPositionRelative[2]
            };

            // Moon's velocity relative to the planet
            double[] moonVelocityRelative = new double[]{
                    1e1,                       // x-component
                    moonOrbitalVelocity,     // y-component
                    1e1                        // z-component
            };

            // Moon's absolute velocity
            double[] moonVelocity = new double[]{
                    planetVelocity[0] + moonVelocityRelative[0],
                    planetVelocity[1] + moonVelocityRelative[1],
                    planetVelocity[2] + moonVelocityRelative[2]
            };

            CelestialObject moonObj = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.moon,
                    moonPosition,
                    moonVelocity,
                    1
            );
            simulator.addObject(moonObj);

            SystemPresets.setTemperatureToSystem(simulator);

            return 1e-8;
        }
    };

    public static Preset RANDOM_STAR_SYSTEM = new Preset("StarSystem", 91) {
        @Override
        public double instantiate(Simulator simulator) {
            return randomStarSystem(simulator, 90, 1, 5e29, 1e27, 10, 0.05);
        }
    };

    public static Preset INFANT_STAR_SYSTEM = new Preset("InfantStarSystem", 191) {
        @Override
        public double instantiate(Simulator simulator) {
            return randomStarSystem(simulator, 100, 5, 2e30, 1e28, 30, 0.05);
        }
    };

    public static Preset TWO_RANDOM_STAR_SYSTEM = new Preset("TwoStarSystem", 182) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = randomStarSystem(simulator, 70, 0.8, 3e29, 5e25, 10, 0.01);
            simulator.rotateWholeSystem(new double[]{0, 0.5, 0.5});
            simulator.accelerateWholeSystem(new double[]{0, 0, -3e4});
            simulator.shiftWholeSystem(new double[]{1e11, 1e10, 1e10});

            randomStarSystem(simulator, 80, 1.2, 2e30, 1e26, 10, 0.01);

            return scale;
        }
    };

    public static Preset TWO_RANDOM_CHAOS_SYSTEM = new Preset("TwoChaosSystem", 182) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = randomStarSystem(simulator, 90, 0.8, 3e28, 1e26, 10, 0.05);
            simulator.rotateWholeSystem(new double[]{0, 0.5, 0.5});
            simulator.accelerateWholeSystem(new double[]{0, 0, -2e4});
            simulator.shiftWholeSystem(new double[]{1e11, 1e10, 1e10});

            randomStarSystem(simulator, 90, 1.2, 2e29, 5e27, 10, 0.05);

            return scale;
        }
    };

    public static Preset ELLIPSE_CLUSTER = new Preset("EllipseCluster", 181) {
        @Override
        public double instantiate(Simulator simulator) {
            int n = nObjects - 1;
            double a = 1e11;
            double b = 1e11;
            double c = 1e11;

//        double speed = 1e3;

            double centroidMass = 5e29;
            double starDensity = BodyType.massiveObjectDensity(centroidMass);
            double starRadius = CelestialObject.radiusOf(centroidMass, starDensity);
            String colorCode = GuiUtils.temperatureToRGBString(
                    CelestialObject.approxColorTemperatureOfStar(
                            CelestialObject.approxLuminosityOfStar(centroidMass),
                            starRadius
                    )
            );
            CelestialObject centroid = CelestialObject.create3d(
                    "Centroid",
                    centroidMass,
                    starRadius,
                    new double[3],
                    new double[3],
                    colorCode
            );
            centroid.forcedSetRotation(new double[]{0, 0, 1}, 1e-3);
            simulator.addObject(centroid);

            Random rand = new Random();
            for (int i = 0; i < n; i++) {
                double mass = rand.nextDouble(1e26, 1e28);
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
                        new double[3],
                        cc
                );
                double[] axis = new double[]{rand.nextDouble(), rand.nextDouble(), rand.nextDouble()};
                axis = VectorOperations.normalize(axis);
                double angVel = rand.nextDouble(1e-8, 1e-1);
                co.forcedSetRotation(axis, angVel);

                double[] orbitAxis = new double[]{rand.nextDouble(), rand.nextDouble(), rand.nextDouble()};
                orbitAxis = VectorOperations.normalize(orbitAxis);
                double[] velocity = simulator.computeVelocityOfN(
                        centroid,
                        co,
                        rand.nextDouble(0, 1),
                        orbitAxis
                );
                co.setVelocity(velocity);

                simulator.addObject(co);
            }

            return 10 / c;
        }
    };

    public static final Preset JUPITER_HARMONIC = new Preset("JupiterHarmonicMoons", 4) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject jupiter = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.jupiter,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(jupiter);

            CelestialObject io = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.io,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(io);
            io.setPosition(SystemPresets.rotateToParentEclipticPlane(
                    new double[]{421700_000, 0, 0},
                    jupiter.getRotationAxis()
            ));
            io.setVelocity(simulator.computeOrbitVelocity(jupiter, io, jupiter.getRotationAxis()));

            CelestialObject europa = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.europa,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(europa);
            europa.setPosition(SystemPresets.rotateToParentEclipticPlane(
                    new double[]{670900_000, 0, 0},
                    jupiter.getRotationAxis()
            ));
            europa.setVelocity(simulator.computeOrbitVelocity(jupiter, europa, jupiter.getRotationAxis()));

            CelestialObject ganymede = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.ganymede,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(ganymede);
            ganymede.setPosition(SystemPresets.rotateToParentEclipticPlane(
                    new double[]{0, 1070400_000, 0},
                    jupiter.getRotationAxis()
            ));
            ganymede.setVelocity(simulator.computeOrbitVelocity(jupiter, ganymede, jupiter.getRotationAxis()));

            return 1e-8;
        }
    };

    public static final Preset PLUTO_CHARON = new Preset("PlutoCharon", 2) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject pluto = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.pluto,
                    new double[3],
                    new double[3],
                    1
            );
            simulator.addObject(pluto);
            CelestialObject charon = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.charon,
                    new double[]{2e7, 0, 1e6},
                    new double[3],
                    1
            );
            simulator.addObject(charon);
            charon.setVelocity(simulator.computeVelocityOfN(pluto, charon, 1.0, pluto.getRotationAxis()));

            return 5e-7;
        }
    };

    public static Preset ORBIT_TEST = new Preset("OrbitTest", 4) {
        @Override
        public double instantiate(Simulator simulator) {
            CelestialObject star1 = SystemPresets.createMainSequenceStar("Star1", SOLAR_MASS);
            simulator.addObject(star1);

            CelestialObject star2 = SystemPresets.createMainSequenceStar("Star2", SOLAR_MASS * 0.6);
            star2.setPosition(new double[]{5e10, 0, 1e8});
            star2.setVelocity(simulator.computeVelocityOfN(star1, star2, 1, star1.getRotationAxis()));
            simulator.addObject(star2);

//            simulator.simulate(10, false);

            CelestialObject planet1 = Preset.addPlanetRandomPosition(simulator,
                    star1,
                    earth,
                    null,
                    1,
                    1e10,
                    0,
                    0.1);

            CelestialObject planet2 = createObjectPreset(simulator,
                    jupiter,
                    new double[]{2e11, 0, 1e9},
                    new double[3],
                    1);
            simulator.addObject(planet2);
            planet2.setVelocity(simulator.computeVelocityOfN(
                    Simulator.barycenterOf(3, star1, star2),
                    star1.getMass() + star2.getMass(),
                    new double[3],
                    planet2,
                    1,
                    new double[]{0, 0, 1}
            ));

            return 1e-10;
        }
    };

    public static Preset SINGLE_GALAXY = new Preset("SingleGalaxy", 120) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = randomDiskGalaxy(
                    simulator,
                    180,
                    3e10,
                    1e25,
                    1e24,
                    0.01,
                    0.3,
                    0.1);
            return scale;
        }
    };

    public static final Preset[] DEFAULT_PRESETS = {
            SOLAR_SYSTEM,
            SOLAR_SYSTEM_NO_MOONS,
            SOLAR_SYSTEM_WITH_ASTEROIDS, TWO_SOLAR_SYSTEMS, 
            NESTED_PLANET,
            JUPITER_LAGRANGE,
            SIMPLE_THREE_BODY, TOY_STAR_SYSTEM, HARMONIC_KITTY_SYSTEM,
            RANDOM_STAR_SYSTEM, INFANT_STAR_SYSTEM,
            TWO_RANDOM_STAR_SYSTEM,
            TWO_RANDOM_CHAOS_SYSTEM,
            SINGLE_GALAXY,
            ELLIPSE_CLUSTER,
            PLUTO_CHARON,
            JUPITER_HARMONIC,
            ORBIT_TEST
    };
}
