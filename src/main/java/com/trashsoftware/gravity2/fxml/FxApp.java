package com.trashsoftware.gravity2.fxml;

import com.jme3.system.AppSettings;
import com.trashsoftware.gravity2.fxml.units.AdaptiveUnitsConverter;
import com.trashsoftware.gravity2.fxml.units.OriginalUnitsConverter;
import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.gui.JmeApp;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.physics.SystemPresets;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FxApp extends Application {
    public static final String SAVE_PATH = "saves";
    
    private static FxApp instance;

    private static ResourceBundle strings;
    
    private JmeApp jmeApp;
    
    private UiUpdateTimer uiUpdateTimer;
    private Timer textRefresher = new Timer();
    private long textRefreshInterval = 250;
    
    private boolean stopped = false;
    
    private Stage primaryStage;
    private ControlBar controlBar;
    private ObjectListPanel objectListPanel;
    private final List<ObjectPanel> objectInfoPanels = new ArrayList<>();

    public static void startApp(String[] args) {
        launch(args);
    }

    public static FxApp getInstance() {
        return instance;
    }

    private static void initDirectories() {
        File saveDir = new File(SAVE_PATH);
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                System.err.println("Cannot create " + saveDir.getAbsolutePath());
            }
        }
    }

    public static ResourceBundle getStrings() {
        return strings;
    }

    public static void reloadStrings() {
//        Locale locale = ConfigLoader.getInstance().getLocale();
        Locale locale = Locale.getDefault();
        strings = ResourceBundle.getBundle(
                "com.trashsoftware.gravity2.strings.Strings",
                locale);
    }

    public ControlBar getControlBar() {
        return controlBar;
    }
    
    public void notifyObjectCountChanged(Simulator simulator) {
        // Since updating info pane can be time-consuming,
        // there might be this scenario:
        // the reload info pane in running, but the jme ui has triggered new simulation steps
        // which modifies the simulators object list
        // causing concurrent modification exception
        List<CelestialObject> objectsCopy = new ArrayList<>(simulator.getObjects());
        Platform.runLater(() -> {
            if (objectListPanel != null) {
                objectListPanel.reloadInfoPane(simulator, objectsCopy);
            }
        });
    }

    public ObjectListPanel getObjectListPanel() {
        return objectListPanel;
    }

    public UiUpdateTimer getUiUpdateTimer() {
        return uiUpdateTimer;
    }

    public JmeApp getJmeApp() {
        return jmeApp;
    }
    
    public Simulator getSimulator() {
        if (jmeApp == null) return null;
        return jmeApp.getSimulator();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        
        this.primaryStage = primaryStage;

        reloadStrings();
        initDirectories();
        
        new Thread(() -> {
            jmeApp = new JmeApp(this);
            AppSettings settings = new AppSettings(true);
            settings.setWidth(1536);
            settings.setHeight(864);
            jmeApp.setSettings(settings);
            jmeApp.setShowSettings(false);
            jmeApp.setPauseOnLostFocus(false); // Ensure continuous rendering
            jmeApp.start(); // Start in standard mode (creates its own window)
            
            jmeApp.setTracePathOrbit(true, false, false);
        }).start();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("controlBar.fxml"),
                strings
        );
        Pane parent = loader.load();
        Scene scene = new Scene(parent);

        primaryStage.setTitle(strings.getString("appName"));
        primaryStage.setScene(scene);

        controlBar = loader.getController();
        controlBar.setWindow(primaryStage, this);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        
//        primaryStage.setX(screenWidth * 0.5);
        primaryStage.setY(screenHeight * 0.8);

        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();
        
        createObjectListPanel();
        
        uiUpdateTimer = new UiUpdateTimer();
        uiUpdateTimer.start();

        textRefresher.scheduleAtFixedRate(new TextRefreshTask(), 0, textRefreshInterval);
    }
    
    private void createObjectListPanel() throws IOException {
        Stage stage = new Stage(StageStyle.DECORATED);
        stage.initOwner(primaryStage);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("objectListPanel.fxml"),
                strings
        );
        Pane parent = loader.load();
        Scene scene = new Scene(parent);
        
        stage.setScene(scene);
        
        objectListPanel = loader.getController();
        objectListPanel.setWindow(stage, this);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

//        primaryStage.setX(screenWidth * 0.5);
        stage.setX(screenWidth * 0.7);
        
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (stopped) return;
        
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
            textRefresher.cancel();
        }
        
        stopped = true;
        if (jmeApp != null) {
            jmeApp.stop();
        }
        Platform.exit();
    }

    public UnitsConverter getUnitConverter() {
        return UnitMethod.ADAPTIVE.unitsConverter;
//        UnitMethod um = unitsMethodBox.getValue();
//        return um == null ? UnitMethod.ADAPTIVE.unitsConverter : um.unitsConverter;
    }
    
    class UiUpdateTimer extends AnimationTimer {
        
        long lastNano;

        @Override
        public void handle(long now) {
            double frameTimeMs = 0;
            if (lastNano != 0) {
                frameTimeMs = (now - lastNano) / 1e9;
            }
            if (controlBar != null) {
                controlBar.oneFrameFast(frameTimeMs);
            }
            lastNano = now;
        }
    }

    class TextRefreshTask extends TimerTask {

        @Override
        public void run() {
            Platform.runLater(() -> {
                if (controlBar != null) {
                    controlBar.oneFrameSlow(textRefreshInterval);
                }
                if (objectListPanel != null) {
                    objectListPanel.oneFrameSlow(textRefreshInterval);
                }
                for (ObjectPanel op : objectInfoPanels) {
                    op.oneFrameSlow(textRefreshInterval);
                }
            });
        }
    }

    public enum UnitMethod {
        BASE("unitBase", new OriginalUnitsConverter()),
        ADAPTIVE("unitAdaptive", new AdaptiveUnitsConverter());

        private final String key;
        public final UnitsConverter unitsConverter;

        UnitMethod(String key, UnitsConverter uc) {
            this.key = key;
            this.unitsConverter = uc;
        }

        @Override
        public String toString() {
            return FxApp.getStrings().getString(key);
        }
    }
}
