package com.trashsoftware.gravity2.fxml;

import com.trashsoftware.gravity2.physics.SystemPresets;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public abstract class AbstractObjectPanel implements Initializable {

    protected Stage window;
    protected FxApp fxApp;
    protected ResourceBundle strings;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }

    public void setWindow(Stage window, FxApp fxApp) {
        this.window = window;
        this.fxApp = fxApp;
    }

    public abstract void oneFrameSlow(double frameTimeMs);

    public enum Sorting {
        DEFAULT("sortDefault"),
        MASS("sortMass"),
        HIERATICAL("sortHieratical");

        private final String key;

        Sorting(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return FxApp.getStrings().getString(key);
        }
    }

    public static class SpawnPreset {

        protected final SystemPresets.ObjectInfo value;

        protected SpawnPreset(SystemPresets.ObjectInfo value) {
            this.value = value;
        }

        @Override
        public String toString() {
            if (value == null) {
                return FxApp.getStrings().getString("spawnPresetCustom");
            } else {
                return value.name();
            }
        }
    }
}
