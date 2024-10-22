package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.fxml.units.UnitsUtil;
import com.trashsoftware.gravity2.physics.*;
import com.trashsoftware.gravity2.physics.status.Comet;
import com.trashsoftware.gravity2.physics.status.Star;
import com.trashsoftware.gravity2.presets.SystemPresets;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ObjectStatsWrapper extends HBox {

    @FXML
    Pane modelPane;
    @FXML
    Label nameLabel, typeLabel, massLabel, diameterLabel, speedLabel, densityLabel;
    @FXML
    GridPane starPane, planetPane;
    @FXML
    GridPane detailPane;

    Label parentLabel, orbitStatusLabel, distanceLabel, avgDistanceLabel, periodLabel, eccLabel,
            hillRadiusLabel, aphelionLabel, perihelionLabel,
            inclinationLabel, ascendingNodeLabel,
            semiMajorLabel, hieraticalLabel,
            rotationPeriodLabel, rotationAxisTiltLabel, transKineticLabel, rotKineticLabel,
            bindingEnergyLabel, thermalEnergyLabel, avgTempLabel,
            volumeLabel, accelerationLabel, eqRadiusLabel, polarRadiusLabel,
            rocheLimitSolidLabel, rocheLimitLiquidLabel;
    Label childrenCountLabel, circlingChildrenCountLabel, subsystemMassLabel;

    Label colorTempLabel, luminosityLabel;
    Label surfaceTempDayLabel, surfaceTempNightLabel, surfaceTempAvgLabel,
            powerReceivedLabel, powerEmittedLabel, albedoLabel;
    
    Label binaryPairPrompt, binaryPairLabel;

    @FXML
    Hyperlink showOrbitPaneBtn;

    private Runnable onFocus, onCollapse;
    private Consumer<ObjectStatsWrapper> onExpand;
    private Consumer<CelestialObject> onLand;
    CelestialObject object;
    OrbitalElements orbitalElements;

    private final ResourceBundle strings;
    boolean hasExpanded = false;
    boolean hasStarPaneExpanded = false;
    boolean hasPlanetPaneExpanded = false;

    public ObjectStatsWrapper(CelestialObject celestialObject,
                              Simulator simulator,
                              UnitsConverter defaultUnit,
                              Runnable onFocus,
                              Consumer<ObjectStatsWrapper> onExpand,
                              Consumer<CelestialObject> onLand,
                              Runnable onCollapse,
                              ResourceBundle resourceBundle) {
        super();

        this.strings = resourceBundle;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "objectStatsWrapper.fxml"),
                strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        setObject(celestialObject, simulator, defaultUnit, onFocus, onExpand, onLand, onCollapse);
    }

    @FXML
    public void focusAction() {
        onFocus.run();
    }

    @FXML
    public void landAction() {
        onLand.accept(object);
    }

    @FXML
    public void expandAction() {

    }

    private void setObject(CelestialObject celestialObject,
                           Simulator simulator,
                           UnitsConverter defaultUnit,
                           Runnable onFocus,
                           Consumer<ObjectStatsWrapper> onExpand,
                           Consumer<CelestialObject> onLand,
                           Runnable onCollapse) {
        this.object = celestialObject;
        this.onFocus = onFocus;
        this.onExpand = onExpand;
        this.onCollapse = onCollapse;
        this.onLand = onLand;

        nameLabel.setText(object.getNameShowing());
//        GraphicsContext gc = canvas.getGraphicsContext2D();
//        gc.setFill(object.getColor());
//
//        double w = canvas.getWidth();
//        double h = canvas.getHeight();
//        gc.fillOval(0, 0, w, h);
//        StarModel modelCopy = Util.cloneShape(celestialObject.getModel(), 15);
//        Rotate sideToMe = new Rotate(90, new Point3D(0, 0, 1));
//        modelCopy.getTransforms().add(sideToMe);

//        modelPane.getChildren().add(modelCopy);

        update(simulator, defaultUnit);
    }

    private void initStarPane() {
        int rowIndex = 0;

        starPane.add(new Separator(), 0, rowIndex, 4, 1);
        rowIndex++;

        starPane.add(new Label(strings.getString("colorTemp")), 0, rowIndex);
        colorTempLabel = new Label();
        starPane.add(colorTempLabel, 1, rowIndex);

        starPane.add(new Label(strings.getString("luminosity")), 2, rowIndex);
        luminosityLabel = new Label();
        starPane.add(luminosityLabel, 3, rowIndex);

        rowIndex++;
    }

    private void initPlanetPane() {
        int rowIndex = 0;

        planetPane.add(new Separator(), 0, rowIndex, 4, 1);
        rowIndex++;

//        planetPane.add(new Label(strings.getString("surfaceTempDay")), 0, rowIndex);
//        surfaceTempDayLabel = new Label();
//        planetPane.add(surfaceTempDayLabel, 1, rowIndex);
//
//        planetPane.add(new Label(strings.getString("surfaceTempNight")), 2, rowIndex);
//        surfaceTempNightLabel = new Label();
//        planetPane.add(surfaceTempNightLabel, 3, rowIndex);
        planetPane.add(new Label(strings.getString("albedo")), 0, rowIndex);
        albedoLabel = new Label();
        planetPane.add(albedoLabel, 1, rowIndex);

        rowIndex++;

        planetPane.add(new Label(strings.getString("surfaceTempAvg")), 0, rowIndex);
        surfaceTempAvgLabel = new Label();
        planetPane.add(surfaceTempAvgLabel, 1, rowIndex);

        rowIndex++;

        planetPane.add(new Label(strings.getString("powerReceived")), 0, rowIndex);
        powerReceivedLabel = new Label();
        planetPane.add(powerReceivedLabel, 1, rowIndex);

        planetPane.add(new Label(strings.getString("powerEmitted")), 2, rowIndex);
        powerEmittedLabel = new Label();
        planetPane.add(powerEmittedLabel, 3, rowIndex);
    }

    private void initDetailPane() {
        int rowIndex = 0;

        detailPane.add(new Label(strings.getString("eqRadius")), 0, rowIndex);
        eqRadiusLabel = new Label();
        detailPane.add(eqRadiusLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("polarRadius")), 2, rowIndex);
        polarRadiusLabel = new Label();
        detailPane.add(polarRadiusLabel, 3, rowIndex);

        rowIndex++;

        detailPane.add(new Label(strings.getString("volume")), 0, rowIndex);
        volumeLabel = new Label();
        detailPane.add(volumeLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("rotationPeriod")), 2, rowIndex);
        rotationPeriodLabel = new Label();
        detailPane.add(rotationPeriodLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("transKinetic")), 0, rowIndex);
        transKineticLabel = new Label();
        detailPane.add(transKineticLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("rotKinetic")), 2, rowIndex);
        rotKineticLabel = new Label();
        detailPane.add(rotKineticLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("thermalEnergy")), 0, rowIndex);
        thermalEnergyLabel = new Label();
        detailPane.add(thermalEnergyLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("bindingEnergy")), 2, rowIndex);
        bindingEnergyLabel = new Label();
        detailPane.add(bindingEnergyLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("avgTemperature")), 0, rowIndex);
        avgTempLabel = new Label();
        detailPane.add(avgTempLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("acceleration")), 2, rowIndex);
        accelerationLabel = new Label();
        detailPane.add(accelerationLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("rocheLimitSolid")), 0, rowIndex);
        rocheLimitSolidLabel = new Label();
        detailPane.add(rocheLimitSolidLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("rocheLimitLiquid")), 2, rowIndex);
        rocheLimitLiquidLabel = new Label();
        detailPane.add(rocheLimitLiquidLabel, 3, rowIndex);
        rowIndex++;

        // orbit related
        detailPane.add(new Separator(), 0, rowIndex, 4, 1);
        rowIndex++;

        detailPane.add(new Label(strings.getString("hieraticalLevel")), 0, rowIndex);
        hieraticalLabel = new Label();
        detailPane.add(hieraticalLabel, 1, rowIndex);

        binaryPairPrompt = new Label(strings.getString("binaryPair"));
        detailPane.add(binaryPairPrompt, 2, rowIndex);
        binaryPairLabel = new Label();
        detailPane.add(binaryPairLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("parent")), 0, rowIndex);
        parentLabel = new Label();
        detailPane.add(parentLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("status")), 2, rowIndex);
        orbitStatusLabel = new Label();
        detailPane.add(orbitStatusLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("numChildrenStrict")), 0, rowIndex);
        circlingChildrenCountLabel = new Label();
        detailPane.add(circlingChildrenCountLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("subsystemMass")), 2, rowIndex);
        subsystemMassLabel = new Label();
        detailPane.add(subsystemMassLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("distance")), 0, rowIndex);
        distanceLabel = new Label();
        detailPane.add(distanceLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("avgDt")), 2, rowIndex);
        avgDistanceLabel = new Label();
        detailPane.add(avgDistanceLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("aphelion")), 0, rowIndex);
        aphelionLabel = new Label();
        detailPane.add(aphelionLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("perihelion")), 2, rowIndex);
        perihelionLabel = new Label();
        detailPane.add(perihelionLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("semiMajorAxis")), 0, rowIndex);
        semiMajorLabel = new Label();
        detailPane.add(semiMajorLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("eccentricity")), 2, rowIndex);
        eccLabel = new Label();
        detailPane.add(eccLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("inclination")), 0, rowIndex);
        inclinationLabel = new Label();
        detailPane.add(inclinationLabel, 1, rowIndex);

        detailPane.add(new Label(strings.getString("ascendingNode")), 2, rowIndex);
        ascendingNodeLabel = new Label();
        detailPane.add(ascendingNodeLabel, 3, rowIndex);
        rowIndex++;

        detailPane.add(new Label(strings.getString("period")), 0, rowIndex);
        periodLabel = new Label();
        detailPane.add(periodLabel, 1, rowIndex);

        rowIndex++;

        detailPane.add(new Label(strings.getString("rotationAxisTilt")), 0, rowIndex);
        rotationAxisTiltLabel = new Label();
        detailPane.add(rotationAxisTiltLabel, 1, rowIndex);

        rowIndex++;
        detailPane.add(new Label(strings.getString("hillRadius")), 0, rowIndex);
        hillRadiusLabel = new Label();
        detailPane.add(hillRadiusLabel, 1, rowIndex);

        rowIndex++;
        Hyperlink hide = new Hyperlink(strings.getString("hideDetails"));
        hide.setOnAction(e -> hideOrbitPane());
        detailPane.add(hide, 0, rowIndex);
    }

    public void update(Simulator simulator, UnitsConverter uc) {
        typeLabel.setText(objectType());
        massLabel.setText(uc.mass(object.getMass()));
        diameterLabel.setText(uc.radius(object.getAverageRadius() * 2));
        double vol = object.getVolume();
        densityLabel.setText(uc.mass(object.getMass() / vol) + "/m³");
        speedLabel.setText(uc.speed(object.getSpeed()));

        if (detailPane.isVisible()) {
            selfDetail(simulator, uc);
            orbitRelated(simulator, uc);
        }
    }

    private void selfDetail(Simulator simulator, UnitsConverter uc) {
        eqRadiusLabel.setText(uc.radius(object.getEquatorialRadius()));
        polarRadiusLabel.setText(uc.radius(object.getPolarRadius()));

        transKineticLabel.setText(uc.energy(object.transitionalKineticEnergy()));
        rotKineticLabel.setText(uc.energy(object.rotationalKineticEnergy()));
        bindingEnergyLabel.setText(uc.energy(simulator.gravitationalBindingEnergyOf(object)));
        thermalEnergyLabel.setText(uc.energy(object.getInternalThermalEnergy()));
        avgTempLabel.setText(uc.temperature(object.getBodyAverageTemperature()));

        rocheLimitSolidLabel.setText(uc.distance(Simulator.computeRocheLimitSolid(object)));
        rocheLimitLiquidLabel.setText(uc.distance(Simulator.computeRocheLimitLiquid(object)));
        accelerationLabel.setText(uc.acceleration(object.accelerationAlongMovingDirection()));
        rotationPeriodLabel.setText(uc.time(object.getRotationPeriod()));

        double vol = object.getVolume();
        volumeLabel.setText(uc.volume(vol));
        
        if (object.getStatus() instanceof Star star) {
            if (!hasStarPaneExpanded) {
                initStarPane();
                hasStarPaneExpanded = true;
            }

            starPane.setVisible(true);
            starPane.setManaged(true);
            planetPane.setVisible(false);
            planetPane.setManaged(false);
            starRelated(simulator, star, uc);
        } else {
            if (!hasPlanetPaneExpanded) {
                initPlanetPane();
                hasPlanetPaneExpanded = true;
            }

            starPane.setVisible(false);
            starPane.setManaged(false);
            planetPane.setVisible(true);
            planetPane.setManaged(true);
            planetRelated(simulator, uc);
        }
    }

    private void starRelated(Simulator simulator, Star star, UnitsConverter uc) {
        double luminosity = star.getLuminosity();
        double colorTemp = star.getEmissionColorTemperature();

        luminosityLabel.setText(uc.luminosity(luminosity));
        colorTempLabel.setText(String.format("%.0fK", colorTemp));
    }

    private void planetRelated(Simulator simulator, UnitsConverter uc) {
        double albedo = object.estimateAlbedo();
        double received = 0.0;
        for (CelestialObject co : simulator.getObjects()) {
            double luminosity = co.getLuminosity();
            if (luminosity > 0 && co != object) {  // shouldn't be this, but just for safety
                // is a light source
                double distance = VectorOperations.distance(co.getPosition(), object.getPosition());
                received += object.calculateLightReceived(luminosity, distance);
            }
        }

        double emitted = object.getThermalEmission();

        albedoLabel.setText(uc.generalNumber(albedo * 100) + "%");
        powerReceivedLabel.setText(UnitsUtil.sciFmt.format(received) + "W");
        powerEmittedLabel.setText(UnitsUtil.sciFmt.format(emitted) + "W");
        surfaceTempAvgLabel.setText(uc.temperature(object.getSurfaceTemperature()));
    }
    
    private boolean isBinaryRelation(CelestialObject primary, AbstractObject secondary) {
        double[] barycenter = Simulator.barycenterOf(primary.getPosition().length, primary, secondary);
        double dt = VectorOperations.distance(primary.getPosition(), barycenter);
        // whether is outside primary body
        return dt > primary.getAverageRadius();
    }

    private CelestialObject getBinaryStar(HieraticalSystem selfSystem, CelestialObject hillMaster) {
        if (hillMaster != null && hillMaster.isEmittingLight()) {
            // a planet cannot be binary star with its planet
            if (isBinaryRelation(hillMaster, selfSystem)) {
                return hillMaster;
            }
        }

        List<HieraticalSystem> sortedChildren = selfSystem.getChildrenSorted();
        if (!sortedChildren.isEmpty()) {
            HieraticalSystem firstChild = sortedChildren.get(0);
            if (firstChild.master.isEmittingLight() && isBinaryRelation(object, firstChild)) {
                return firstChild.master;
            }
        }

        return null;
    }
    
    private CelestialObject getBinaryPlanet(HieraticalSystem selfSystem, CelestialObject hillMaster) {
        if (hillMaster != null && !hillMaster.isEmittingLight()) {
            // a planet cannot be binary star with its planet
            if (isBinaryRelation(hillMaster, selfSystem)) {
                return hillMaster;
            }
        }

        List<HieraticalSystem> sortedChildren = selfSystem.getChildrenSorted();
        if (!sortedChildren.isEmpty()) {
            HieraticalSystem firstChild = sortedChildren.get(0);
            if (isBinaryRelation(object, firstChild)) {
                return firstChild.master;
            }
        }
        
        return null;
    }

    private void orbitRelated(Simulator simulator, UnitsConverter uc) {
        CelestialObject parent = object.getHillMaster();
        HieraticalSystem system = simulator.getHieraticalSystem(object);
//        childrenCountLabel.setText(String.valueOf(system.nChildren()));
        int level = object.getLevelFromStar();
        if (level == 0) {
            CelestialObject binaryPartner = getBinaryStar(system, parent);
            if (binaryPartner == null) {
                hieraticalLabel.setText(levelName(level, system.isRoot()));
                binaryPairPrompt.setVisible(false);
                binaryPairLabel.setVisible(false);
            } else {
                hieraticalLabel.setText(strings.getString("levelBinaryStar"));
                binaryPairPrompt.setVisible(true);
                binaryPairLabel.setVisible(true);
                binaryPairLabel.setText(binaryPartner.getNameShowing());
            }
        } else if (level == 1 || level == 2) {
            CelestialObject binaryPartner = getBinaryPlanet(system, parent);
            if (binaryPartner == null) {
                hieraticalLabel.setText(levelName(level, system.isRoot()));
                binaryPairPrompt.setVisible(false);
                binaryPairLabel.setVisible(false);
            } else {
                hieraticalLabel.setText(strings.getString("levelBinaryPlanet"));
                binaryPairPrompt.setVisible(true);
                binaryPairLabel.setVisible(true);
                binaryPairLabel.setText(binaryPartner.getNameShowing());
            }
        } else {
            hieraticalLabel.setText(levelName(level, false));
            binaryPairPrompt.setVisible(false);
            binaryPairLabel.setVisible(false);
        }

        if (!system.isObject()) {
            HieraticalSystem.SystemStats systemStats = system.getCurStats(simulator);
            circlingChildrenCountLabel.setText(String.valueOf(systemStats.getNClosedObject() - 1));
            subsystemMassLabel.setText(uc.mass(systemStats.getCirclingMass()));
        } else {
            circlingChildrenCountLabel.setText("--");
            subsystemMassLabel.setText("--");
        }

        if (parent != null && parent.getMass() > object.getMass() * Simulator.PLANET_MAX_MASS) {
            parentLabel.setText(parent.getNameShowing());
            HieraticalSystem parentSystem = simulator.getHieraticalSystem(parent);

            double orbitBinding = parentSystem.bindingEnergyOf(system, simulator);
//            System.out.println(object.getName() + " " + orbitBinding);
            boolean isOrbiting = orbitBinding < 0;
            if (isOrbiting) {
                orbitStatusLabel.setText(strings.getString("statusOrbiting"));
            } else {
                orbitStatusLabel.setText(strings.getString("statusEscape"));
            }

            double[] orbitPlaneNormal = system.getEclipticPlaneNormal();

            double axisTiltToOrbit = Math.acos(VectorOperations.dotProduct(
                    object.getRotationAxis(),
                    orbitPlaneNormal
            ));
            rotationAxisTiltLabel.setText(uc.angleDegreeDecimal(Math.toDegrees(axisTiltToOrbit)));

//            double[] barycenter = OrbitCalculator.calculateBarycenter(system, parent);
//            double[] velocity = VectorOperations.subtract(system.getVelocity(), parent.getVelocity());
//            double[] position = VectorOperations.subtract(system.getPosition(), parent.getPosition());
//
//            if (level >= 2) {
//                // moons
//                double[] parentEclipticNormal = parentSystem.getEclipticPlaneNormal();
//                position = SystemPresets.rotateFromXYPlaneToPlanetEclipticPlane(position, parentEclipticNormal);
//                position = SystemPresets.rotateFromPlanetEclipticPlaneToEquatorialPlane(position, parent.getRotationAxis());
//                velocity = SystemPresets.rotateFromXYPlaneToPlanetEclipticPlane(velocity, parentEclipticNormal);
//                velocity = SystemPresets.rotateFromPlanetEclipticPlaneToEquatorialPlane(velocity, parent.getRotationAxis());
//            } else {
//                // planets
//                double[] parentEclipticNormal = parentSystem.getEclipticPlaneNormal();
//                position = SystemPresets.rotateFromXYPlaneToPlanetEclipticPlane(position, parentEclipticNormal);
//                velocity = SystemPresets.rotateFromXYPlaneToPlanetEclipticPlane(velocity, parentEclipticNormal);
//            }
            // todo: inclination, etc. relative to parent
            FullOrbitSpec fos = simulator.computeOrbitOf(object, parent, true);
            orbitalElements = fos.elements;
//            orbitalElements = OrbitCalculator.computeOrbitSpecs3d(position,
//                    velocity,
//                    system.getMass() + parent.getMass(),
//                    simulator.getG());

            if (isOrbiting != orbitalElements.isElliptical()) {
                System.out.println(object.getId() + " has marginal orbit with ecc: " +
                        orbitalElements.eccentricity + ", energy: " +
                        orbitBinding);
            }

            double dt = VectorOperations.distance(object.getPosition(), parent.getPosition());
            distanceLabel.setText(uc.distance(dt));

            double a = orbitalElements.semiMajorAxis;
            double e = orbitalElements.eccentricity;
            double aph = a * (1 + e);
            double per = a * (1 - e);

            double mean = a * (1 - Math.pow(e, 2) / 2);

            semiMajorLabel.setText(uc.distance(a));
            eccLabel.setText(String.format("%.4f", e));
            if (orbitalElements.isElliptical()) {
                avgDistanceLabel.setText(uc.distance(mean));
                periodLabel.setText(uc.time(orbitalElements.period));
                aphelionLabel.setText(uc.distance(aph));
            } else {
                avgDistanceLabel.setText("--");
                periodLabel.setText("--");
                aphelionLabel.setText("--");
            }

            perihelionLabel.setText(uc.distance(per));
            inclinationLabel.setText(uc.angleDegreeDecimal(orbitalElements.inclination));
            ascendingNodeLabel.setText(uc.angleDegreeDecimal(orbitalElements.ascendingNode));
            hillRadiusLabel.setText(uc.radius(object.getHillRadius()));
        } else {
            parentLabel.setText("free");
            distanceLabel.setText("--");
            avgDistanceLabel.setText("--");
            periodLabel.setText("--");
            semiMajorLabel.setText("--");
            eccLabel.setText("--");
            aphelionLabel.setText("--");
            perihelionLabel.setText("--");
            inclinationLabel.setText("--");
            ascendingNodeLabel.setText("--");
            hillRadiusLabel.setText("--");
            rotationAxisTiltLabel.setText(uc.angleDegreeDecimal(object.getAxisTiltAngle()));
        }
    }

    private String levelName(int level, boolean isRoot) {
        return switch (level) {
            case 0 -> strings.getString("levelStar");
            case 1 ->
                    isRoot ? strings.getString("levelCentralBody") : strings.getString("levelPlanet");
            case 2 -> strings.getString("levelMoon");
            default -> String.format(strings.getString("levelSecondaryMoonFmt"), level - 1);
        };
    }

    private String objectType() {
        BodyType bodyType = object.getBodyType();
        if (bodyType == BodyType.STAR) {
            if (object.getStatus() instanceof Star star) {
                return starType(star.getEmissionColorTemperature());
            } else {
                return "";  // should not happen
            }
        } else if (bodyType == BodyType.BROWN_DWARF) {
            return strings.getString("brownDwarf");
        } else if (bodyType == BodyType.GAS_GIANT) {
            return strings.getString("gasGiant");
        } else if (bodyType == BodyType.ICE_GIANT) {
            return strings.getString("iceGiant");
        } else {
            if (object.getStatus() instanceof Comet) {
                return strings.getString("comet");
            } else {
                int level = object.getLevelFromStar();
                if (level == 0) {
                    return "";  // should not happen
                } else if (level == 1) {
                    if (object.getMass() > SystemPresets.MOON_MASS) {
                        return strings.getString("terrestrialPlanet");
                    } else {
                        return strings.getString("smallPlanet");
                    }
                } else {
                    return strings.getString("levelMoon");
                }
            }
        }
    }

    private String starType(double colorTemp) {
        if (colorTemp == 0) return "";

        final String[] classes = {"O", "B", "A", "F", "G", "K", "M"};
        final double[] thresholds = {50000, 33000, 10000, 7500, 6000, 5200, 3700, 2400};

        String type;
        if (colorTemp < thresholds[thresholds.length - 1]) type = "L";
        else {
            int classIndex = 1;  // start from 33000
            for (; classIndex < thresholds.length; classIndex++) {
                if (colorTemp >= thresholds[classIndex]) {
                    break;
                }
            }
            double tUpper = thresholds[classIndex - 1], tLower = thresholds[classIndex];
            type = classes[classIndex - 1] + starSubDivision(colorTemp, tUpper, tLower);
        }

        return strings.getString("spectralClfFmt").formatted(type);
    }

    private int starSubDivision(double colorTemp, double tUpper, double tLower) {
        return (int) ((tUpper - colorTemp) / (tUpper - tLower) * 10);
    }

    @FXML
    public void showOrbitPane() {
        if (!hasExpanded) {
            initDetailPane();
            hasExpanded = true;
        }

        onExpand.accept(this);

        detailPane.setManaged(true);
        detailPane.setVisible(true);

        showOrbitPaneBtn.setManaged(false);
        showOrbitPaneBtn.setVisible(false);
    }

    @FXML
    public void hideOrbitPane() {
        detailPane.setManaged(false);
        detailPane.setVisible(false);

        starPane.setVisible(false);
        starPane.setManaged(false);

        planetPane.setVisible(false);
        planetPane.setManaged(false);

        showOrbitPaneBtn.setManaged(true);
        showOrbitPaneBtn.setVisible(true);

        onCollapse.run();
    }
}
