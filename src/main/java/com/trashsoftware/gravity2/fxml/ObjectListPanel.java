package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.gui.JmeApp;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.presets.Preset;
import com.trashsoftware.gravity2.presets.SystemPresets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class ObjectListPanel extends AbstractObjectPanel {

    @FXML
    GridPane infoPane;
    @FXML
    Label totalKineticText, totalPotentialText, totalInternalText, totalEnergyText,
            totalMassText, nObjectsText;
    @FXML
    ScrollPane celestialContainer;
    @FXML
    VBox celestialListPane;
    @FXML
    ComboBox<Sorting> sortBox;
    @FXML
    ComboBox<FxApp.UnitMethod> unitsMethodBox;
    @FXML
    CheckMenuItem gravityContourMenu, effPotentialContourMenu;
    @FXML
    Label spawnPrompt, spawnMassText, spawnRadiusText, spawnDensityText;
    @FXML
    ToggleGroup spawnOrbitGroup;
    @FXML
    RadioButton spawnStillBtn, spawnOrbitBtn, SpawnEllipseBtn, spawnParabolicBtn;
    private Map<Toggle, Double> orbitSpeedMultipliers;
    @FXML
    TextField createNameInput, createMassInput, createRadiusInput, speedMulInput;
    @FXML
    ColorPicker colorPicker;
    @FXML
    ComboBox<SpawnPreset> spawnPresetBox;
    @FXML
    Menu presetsMenu;
    private double celestialContainerVValueCache;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);

        setComboBoxes();
        setCheckBoxes();
        setInputsFields();
        setMenu();
    }

    @Override
    public void oneFrameSlow(double frameTimeMs) {
        Simulator simulator = fxApp.getSimulator();
        if (simulator == null) return;

        setTexts(simulator);
        setInfo(simulator);
    }
    
    private void setMenu() {
        for (Preset preset : Preset.DEFAULT_PRESETS) {
            PresetMenuItem menuItem = new PresetMenuItem(preset);
            presetsMenu.getItems().add(menuItem);
        }
    }

    private void setCheckBoxes() {
        orbitSpeedMultipliers = Map.of(
                spawnStillBtn, 0.0,
                spawnOrbitBtn, 1.0,
                SpawnEllipseBtn, 0.25,
                spawnParabolicBtn, 2.0
        );

        spawnOrbitGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            Double speedMul = orbitSpeedMultipliers.get(newValue);
            if (speedMul != null) {
                speedMulInput.setText(String.valueOf(speedMul));
            }
        });
    }

    private void setComboBoxes() {
        sortBox.getItems().addAll(Sorting.values());
        sortBox.getSelectionModel().select(0);

        sortBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Simulator simulator = fxApp.getSimulator();
            if (simulator != null) {
                reloadInfoPane(simulator, simulator.getObjects());
            }
        });

        unitsMethodBox.getItems().addAll(FxApp.UnitMethod.values());
        unitsMethodBox.getSelectionModel().select(FxApp.UnitMethod.ADAPTIVE);

        spawnPresetBox.getItems().add(new SpawnPreset(null));
        spawnPresetBox.getItems().addAll(SystemPresets.PRESET_OBJECTS.stream().map(SpawnPreset::new).toList());
        spawnPresetBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.value != null) {
                colorPicker.setDisable(true);
                createNameInput.setText(newValue.value.name);
                createMassInput.setText(String.valueOf(newValue.value.mass));
                createRadiusInput.setText(String.valueOf(newValue.value.radius * 1e3));
            } else {
                colorPicker.setDisable(false);
            }
        });
    }

    private void setInputsFields() {
        createMassInput.textProperty().addListener((observable, oldValue, newValue) ->
                updateSpawnMass(newValue, createRadiusInput.getText()));
        createRadiusInput.textProperty().addListener((observable, oldValue, newValue) ->
                updateSpawnMass(createMassInput.getText(), newValue));
    }

    private void updateSpawnMass(String massText, String radiusText) {
        double mass, radius;
        try {
            mass = Double.parseDouble(massText);
            radius = Double.parseDouble(radiusText);
        } catch (NumberFormatException e) {
            spawnDensityText.setText("--");
            return;
        }
        double volume = 4.0 / 3.0 * Math.PI * Math.pow(radius, 3);
        double density = mass / volume;
        UnitsConverter uc = fxApp.getUnitConverter();
        spawnMassText.setText(uc.mass(mass));
        spawnRadiusText.setText(uc.distance(radius));
        spawnDensityText.setText(uc.mass(density) + "/m³");
    }

    private void setInfo(Simulator simulator) {
        Node root = celestialContainer.getContent();
        if (root == celestialListPane) {
            for (Node node : celestialListPane.getChildren()) {
                if (node instanceof ObjectStatsWrapper objectStatsWrapper) {
                    objectStatsWrapper.update(simulator, unitsMethodBox.getValue().unitsConverter);
                }
            }
        } else if (root instanceof ObjectStatsWrapper osw) {
            if (osw.object.isExist()) {
                osw.update(simulator, unitsMethodBox.getValue().unitsConverter);
            } else {
                collapseObjectStats();
            }
        }
    }

    private void setTexts(Simulator simulator) {
        UnitsConverter uc = fxApp.getUnitConverter();
        double kinetic = simulator.calculateTotalKineticEnergy();
        double potential = simulator.calculateTotalPotentialEnergy();
        double internal = simulator.calculateTotalInternalEnergy();

        totalPotentialText.setText(uc.energy(potential));
        totalKineticText.setText(uc.energy(kinetic));
        totalInternalText.setText(uc.energy(internal));
        totalEnergyText.setText(uc.energy(potential + kinetic + internal));

        List<CelestialObject> objects = simulator.getObjects();
        nObjectsText.setText(String.format("%,d", objects.size()));
        totalMassText.setText(uc.mass(objects
                .stream()
                .map(CelestialObject::getMass)
                .reduce(0.0, Double::sum)
        ));
    }

    public void reloadInfoPane(Simulator simulator, List<CelestialObject> loadObjects) {
        long t0 = System.currentTimeMillis();

        Map<CelestialObject, ObjectStatsWrapper> infoMap = new HashMap<>();
        for (Node node : celestialListPane.getChildren()) {
            if (node instanceof ObjectStatsWrapper objectStatsWrapper) {
                infoMap.put(objectStatsWrapper.object, objectStatsWrapper);
            }
        }

        celestialListPane.getChildren().clear();

        Sorting sorting = sortBox.getSelectionModel().getSelectedItem();
        List<CelestialObject> objectList;
        switch (sorting) {
            default -> objectList = loadObjects;
            case MASS -> {
                objectList = new ArrayList<>(loadObjects);
                objectList.sort(Comparator.comparingDouble(CelestialObject::getMass));
                Collections.reverse(objectList);
            }
            case HIERATICAL -> objectList = simulator.getObjectsSortByHieraticalDistance();
        }

        JmeApp jmeApp = fxApp.getJmeApp();
        if (jmeApp != null) {
            if (jmeApp.getFocusing() != null && !objectList.contains(jmeApp.getFocusing())) {
                fxApp.getControlBar().clearFocusAction();
            }
        }

        for (CelestialObject object : objectList) {
            ObjectStatsWrapper oi = infoMap.computeIfAbsent(object,
                    o -> new ObjectStatsWrapper(
                            object,
                            simulator,
                            unitsMethodBox.getValue().unitsConverter,
                            () -> fxApp.getJmeApp().focusOn(object, false),
                            this::expandObjectStats,
                            obj -> fxApp.getJmeApp().landOn(obj),
                            this::collapseObjectStats,
                            strings
                    ));
            celestialListPane.getChildren().add(oi);
        }

        System.out.println("Reload info pane in " + (System.currentTimeMillis() - t0));
    }

    private void expandObjectStats(ObjectStatsWrapper osw) {
        celestialContainerVValueCache = celestialContainer.getVvalue();
        celestialContainer.setContent(osw);
    }

    private void collapseObjectStats() {
        celestialContainer.setContent(celestialListPane);
        reloadInfoPane(fxApp.getSimulator(), fxApp.getSimulator().getObjects());
        celestialContainer.setVvalue(celestialContainerVValueCache);
    }

    public void reshow() {
        Platform.runLater(() -> window.show());
    }

    public boolean isObjectExpanded() {
        Node root = celestialContainer.getContent();
        return root instanceof ObjectStatsWrapper;
    }

    @FXML
    public void saveAction() {
        fxApp.getControlBar().saveAction();
    }

    @FXML
    public void loadAction() {
        fxApp.getControlBar().loadAction();
    }
    
    @FXML
    public void presetsAction() {
        
    }

    @FXML
    public void spawnModeAction() {
        JmeApp jmeApp = fxApp.getJmeApp();
        if (jmeApp == null) return;
        Simulator simulator = jmeApp.getSimulator();

        if (jmeApp.isInSpawningMode()) {
            jmeApp.exitSpawningModeEnqueue();
        } else {
            String name = createNameInput.getText();
            String massText = createMassInput.getText();
            String radiusText = createRadiusInput.getText();

            if (name.isBlank()) {
                spawnPrompt.setText(strings.getString("spawnPromptInvalidName"));
                return;
            }
            double mass, radius;
            try {
                mass = Double.parseDouble(massText);
                radius = Double.parseDouble(radiusText);
            } catch (NumberFormatException e) {
                spawnPrompt.setText(strings.getString("spawnPromptInvalidNumber"));
                return;
            }

            String speedMulText = speedMulInput.getText();
            double speed;
            try {
                speed = Double.parseDouble(speedMulText);
            } catch (NumberFormatException e) {
                spawnPrompt.setText(strings.getString("spawnPromptInvalidSpeed"));
                return;
            }

            SpawnPreset sp = spawnPresetBox.getValue();
            SystemPresets.ObjectInfo presetInfo = sp == null ? null : sp.value;
            CelestialObject spawning;
            if (presetInfo == null) {
                Color color = colorPicker.getValue();
                String colorCode = GuiUtils.fxColorToHex(color);

                spawning = CelestialObject.createNd(name, mass, radius, simulator.getDimension(),
                        colorCode);
            } else {
                spawning = SystemPresets.createObjectPreset(
                        simulator,
                        presetInfo,
                        new double[simulator.getDimension()],
                        new double[simulator.getDimension()],
                        1.0
                );
            }

            spawnPrompt.setText("");
            jmeApp.enterSpawningMode(spawning, speed);
        }
    }

    private double getSpawningSpeed() {
        String speedMulText = speedMulInput.getText();
        double speed;
        try {
            speed = Double.parseDouble(speedMulText);
        } catch (NumberFormatException e) {
            spawnPrompt.setText(strings.getString("spawnPromptInvalidSpeed"));
            return 0;
        }
        return speed;
    }

    public void scrollTo(CelestialObject co) {
        Node root = celestialContainer.getContent();
        if (root == celestialListPane) {
            Node target = null;
            for (int i = 0; i < celestialListPane.getChildren().size(); i++) {
                Node node = celestialListPane.getChildren().get(i);
                if (node instanceof ObjectStatsWrapper osw) {
                    if (co == osw.object) {
                        target = node;
                        break;
                    }
                }
            }
            if (target != null) {
                // Get the bounds of the node relative to the ScrollPane content
                Bounds nodeBounds = target.localToScene(target.getBoundsInLocal());
                Bounds scrollPaneBounds = celestialContainer.localToScene(celestialContainer.getBoundsInLocal());

                // Calculate the position to scroll
                double scrollTop = nodeBounds.getMinY() - scrollPaneBounds.getMinY();

                // Scroll to the node (vertically in this case)
                celestialContainer.setVvalue(scrollTop / (celestialContainer.getContent().getBoundsInLocal().getHeight() - celestialContainer.getViewportBounds().getHeight()));
            } else {
                System.err.println("?");
            }
        }
    }
    
    public class PresetMenuItem extends MenuItem {
        final Preset preset;
        PresetMenuItem(Preset preset) {
            super();
            
            this.preset = preset;

            String shown = preset.name;
            
            shown += " " + String.format(strings.getString("nObjectsFmt"), preset.nObjects);
            setText(shown);
            
            setOnAction(e -> fxApp.getControlBar().loadPreset(preset));
        }
    }
}
