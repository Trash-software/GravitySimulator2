package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.gui.JmeApp;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.physics.SystemPresets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

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
    Label spawnPrompt, spawnDensityText;
    @FXML
    ToggleGroup spawnOrbitGroup;
    @FXML
    RadioButton spawnStillBtn, spawnOrbitBtn, SpawnEllipseBtn, spawnParabolicBtn;
    @FXML
    TextField createNameInput, createMassInput, createRadiusInput, speedMulInput;
    @FXML
    ColorPicker colorPicker;
    @FXML
    ComboBox<SpawnPreset> spawnPresetBox;
    private double celestialContainerVValueCache;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        
        setComboBoxes();
    }

    @Override
    public void oneFrameSlow(double frameTimeMs) {
        Simulator simulator = fxApp.getSimulator();
        if (simulator == null) return;
        
        setTexts(simulator);
        setInfo(simulator);
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
                createRadiusInput.setText(String.valueOf(newValue.value.radius));
            } else {
                colorPicker.setDisable(false);
            }
        });
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
                            () -> fxApp.getJmeApp().focusOn(object),
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
    
    @FXML
    public void spawnModeAction() {
        JmeApp jmeApp = fxApp.getJmeApp();
        if (jmeApp == null) return;
        
        
        
    }
}
