package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.fxml.units.UnitsUtil;
import com.trashsoftware.gravity2.gui.JmeApp;
import com.trashsoftware.gravity2.physics.Simulator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.ResourceBundle;

public class ControlBar implements Initializable {
    @FXML
    Label speedLabel, realSpeedLabel;
    @FXML
    Label timeStepText;
    @FXML
    Button playPauseBtn, clearFocusBtn, clearLandBtn;
    @FXML
    Slider pathLengthSlider, massPercentileSlider;
    @FXML
    Label pathLengthText, massPercentileText;
    @FXML
    ToggleGroup orbitShowingGroup, refFrameGroup;
    @FXML
    RadioButton showTraceBtn, showPathBtn, showOrbitBtn;
    @FXML
    RadioButton refStaticBtn, refSystemBtn, refTargetBtn;
    @FXML
    CheckMenuItem nameOnCanvasCheck, barycenterCheck, hillSpheresCheck, rocheLimitCheck;
    @FXML
    CheckMenuItem ellipticalOnlyCheck;

    private Stage window;
    private FxApp fxApp;
    private ResourceBundle strings;

    private double lastRealTimeStep;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        setRadioButtons();
        setCheckBoxes();
        setSliders();
    }

    public void setWindow(Stage window, FxApp fxApp) {
        this.window = window;
        this.fxApp = fxApp;
    }
    
    private void setRadioButtons() {
        orbitShowingGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            JmeApp jmeApp = getJmeApp();
            if (jmeApp == null) return;
            if (newValue == showTraceBtn) {
                jmeApp.setTracePathOrbit(true, false, false);
            } else if (newValue == showPathBtn) {
                jmeApp.setTracePathOrbit(false, true, false);
            } else if (newValue == showOrbitBtn) {
                jmeApp.setTracePathOrbit(true, false, true);
            }
        });
        
        refFrameGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            JmeApp jmeApp = getJmeApp();
            if (jmeApp == null) return;
            if (newValue == refStaticBtn) {
                jmeApp.setRefFrame(JmeApp.RefFrame.STATIC);
            } else if (newValue == refSystemBtn) {
                jmeApp.setRefFrame(JmeApp.RefFrame.SYSTEM);
            } else if (newValue == refTargetBtn) {
                jmeApp.setRefFrame(JmeApp.RefFrame.TARGET);
            }
        });
    }
    
    private void setCheckBoxes() {
        nameOnCanvasCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
            JmeApp jmeApp = getJmeApp();
            if (jmeApp == null) return;
            jmeApp.toggleLabelShowing(newValue);
        });
        
        barycenterCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
            JmeApp jmeApp = getJmeApp();
            if (jmeApp == null) return;
            jmeApp.toggleBarycenterShowing(newValue);
        });
    }
    
    private void setSliders() {
        pathLengthSlider.setMin(1);
        pathLengthSlider.setMax(50000);

        pathLengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double pathLength = newValue.doubleValue();
            pathLengthText.setText(String.format("%,.0f", pathLength));

            JmeApp jmeApp = getJmeApp();
            if (jmeApp == null) return;
            jmeApp.setPathLength(pathLength);
        });
        pathLengthSlider.setValue(5000.0);
    }
    
    public void setFocus() {
        clearFocusBtn.setDisable(false);
    }
    
    public void setLand() {
        clearLandBtn.setDisable(false);
        refTargetBtn.setSelected(true);
        
        refStaticBtn.setDisable(true);
        refSystemBtn.setDisable(true);
        refTargetBtn.setDisable(true);
    }

    @FXML
    public void clearFocusAction() {
        getJmeApp().clearFocus();
        clearFocusBtn.setDisable(true);
//        effPotentialContourMenu.setSelected(false);
//        effPotentialContourMenu.setDisable(true);
    }
    
    @FXML
    public void clearLandAction() {
        getJmeApp().clearLand();
        clearLandBtn.setDisable(true);

        refStaticBtn.setDisable(false);
        refSystemBtn.setDisable(false);
        refTargetBtn.setDisable(false);
    }
    
    @FXML
    public void speedUpAction() {
        getJmeApp().speedUpAction();
    }
    
    @FXML
    public void speedDownAction() {
        getJmeApp().speedDownAction();
    }

    @FXML
    public void playPauseAction() {
        if (getJmeApp().isPlaying()) pause();
        else play();
    }

    void play() {
//        gravityContourMenu.setSelected(false);
//        effPotentialContourMenu.setSelected(false);
        getJmeApp().setPlaying(true);
        playPauseBtn.setText("⏸");
    }

    void pause() {
        getJmeApp().setPlaying(false);
        playPauseBtn.setText("⏵");
    }
    
    @FXML
    public void showObjectPaneAction() {
        Window objWindow = fxApp.getObjectListPanel().getWindow();
        if (objWindow.isShowing()) {
            objWindow.requestFocus();
        } else {
            fxApp.getObjectListPanel().reshow();
        }
    }

    public FxApp getFxApp() {
        return fxApp;
    }
    
    public JmeApp getJmeApp() {
        if (getFxApp() == null) return null;
        return getFxApp().getJmeApp();
    }
    
    public void oneFrameFast(double frameTimeMs) {
        
    }

    public void oneFrameSlow(double frameTimeMs) {
        UnitsConverter uc = getFxApp().getUnitConverter();
        
        // update playing speed
        Simulator simulator = getJmeApp().getSimulator();
        if (simulator == null) return;
        double timeStep = simulator.getTimeStepAccumulator();
        double diff = timeStep - lastRealTimeStep;
        double realDiff = diff * (1000.0 / frameTimeMs);
        realSpeedLabel.setText(UnitsUtil.adaptiveTime(realDiff) + "/s");
        lastRealTimeStep = timeStep;

        speedLabel.setText(getJmeApp().getSimulationSpeed() + "x");
        
        timeStepText.setText(uc.dateTime(timeStep, strings));
    }
}
