package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.fxml.units.UnitsUtil;
import com.trashsoftware.gravity2.gui.JmeApp;
import com.trashsoftware.gravity2.physics.Simulator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ControlBar implements Initializable {
    @FXML
    Label speedLabel, realSpeedLabel;
    @FXML
    Label timeStepText;
    @FXML
    Button playPauseBtn, clearFocusBtn;
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
    }
    
    public void setFocus() {
        clearFocusBtn.setDisable(false);
    }

    @FXML
    public void clearFocusAction() {
        getJmeApp().clearFocus();
        clearFocusBtn.setDisable(true);
//        effPotentialContourMenu.setSelected(false);
//        effPotentialContourMenu.setDisable(true);
    }
    
    @FXML
    public void speedUpAction() {
        getJmeApp().speedUpAction();
        speedLabel.setText(getJmeApp().getSimulationSpeed() + "x");
    }
    
    @FXML
    public void speedDownAction() {
        getJmeApp().speedDownAction();
        speedLabel.setText(getJmeApp().getSimulationSpeed() + "x");
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

    public FxApp getFxApp() {
        return fxApp;
    }
    
    public JmeApp getJmeApp() {
        return getFxApp().getJmeApp();
    }
    
    public void oneFrameFast(double frameTimeMs) {
        
    }

    public void oneFrameSlow(double frameTimeMs) {
        // update playing speed
        Simulator simulator = getJmeApp().getSimulator();
        if (simulator == null) return;
        double timeStep = simulator.getTimeStepAccumulator();
        double diff = timeStep - lastRealTimeStep;
        double realDiff = diff * (1000.0 / frameTimeMs);
        realSpeedLabel.setText(UnitsUtil.adaptiveTime(realDiff) + "/s");

        lastRealTimeStep = timeStep;
    }
}
