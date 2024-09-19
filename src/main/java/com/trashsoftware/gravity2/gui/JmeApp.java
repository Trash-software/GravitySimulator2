package com.trashsoftware.gravity2.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.fxml.FxApp;
import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.physics.*;
import com.trashsoftware.gravity2.presets.Preset;
import com.trashsoftware.gravity2.presets.SystemPresets;
import javafx.application.Platform;

import java.util.*;

public class JmeApp extends SimpleApplication {
    private static JmeApp instance;

    // Load the font for the labels
    protected BitmapFont font;
    protected ColorRGBA backgroundColor = ColorRGBA.Black;

    private MouseManagement leftButton = new MouseManagement();
    private MouseManagement rightButton = new MouseManagement();
    private MouseManagement midButton = new MouseManagement();

    private boolean keyWPressed, keyUpPressed, keyDownPressed;

    private float horizontalSpeed = 50.0f;
    private float verticalSpeed = 50.0f;
    private float rotationSpeed = 3.0f;

    private float azimuthSensitivity = 50.0f;
    private float altitudeAngleSensitivity = 50.0f;
//    private Vector3f pivotPoint = Vector3f.ZERO;  // Assuming the object is at the origin

    private double pathLength = 5000.0;

    protected Simulator simulator;
    protected double speed = 1.0;
    protected boolean playing = true;
    protected double scale = 1.0;
    private final Vector3d screenCenter = new Vector3d();  // must be double, otherwise the distance object will jump
    private double refOffsetX, refOffsetY, refOffsetZ;
    //    private double focusingLastX, focusingLastY, focusingLastZ;
    private final Vector3f centerRelToFocus = new Vector3f();

    private Vector3f lookAtPoint = new Vector3f(0, 0, 0);
    private Vector3f worldUp = Vector3f.UNIT_Z;

    //    private final Map<CelestialObject, Geometry> lineGeometries = new HashMap<>();
    private final Map<CelestialObject, ObjectModel> modelMap = new HashMap<>();
    private final Map<CelestialObject, ObjectModel> diedObjects = new HashMap<>();
    //    private List<Geometry> tempGeom = new ArrayList<>();
//    private Node rootLabelNode = new Node("RootLabelNode");
    private Node axisMarkNode;
    private Node globalBarycenterNode;
    private GridPlane gridPlaneNode;
    private CompassNode compassNode;
    private GuiTextNode lonLatTextNode;
    private boolean showLabel = true;
    private boolean showBarycenter = false;
    private boolean showTrace, showFullPath, showOrbit;
    private boolean eclipticOrbitOnly;
    private double minimumMassShowing;
    private CelestialObject focusing;
    private FirstPersonMoving firstPersonStar;
    private final FxApp fxApp;
    //    private final Set<Spatial> eachFrameErase = new HashSet<>();
    private AmbientLight ambientLight;
    protected FilterPostProcessor filterPostProcessor;
    private RefFrame refFrame = RefFrame.STATIC;

    protected SpawningObject spawning;

    public static JmeApp getInstance() {
        return instance;
    }

    public JmeApp(FxApp fxApp) {
        this.fxApp = fxApp;
    }

    public FxApp getFxApp() {
        return fxApp;
    }

    @Override
    public void simpleInitApp() {
        instance = this;

        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
//        font = assetManager.loadFont("com/trashsoftware/gravity2/fonts/calibri12.fnt");

        setupMouses();

        filterPostProcessor = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(filterPostProcessor);

        Simulator sim = initializeSimulator();
        setSimulator(sim);

        initLights();
        initMarks();

        setCamera3rdPerson();
        updateLabelShowing();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (playing) {
            int nPhysicalFrames = Math.round(tpf * 1000);
            Simulator.SimResult sr = simulator.simulate(nPhysicalFrames, false);
            if (sr == Simulator.SimResult.NUM_CHANGED) {
                reloadObjects();
                getFxApp().notifyObjectCountChanged(simulator);
            } else if (sr == Simulator.SimResult.TOO_FAST) {
                getFxApp().getControlBar().speedDownAction();
                reloadObjects();
                getFxApp().notifyObjectCountChanged(simulator);
            }

            updateRefFrame();
            if (firstPersonStar != null) {

            } else if (focusing != null) {
                moveScreenWithFocus();
            }
        }

        if (firstPersonStar != null) {
            if (keyWPressed) {
                firstPersonStar.moveForward(3000);
            }
            if (keyUpPressed) {
                firstPersonStar.moveUp(1e3);
            } else if (keyDownPressed) {
                firstPersonStar.moveDown(1e3);
            }
            moveCameraWithFirstPerson();
            updateCompass();
        }

        if (spawning != null) {
            updateSpawningPosition();
            computeSpawningMaster();
            drawSpawningConnection();
        }

        updateModelPositions();
        updateAxisMarks();

        clearUnUsedMeshes();
        if (showFullPath) {
            drawFullPaths();
        }
        if (showOrbit) {
            drawOrbits();
        }
        if (showTrace) {
            drawRecentPaths();
        }
        if (showBarycenter) {
            updateBarycentersNodes();
        }
//        drawNameTexts();
//        System.out.println(tpf * 1000);
    }

    private void initLights() {
        ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White.mult(0.02f));
        rootNode.addLight(ambientLight);
        rootNode.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    private void initMarks() {
        axisMarkNode = createHalfCrossAt(
                "AxisMark",
                Vector3f.ZERO,
                0.01f);

        int screenWidth = settings.getWidth();
        int screenHeight = settings.getHeight();

        compassNode = new CompassNode(this);
        compassNode.setLocalTranslation(100, screenHeight - 100, 0);

        lonLatTextNode = new GuiTextNode(this);
        lonLatTextNode.setLocalTranslation(20, screenHeight - 200, 0);

        gridPlaneNode = new GridPlane(this);
        gridPlaneNode.setLocalTranslation(0, 0, 0);
    }

    private void updateAxisMarks() {
        // Get the screen width and height
        int screenWidth = settings.getWidth();
        int screenHeight = settings.getHeight();

        // Set the 2D screen position (top-left corner)
        Vector2f screenPosition = new Vector2f(100, screenHeight - 100);

        // Convert screen position to 3D world position near the camera
        Vector3f worldPosition = cam.getWorldCoordinates(screenPosition, 0.1f); // Slightly in front of the camera

        axisMarkNode.setLocalTranslation(worldPosition);

//        // Rotate the axis marker to always face the camera
//        axisMarkNode.lookAt(cam.getLocation(), worldUp);
    }

    private void updateCompass() {
        if (firstPersonStar != null) {
            double deg = -FirstPersonMoving.compassAzimuthToGame(firstPersonStar.compassAzimuth + 90);
            float rad = FastMath.DEG_TO_RAD * (float) deg;
            compassNode.setLocalRotation(new Quaternion().fromAngleAxis(rad, Vector3f.UNIT_Z));

            UnitsConverter uc = getFxApp().getUnitConverter();

            String lon = uc.longitude(firstPersonStar.getGeologicalLongitude());
            String lat = uc.latitude(firstPersonStar.getLatitude());

            lonLatTextNode.setText(String.format("""
                            Longitude: %s
                            Latitude: %s
                            Altitude: %s
                            FOV: %s""",
                    lon, lat, uc.distance(firstPersonStar.getAltitude()),
                    uc.angleDegreeDecimal(cam.getFov())));
        }
    }

    private void clearUnUsedMeshes() {
        for (Spatial spatial : rootNode.getChildren()) {
            if (spatial instanceof Geometry geometry) {
                if (!showFullPath) {
                    if (spatial.getName().startsWith("Path")) {
                        geometry.setMesh(ObjectModel.blank);
                    }
                }
                if (!showTrace) {
                    if (spatial.getName().startsWith("Trace")) {
                        geometry.setMesh(ObjectModel.blank);
                    }
                }
                if (!showOrbit) {
                    if (spatial.getName().startsWith("Orbit")) {
                        geometry.setMesh(ObjectModel.blank);
                    }
                }
//                if (!showBarycenter) {
//                    if (spatial.getName().startsWith("#Barycenter")) {
//                        geometry.setMesh(ObjectModel.blank);
//                    }
//                }
            }
        }
    }

    private Simulator initializeSimulator() {
        simulator = new Simulator();

//        simpleTest();
//        simpleTest2();
//        simpleTest3();
//        simpleTest4();
//        saturnRingTest();
//        rocheEffectTest();
        solarSystemTest();
//        solarSystemWithCometsTest();
//        smallSolarSystemTest();
//        tidalTest();
//        ellipseClusterTest();
//        subStarTest();
//        chaosSolarSystemTest();
//        twoChaosSolarSystemTest();
//        twoChaosSystemTest();
//        threeBodyTest();

        getFxApp().notifyObjectCountChanged(simulator);

        return simulator;
    }

    private void detachObjectModel(ObjectModel om) {
        // died object
        rootNode.detachChild(om.objectNode);
        if (om.orbitNode != null) {
            rootNode.detachChild(om.orbitNode);
        }
        if (om.barycenterMark != null) {
            rootNode.detachChild(om.barycenterMark);
        }
        om.setShowApPe(false);

        // left its paths continues alive
        if (om.emissionLight != null) {
            om.removeEmissionLight();
        }
    }

    void reloadObjects() {
        List<CelestialObject> objects = simulator.getObjects();
        Set<CelestialObject> objectSet = new HashSet<>(objects);

        // garbage collect for those destroyed things
        for (var iterator = modelMap.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            if (!objectSet.contains(entry.getKey())) {
                ObjectModel om = entry.getValue();

                // transfer it into died objects
                iterator.remove();
                diedObjects.put(entry.getKey(), om);

                detachObjectModel(om);
            }
        }

        for (CelestialObject object : objects) {
            ObjectModel om = modelMap.get(object);
            if (om == null) {
                om = new ObjectModel(object, this);
                System.out.println("Creating model for " + object.getName());
                modelMap.put(object, om);
                rootNode.attachChild(om.objectNode);

                // Initialize the geometry for the curve (we'll reuse this each frame)
//                rootNode.attachChild(om.path);
//                rootNode.attachChild(om.orbit);
//                rootNode.attachChild(om.trace);

                // Synchronize the global label showing status to the new object
                om.setShowLabel(showLabel);
            }
            om.notifyObjectChanged();
        }

        if (spawning != null) {
            rootNode.attachChild(spawning.model.objectNode);
            rootNode.attachChild(spawning.model.orbitNode);
        }

        updateCurvesShowing();
        updateModelPositions();
    }

    void updateModelPositions() {
        for (CelestialObject object : simulator.getObjects()) {
            ObjectModel objectModel = modelMap.get(object);
            if (objectModel == null) throw new RuntimeException(object.getName());
            objectModel.updateModelPosition(
                    scale
            );
        }
    }

    private double get1stPersonDefaultScale() {
        double totalRadius = 0;
        for (CelestialObject co : simulator.getObjects()) {
            totalRadius += co.getEquatorialRadius();
        }
        double avgRadius = totalRadius / simulator.getObjects().size();
        return 100.0 / avgRadius;
    }

    /**
     * @return a good scale of closely viewing the object
     */
    private double get3rdPersonObjectViewScale(CelestialObject object) {
        double radius = object.getEquatorialRadius();
        return 30.0 / radius;
    }

    private void adjustFov(float delta) {
        float fov = cam.getFov();
        float newFov = FastMath.clamp(fov + delta, 5, 175f);

        cam.setFrustumPerspective(newFov,
                (float) cam.getWidth() / cam.getHeight(),
                cam.getFrustumNear(),
                cam.getFrustumFar());
    }

    private void setCamera1stPerson() {
        cam.setFrustumPerspective(45f,
                (float) cam.getWidth() / cam.getHeight(),
                0.1f,
                1e8f);

        rootNode.detachChild(axisMarkNode);
        rootNode.detachChild(gridPlaneNode);
        guiNode.attachChild(compassNode);
        guiNode.attachChild(lonLatTextNode);
    }

    private void setCamera3rdPerson() {
        cam.setFrustumPerspective(45f,
                (float) cam.getWidth() / cam.getHeight(),
                0.1f,
                1e6f);

        rootNode.attachChild(axisMarkNode);
        rootNode.attachChild(gridPlaneNode);
        guiNode.detachChild(compassNode);
        guiNode.detachChild(lonLatTextNode);
    }

    private void setupMouses() {
//        cam.setFrustumNear(1f);
//        cam.setFrustumFar(1e7f);

        cam.setLocation(new Vector3f(0, 0, 100));

        // Disable flyCam
//        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true); // Show the mouse cursor

        inputManager.addMapping("KeyW", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("KeyUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("KeyDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(actionListener, "KeyW", "KeyUp", "KeyDown");

        // Map the mouse buttons
        inputManager.addMapping("LeftClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("RightClick", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("MiddleClick", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        // Add listeners for mouse clicks
        inputManager.addListener(actionListener, "LeftClick", "RightClick", "MiddleClick");

        // Map mouse drag motions for both directions on the X-axis and Y-axis
        inputManager.addMapping("MouseMoveX+", new MouseAxisTrigger(MouseInput.AXIS_X, false)); // Rightward
        inputManager.addMapping("MouseMoveX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));  // Leftward
        inputManager.addMapping("MouseMoveY+", new MouseAxisTrigger(MouseInput.AXIS_Y, false)); // Downward
        inputManager.addMapping("MouseMoveY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));  // Upward
//        inputManager.addMapping("KeyW", new KeyP(KeyInput.KEY_W));

        // Add listeners for the new mappings
        inputManager.addListener(analogListener, "MouseMoveX+", "MouseMoveX-", "MouseMoveY+", "MouseMoveY-");

        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false)); // Zoom in (scroll up)
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true)); // Zoom out (scroll down)

        // Add listeners for zooming
        inputManager.addListener(analogListener, "ZoomIn", "ZoomOut");
    }

    public void performAction(String name, boolean isPressed, float tpf) {
        if (name.equals("LeftClick")) {
            boolean wasDragging = leftButton.dragging;
            leftButton.press(isPressed);
            if (isPressed) {

            } else {
                if (wasDragging) {

                } else if (spawning != null) {
                    spawn();
                } else {
                    // Reset results list.
                    CollisionResults results = new CollisionResults();
                    // Convert screen click to 3d position
                    Vector2f click2d = inputManager.getCursorPosition();
                    Vector3f click3d = cam.getWorldCoordinates(click2d, 0f).clone();

                    // Convert screen click to 3D position at far plane
                    Vector3f farPoint = cam.getWorldCoordinates(click2d, 1f);  // Position on far plane

                    // Calculate the direction vector from near plane to far plane
                    Vector3f dir = farPoint.subtract(click3d).normalizeLocal();  // Ensure it's a valid direction

                    // Aim the ray from the clicked spot forwards.
                    Ray ray = new Ray(click3d, dir);
                    rootNode.collideWith(ray, results);

                    // Check if there's a hit
                    if (results.size() > 0) {
                        // Get the closest collision result
                        Geometry target = results.getClosestCollision().getGeometry();
                        if (target != null) {
                            // Handle the click on the geometry
                            onGeometryClicked(target);
                        }
                    }
                }
            }
        } else if (name.equals("RightClick")) {
            boolean wasDragging = rightButton.dragging;
            rightButton.press(isPressed);
            if (isPressed) {

            } else {
                if (spawning != null && !wasDragging) {
                    exitSpawningMode();
                }
            }
        } else if (name.equals("MiddleClick")) {
            midButton.press(isPressed);
        } else if (name.equals("KeyW")) {
            keyWPressed = isPressed;
        } else if (name.equals("KeyUp")) {
            keyUpPressed = isPressed;
        } else if (name.equals("KeyDown")) {
            keyDownPressed = isPressed;
        }
    }

    // Action listener to track button press/release
    private final ActionListener actionListener = this::performAction;

    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (name.startsWith("MouseMove")) {
                if (leftButton.pressed) leftButton.updateDragging();
                if (rightButton.pressed) rightButton.updateDragging();
                if (midButton.pressed) midButton.updateDragging();
            }

            if (leftButton.pressed) {
                if (firstPersonStar == null) {
                    if (name.equals("MouseMoveX-")) {
                        // Move the camera horizontally when dragging the left button
                        Vector3f left = cam.getLeft().mult(-horizontalSpeed * value);
                        screenCenter.addLocal(left);
                        if (focusing != null) centerRelToFocus.addLocal(left);
                    } else if (name.equals("MouseMoveX+")) {
                        // Move the camera horizontally when dragging the left button
                        Vector3f left = cam.getLeft().mult(horizontalSpeed * value);
                        screenCenter.addLocal(left);
                        if (focusing != null) centerRelToFocus.addLocal(left);
                    } else if (name.equals("MouseMoveY-")) {
                        // Move the camera vertically when dragging the left button
                        Vector3f up = cam.getUp().mult(verticalSpeed * value);
                        screenCenter.addLocal(up);
                        if (focusing != null) centerRelToFocus.addLocal(up);
                    } else if (name.equals("MouseMoveY+")) {
                        // Move the camera vertically when dragging the left button
                        Vector3f up = cam.getUp().mult(-verticalSpeed * value);
                        screenCenter.addLocal(up);
                        if (focusing != null) centerRelToFocus.addLocal(up);
                    }
//                    cam.lookAt(lookAtPoint, worldUp);
                } else {
                    if (name.equals("MouseMoveX-")) {
                        // Move the camera horizontally when dragging the left button
                        firstPersonStar.azimuthChange(-value * azimuthSensitivity);
                    } else if (name.equals("MouseMoveX+")) {
                        // Move the camera horizontally when dragging the left button
                        firstPersonStar.azimuthChange(value * azimuthSensitivity);
                    } else if (name.equals("MouseMoveY-")) {
                        // Move the camera vertically when dragging the left button
                        firstPersonStar.lookingAltitudeChange(value * altitudeAngleSensitivity);
                    } else if (name.equals("MouseMoveY+")) {
                        // Move the camera vertically when dragging the left button
                        firstPersonStar.lookingAltitudeChange(-value * altitudeAngleSensitivity);
                    }
//                    Vector3f sightPos = firstPersonStar.getSightLocalPos();
//                    firstPersonStar.sightPoint.setLocalTranslation(sightPos);
                }
            }
            if (rightButton.pressed) {
                if (firstPersonStar == null) {
                    float rotationAmount = rotationSpeed * value;
//                System.out.println(cam.getLeft());
                    if (name.equals("MouseMoveY-")) {
                        rotateAroundPivot(rotationAmount, cam.getLeft());
                    } else if (name.equals("MouseMoveY+")) {
                        rotateAroundPivot(-rotationAmount, cam.getLeft());
                    }
                } else {

                }
            }
            if (midButton.pressed) {
                if (firstPersonStar == null) {
                    // Rotate the camera around the object when dragging the middle button
                    float rotationAmount = rotationSpeed * value;
                    if (name.equals("MouseMoveX-")) {
                        rotateAroundPivot(rotationAmount, Vector3f.UNIT_Z);
                    } else if (name.equals("MouseMoveX+")) {
                        rotateAroundPivot(-rotationAmount, Vector3f.UNIT_Z);
                    }
                } else {

                }
            }

            // Handle zooming
            if (firstPersonStar == null) {
                if (name.equals("ZoomIn")) {
                    zoomInAction();
                } else if (name.equals("ZoomOut")) {
                    zoomOutAction();
                }
            } else {
                if (name.equals("ZoomIn")) {
                    adjustFov(-5f);
                } else if (name.equals("ZoomOut")) {
                    adjustFov(5f);
                }
            }
        }
    };

    private void updateSpawningPosition() {
        // Get the 2D mouse coordinates
        Vector2f mouseCoords = inputManager.getCursorPosition();

        // Convert the 2D mouse position into a 3D ray
        Vector3f origin = cam.getWorldCoordinates(mouseCoords, 0f);  // Near plane
        Vector3f direction = cam.getWorldCoordinates(mouseCoords, 1f).subtractLocal(origin).normalizeLocal();  // Far plane
        Ray ray = new Ray(origin, direction);

        Plane plane = getSpawningPlane();

        // Intersect the ray with the defined plane
        Vector3f intersection = new Vector3f();
        if (ray.intersectsWherePlane(plane, intersection)) {
            // Move the sphere to the intersection point
            spawning.object.setPosition(realPosition(intersection));
            spawning.model.updateModelPosition(scale);
        }
    }

    private Plane getSpawningPlane() {
        CelestialObject master = spawning.getSpawnRelative();
        if (master == null) {
            return new Plane(Vector3f.UNIT_Z, 0);
        } else {
            // todo: equatorial plane or ecliptic plane
            Vector3f planeNormal = GuiUtils.fromDoubleArray(spawning.planeNormal).normalizeLocal();
            Vector3f point = panePosition(master.getPosition());

            gridPlaneNode.showAt(planeNormal, point, 20, 20, 20.0f);
            float constant = -planeNormal.dot(point);
            return new Plane(planeNormal, constant);
        }
    }

    private void spawn() {
        simulator.addObject(spawning.object);

        CelestialObject master = spawning.object.getHillMaster();
        if (master != null) {
            double speed = spawning.orbitSpeed;
            double[] velocity = simulator.computeVelocityOfN(master, spawning.object, speed,
                    spawning.planeNormal);
            spawning.object.setVelocity(velocity);
        }

        exitSpawningMode();
        reloadObjects();
        getFxApp().notifyObjectCountChanged(simulator);
    }

    private void rotateAroundPivot(float amount, Vector3f axis) {
        // Create a quaternion for rotation based on the given axis and amount
        Quaternion rotation = new Quaternion().fromAngleAxis(amount, axis);

        // Rotate the camera's direction vector and up vector using the quaternion
        Vector3f direction = cam.getLocation().subtract(lookAtPoint).normalizeLocal();
        direction = rotation.mult(direction);

        // Apply the rotation to the camera's up vector to maintain orientation
        Vector3f upVector = rotation.mult(cam.getUp());

        // Calculate the new camera position based on the rotated direction
        Vector3f newCamPos = lookAtPoint.add(direction.mult(cam.getLocation().distance(lookAtPoint)));

        // Set the new camera location and update its orientation
        cam.setLocation(newCamPos);
        cam.lookAt(lookAtPoint, upVector);
    }

    private void scaleScene(float scaleFactor) {
        scale = scale * scaleFactor;

        screenCenter.multLocal(scaleFactor);

        updateLabelShowing();
        updateCurvesShowing();
    }

    public void zoomInAction() {
        scaleScene(1.25f);
    }

    public void zoomOutAction() {
        scaleScene(0.8f);
    }

    // Method to handle the geometry click event
    private void onGeometryClicked(Geometry geom) {
        System.out.println("Clicked on: " + geom.getName() + ", " + geom.getClass());

        for (ObjectModel objectModel : modelMap.values()) {
            CelestialObject object = objectModel.object;
            if (object.isExist()) {
                if (object.getName().equals(geom.getName())) {
                    focusOn(object, true);
                    break;
                }
                BitmapText bt = (BitmapText) objectModel.labelNode.getChildren().get(0);
                if (geom == bt.getChildren().get(0)) {
                    focusOn(object, true);
                    break;
                }
            }
        }
    }

    public void landOn(CelestialObject object) {
        System.out.println("Landing on " + object.getName());

        enqueue(() -> {
            ObjectModel om = modelMap.get(object);

            System.out.println("Scale: " + scale);
            double targetScale = get1stPersonDefaultScale();
            scale = targetScale;
//            double factor = targetScale / scale;
//            scaleScene((float) factor);
            System.out.println("New scale: " + scale);

            setCamera1stPerson();

            if (om.firstPersonMoving == null) {
                om.firstPersonMoving = new FirstPersonMoving(om, 3e5);
            }
            firstPersonStar = om.firstPersonMoving;

            om.rotatingNode.attachChild(firstPersonStar.cameraNode);
            om.rotatingNode.attachChild(firstPersonStar.northNode);

            firstPersonStar.updateCamera(cam);

            getFxApp().getControlBar().setLand();

            if (focusing != null) {
                getFxApp().getControlBar().clearFocusAction();
            }
        });
    }

    public void focusOn(CelestialObject object, boolean scrollToFocus) {
        enqueue(() -> {
            System.out.println("Focused on " + object.getName());

            focusing = object;
            double focusingLastX = focusing.getX() - refOffsetX;
            double focusingLastY = focusing.getY() - refOffsetY;
            double focusingLastZ = focusing.getZ() - refOffsetZ;

            centerRelToFocus.set(0, 0, 0);

            screenCenter.setX(focusingLastX * scale);
            screenCenter.setY(focusingLastY * scale);
            screenCenter.setZ(focusingLastZ * scale);

            updateCurvesShowing();

            getFxApp().getControlBar().setFocus(focusing, scrollToFocus);
        });

    }

    private void moveScreenWithFocus() {
        double focusingLastX = focusing.getX() - refOffsetX;
        double focusingLastY = focusing.getY() - refOffsetY;
        double focusingLastZ = focusing.getZ() - refOffsetZ;

        screenCenter.setX((focusingLastX * scale) + centerRelToFocus.x);
        screenCenter.setY((focusingLastY * scale) + centerRelToFocus.y);
        screenCenter.setZ((focusingLastZ * scale) + centerRelToFocus.z);

//        System.out.println(screenCenter);
//        cam.setLocation(cam.getLocation().add(delta));
//        lookAtPoint.addLocal(delta);
    }

    private void moveCameraWithFirstPerson() {
        firstPersonStar.updateCamera(cam);
//        Vector3f camPos = firstPersonStar.cameraNode.getWorldTranslation();
//        System.out.println(camPos);
//        cam.setLocation(camPos);
//        cam.lookAtDirection(firstPersonStar.getLookingDirection(cam), 
//                firstPersonStar.getUpVector());

//        Vector3f centerPos = firstPersonStar.objectModel.rotatingNode.getWorldTranslation();
//        Vector3f realUp = camPos.subtract(centerPos).normalize();

//        cam.lookAt(firstPersonStar.sightPoint.getWorldTranslation(), firstPersonStar.getUpVector());
    }

//    private void moveCameraWithLookAtPoint(Vector3f oldLookAt, Vector3f newLookAt) {
//        float dt = cam.getLocation().distance(oldLookAt);
//
//        Vector3f direction = cam.getDirection();
//        Vector3f newLocation = newLookAt.subtract(direction.mult(dt));
//        cam.setLocation(newLocation);
//        cam.lookAt(newLookAt, worldUp);
//    }

    private void computeSpawningMaster() {
        HieraticalSystem hillMaster = simulator.findMostProbableHillMaster(spawning.object.getPosition());
        if (hillMaster != null) {
            CelestialObject dominant = hillMaster.master;

            if (dominant != null && dominant.getMass() > spawning.object.getMass() * Simulator.PLANET_MAX_MASS) {
                spawning.object.setHillMaster(dominant);
                if (focusing != null) {
                    spawning.spawnRelative = focusing;
                    spawning.planeNormal = focusing.getRotationAxis();
                } else {
                    spawning.spawnRelative = null;
                    spawning.planeNormal = dominant.getRotationAxis();
                }
            }
        }

        CelestialObject gravityMaster = simulator.computeGravityMaster(spawning.object);
        if (gravityMaster != null) {
            // this does not consider the mass.
            // but gravityMaster will be wiped once the spawning is placed
            spawning.object.setGravityMaster(gravityMaster);
        }
    }

    public float paneX(double realX) {
        return (float) ((realX - refOffsetX) * scale - screenCenter.x);
    }

    public float paneY(double realY) {
        return (float) ((realY - refOffsetY) * scale - screenCenter.y);
    }

    public float paneZ(double realZ) {
        return (float) ((realZ - refOffsetZ) * scale - screenCenter.z);
    }

    public double realXFromPane(float paneX) {
        return (paneX + screenCenter.x) / scale + refOffsetX;
    }

    public double realYFromPane(float paneY) {
        return (paneY + screenCenter.y) / scale + refOffsetY;
    }

    public double realZFromPane(float paneZ) {
        return (paneZ + screenCenter.z) / scale + refOffsetZ;
    }

    public Vector3f panePosition(double[] realPos) {
        if (realPos.length != 3) throw new IndexOutOfBoundsException();
        return new Vector3f(
                paneX(realPos[0]),
                paneY(realPos[1]),
                paneZ(realPos[2])
        );
    }

    public double[] realPosition(Vector3f panePos) {
        return new double[]{
                realXFromPane(panePos.x),
                realYFromPane(panePos.y),
                realZFromPane(panePos.z)
        };
    }

    private void updateRefFrame() {
        RefFrame refFrame = getRefFrame();
        if (firstPersonStar != null) {
            double[] pos = firstPersonStar.getObject().getPosition();
            refOffsetX = pos[0];
            refOffsetY = pos[1];
            refOffsetZ = pos[2];
        } else {
            if (refFrame == RefFrame.SYSTEM) {
                double[] barycenter = simulator.barycenter();
                refOffsetX = barycenter[0];
                refOffsetY = barycenter[1];
                refOffsetZ = barycenter[2];
            } else if (refFrame == RefFrame.TARGET) {
                if (focusing != null) {
                    double[] pos = focusing.getPosition();
                    refOffsetX = pos[0];
                    refOffsetY = pos[1];
                    refOffsetZ = pos[2];
                }
            }
        }
    }

    private void updateBarycentersNodes() {
        List<HieraticalSystem> roots = simulator.getRootSystems();
        for (HieraticalSystem hs : roots) {
            updateBarycenterNode(hs, 2);
        }
        if (globalBarycenterNode != null) {
            double[] barycenter = simulator.barycenter();
            Vector3f scenePos = panePosition(barycenter);
            globalBarycenterNode.setLocalTranslation(scenePos);
        }
    }

    private void updateBarycenterNode(HieraticalSystem hs, float markSize) {
        if (!hs.isObject()) {
            double[] barycenter = hs.getPosition();
            Vector3f scenePos = panePosition(barycenter);

            ObjectModel om = modelMap.get(hs.master);
            if (om.barycenterMark == null) {
                om.barycenterMark = createFullCrossAt(
                        "#Barycenter" + om.object.getName(),
                        Vector3f.ZERO, markSize,
                        ColorRGBA.White);
                rootNode.attachChild(om.barycenterMark);
//                System.err.println("System " + hs.master.getName() + " does not have valid barycenter mark");
            }
            om.barycenterMark.setLocalTranslation(scenePos);
            for (HieraticalSystem child : hs.getChildrenSorted()) {
                updateBarycenterNode(child, markSize - 0.5f);
            }
        } else {
            ObjectModel om = modelMap.get(hs.master);
            if (om.barycenterMark != null) {
                rootNode.detachChild(om.barycenterMark);
            }
        }
    }

    private void disableBarycenters() {
        for (ObjectModel om : modelMap.values()) {
            if (om.barycenterMark != null) {
                rootNode.detachChild(om.barycenterMark);
            }
        }
        if (globalBarycenterNode != null) {
            rootNode.detachChild(globalBarycenterNode);
        }
    }

    private void enableBarycenters() {
        List<HieraticalSystem> roots = simulator.getRootSystems();
        for (HieraticalSystem hs : roots) {
            enableBarycenterNodes(hs, 2);
        }

        // overall barycenter
        if (roots.size() > 1) {
            double[] barycenter = simulator.barycenter();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);

            if (globalBarycenterNode == null) {
                globalBarycenterNode = createFullCrossAt(
                        "#BarycenterGlobal",
                        Vector3f.ZERO, 3, ColorRGBA.Yellow);
            }
            globalBarycenterNode.setLocalTranslation(x, y, z);
            rootNode.attachChild(globalBarycenterNode);
        }
    }

    private void enableBarycenterNodes(HieraticalSystem hs, double markSize) {
        if (!hs.isObject()) {
            double[] barycenter = hs.getPosition();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);

            ObjectModel om = modelMap.get(hs.master);
            if (om.barycenterMark == null) {
                om.barycenterMark = createFullCrossAt(
                        "#Barycenter" + om.object.getName(),
                        Vector3f.ZERO, (float) markSize,
                        ColorRGBA.White);
                om.barycenterMark.setLocalTranslation(x, y, z);
            }

            rootNode.attachChild(om.barycenterMark);

            for (HieraticalSystem child : hs.getChildrenSorted()) {
                enableBarycenterNodes(child, Math.max(0.5, markSize - 0.5));
            }
        }
    }

    public Node createFullCrossAt(String name, Vector3f center, float length, ColorRGBA color) {
        Node crossNode = new Node(name);

        // X-axis lines
        crossNode.attachChild(createLine(center.subtract(new Vector3f(length, 0, 0)), center.add(new Vector3f(length, 0, 0)), color));

        // Y-axis lines
        crossNode.attachChild(createLine(center.subtract(new Vector3f(0, length, 0)), center.add(new Vector3f(0, length, 0)), color));

        // Z-axis lines
        crossNode.attachChild(createLine(center.subtract(new Vector3f(0, 0, length)), center.add(new Vector3f(0, 0, length)), color));

        return crossNode;
    }

    public Node createHalfCrossAt(String name, Vector3f center, float length) {
        Node crossNode = new Node(name);
        ColorRGBA xColor, yColor, zColor;

        xColor = ColorRGBA.Red;
        yColor = ColorRGBA.Green;
        zColor = ColorRGBA.Blue;


        // X-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(length, 0, 0)), xColor));

        // Y-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, length, 0)), yColor));

        // Z-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, 0, length)), zColor));

        return crossNode;
    }

    private Geometry createLine(Vector3f start, Vector3f end, ColorRGBA color) {
        Mesh mesh = new Mesh();

        // Set positions for the vertices
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(start, end));
        mesh.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1});
        mesh.updateBound();

        Geometry geom = new Geometry("Line", mesh);

        // Create a material for the line
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);

        return geom;
    }

    private void drawSpawningConnection() {
        if (spawning != null) {
            CelestialObject hillMaster = spawning.object.getHillMaster();
            CelestialObject gravityMaster = spawning.object.getGravityMaster();

            Vector3f selfPos = panePosition(spawning.object.getPosition());

            if (hillMaster != null) {
                double realDt = VectorOperations.distance(spawning.object.getPosition(),
                        hillMaster.getPosition());
                String dtText = getFxApp().getUnitConverter().distance(realDt);

                Vector3f masterPos = panePosition(hillMaster.getPosition());
                spawning.primaryLine.show(selfPos, masterPos, dtText);
            } else {
                spawning.primaryLine.hide();
            }

            if (gravityMaster != null) {
                double realDt = VectorOperations.distance(spawning.object.getPosition(),
                        gravityMaster.getPosition());
                String dtText = getFxApp().getUnitConverter().distance(realDt);

                Vector3f masterPos = panePosition(gravityMaster.getPosition());
                spawning.secondaryLine.show(selfPos, masterPos, dtText);
            } else {
                spawning.secondaryLine.hide();
            }
        }
    }

    private void drawOrbits() {
        for (CelestialObject object : simulator.getObjects()) {
            if (object.getMass() >= minimumMassShowing) {
                drawOrbitOf(object);
            }
        }
        if (spawning != null) {
            drawSpawningOrbit();
        }
    }

    private void drawSpawningOrbit() {
        CelestialObject parent = spawning.object.getHillMaster();
        if (parent != null && parent.getMass() > spawning.object.getMass() * Simulator.PLANET_MAX_MASS) {
//            HieraticalSystem parentSystem = simulator.getHieraticalSystem(parent);
            AbstractObject child = spawning.object;

            double[] barycenter = OrbitCalculator.calculateBarycenter(parent, child);

            // velocity relative to parent system's barycenter movement
            double[] velocity = simulator.computeVelocityOfN(parent, child, spawning.orbitSpeed,
                    spawning.planeNormal);
            velocity = VectorOperations.subtract(velocity, parent.getVelocity());

            double[] position = VectorOperations.subtract(child.getPosition(),
                    parent.getPosition());
            OrbitalElements specs = OrbitCalculator.computeOrbitSpecs3d(position,
                    velocity,
                    child.getMass() + parent.getMass(),
                    simulator.getG());

            if (specs.isElliptical()) {
                drawEllipticalOrbit(spawning.model, barycenter, specs);
            } else {
                if (eclipticOrbitOnly) {
                    spawning.model.orbit.setMesh(ObjectModel.blank);
                } else {
                    drawHyperbolicOrbit(spawning.model, barycenter, specs);
                }
            }
        }
    }

    private void drawOrbitOf(CelestialObject object) {
        CelestialObject parent = object.getHillMaster();
        if (parent != null && parent.getMass() > object.getMass() * Simulator.PLANET_MAX_MASS) {
            HieraticalSystem parentSystem = simulator.getHieraticalSystem(parent);

            AbstractObject child;
            if (true) {
                child = simulator.getHieraticalSystem(object);
            } else {
                child = object;
            }

            double[] barycenter = OrbitCalculator.calculateBarycenter(parent, child);

            // velocity relative to parent system's barycenter movement
            double[] velocity = VectorOperations.subtract(child.getVelocity(),
                    parentSystem.getVelocity());
            double[] position = VectorOperations.subtract(child.getPosition(),
                    barycenter);
            OrbitalElements specs = OrbitCalculator.computeOrbitSpecs3d(position,
                    velocity,
                    child.getMass() + parent.getMass(),
                    simulator.getG());

            ObjectModel om = modelMap.get(object);
            if (specs.isElliptical()) {
                drawEllipticalOrbit(om, barycenter, specs);
            } else {
                if (eclipticOrbitOnly) {
                    om.orbit.setMesh(ObjectModel.blank);
                } else {
                    drawHyperbolicOrbit(om, barycenter, specs);
                }
            }
        }
    }

    private void drawEllipticalOrbit(ObjectModel om, double[] barycenter, OrbitalElements oe) {
        om.showEllipticOrbit(barycenter, oe, 360);
    }

    private void drawHyperbolicOrbit(ObjectModel om, double[] barycenter, OrbitalElements oe) {
        om.showHyperbolicOrbit(barycenter,
                oe,
                360);
    }

    private void drawRecentPaths() {
        double visPathLength = pathLength * speed;
        final double now = simulator.getTimeStepAccumulator();
        final double earliest = now - visPathLength;

        RefFrame refFrame = getRefFrame();

        int ti = simulator.getDimension();  // time index
        double[] temp;
        double[] offset = new double[3];

        for (Map.Entry<CelestialObject, Deque<double[]>> entry : simulator.getRecentPaths().entrySet()) {
            var obj = entry.getKey();
            if (obj.getMass() < minimumMassShowing) continue;
            ObjectModel om = getObjectModel(obj);
            if (om == null) continue;

            var path = entry.getValue();
            Iterator<double[]> centerPath = switch (refFrame) {
                case SYSTEM -> simulator.getBarycenterPath().iterator();
                case TARGET ->
                        focusing == null ? null : simulator.getRecentPaths().get(focusing).iterator();
                default -> null;
            };

            double pointInterval = speed * Simulator.PATH_INTERVAL;

            double lastPointT = Double.MAX_VALUE;

            int numPoints = (int) (visPathLength / pointInterval);
            Vector3f[] vertices = new Vector3f[numPoints];
            ColorRGBA[] colors = new ColorRGBA[numPoints];

//            double[] last = obj.getPosition();
//            double[] lastOffset = new double[3];
            temp = new double[3];

            int index = 0;
            for (double[] pos : path) {
                if (pos[ti] < earliest) {
                    break;
                }
                double tsa = pos[ti];

                if (centerPath != null) {
                    if (!centerPath.hasNext()) {
                        break;  // center not alive for this long
                    }
                    temp = centerPath.next();
                }

                if (lastPointT - tsa >= pointInterval) {
                    lastPointT = tsa;
                } else {
                    continue;
                }

                if (centerPath != null) {
                    offset[0] = temp[0] - refOffsetX;
                    offset[1] = temp[1] - refOffsetY;
                    offset[2] = temp[2] - refOffsetZ;
                }

                if (index == numPoints) break;

                vertices[index] = new Vector3f(
                        paneX(pos[0] - offset[0]),
                        paneY(pos[1] - offset[1]),
                        paneZ(pos[2] - offset[2])
                );

                float begin = 0.25f;
                float interpolate = (float) index / numPoints * (1 - begin) + begin;
//                System.out.println(interpolate + " " + index + " " + numPoints);
                ColorRGBA color = om.color.clone();
                colors[index] = color.interpolateLocal(backgroundColor,
                        interpolate);

                index++;
            }

            if (index < numPoints) {
                vertices = Arrays.copyOf(vertices, index);
                colors = Arrays.copyOf(colors, index);
            }

            // Create the mesh for the curve
            Mesh mesh = om.trace.getMesh();
            if (mesh == null || mesh == ObjectModel.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                om.trace.setMesh(mesh);
            }

            // Set the vertices
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));

            // Set the vertex colors
            mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));

            // Update the mesh to generate it
            mesh.updateBound();
            mesh.updateCounts();
        }
    }

    private void drawFullPaths() {
//        tempGeom.clear();
        RefFrame refFrame = getRefFrame();
        double[] temp;
        double[] offset = new double[3];
        for (Map.Entry<CelestialObject, Deque<double[]>> entry : simulator.getRecentPaths().entrySet()) {
            var obj = entry.getKey();
            if (obj.getMass() < minimumMassShowing) continue;
            var path = entry.getValue();
            ObjectModel om = getObjectModel(obj);
            if (om == null) continue;

            Iterator<double[]> centerPath = switch (refFrame) {
                case SYSTEM -> simulator.getBarycenterPath().iterator();
                case TARGET ->
                        focusing == null ? null : simulator.getRecentPaths().get(focusing).iterator();
                default -> null;
            };

            temp = new double[3];

            Vector3f[] vertices = new Vector3f[path.size()];
            int index = 0;
            for (double[] pos : path) {
                if (centerPath != null) {
                    if (!centerPath.hasNext()) {
                        break;  // center not alive for this long
                    }
                    temp = centerPath.next();
                }

                if (centerPath != null) {
                    offset[0] = temp[0] - refOffsetX;
                    offset[1] = temp[1] - refOffsetY;
                    offset[2] = temp[2] - refOffsetZ;
                }

                Vector3f vector3f = new Vector3f(
                        paneX(pos[0] - offset[0]),
                        paneY(pos[1] - offset[1]),
                        paneZ(pos[2] - offset[2])
                );
                vertices[index] = vector3f;
                index++;
            }

            drawPolyLine(vertices, om);
        }
    }

    private void drawPolyLine(Vector3f[] vertices, ObjectModel om) {
        int numPoints = vertices.length;

        // Create a mesh and set it to line mode
        Mesh mesh = om.path.getMesh();

        if (mesh == null || mesh == ObjectModel.blank) {
            mesh = new Mesh();
            mesh.setMode(Mesh.Mode.Lines);
            om.path.setMesh(mesh);
        }

        // Set the vertices
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));

        // Set up indices to connect the vertices as line segments
        short[] indices = new short[(numPoints - 1) * 2];
        for (int i = 0; i < numPoints - 1; i++) {
            indices[i * 2] = (short) i;
            indices[i * 2 + 1] = (short) (i + 1);
        }
        mesh.setBuffer(VertexBuffer.Type.Index, 2, BufferUtils.createShortBuffer(indices));

        mesh.updateBound();
        mesh.updateCounts();
    }

    public void toggleLabelShowing(boolean showing) {
        showLabel = showing;
        enqueue(this::updateLabelShowing);
    }

    public void toggleBarycenterShowing(boolean showing) {
        boolean wasShowing = showBarycenter;
        showBarycenter = showing;
        if (wasShowing != showBarycenter) {
            enqueue(() -> {
                if (showBarycenter) {
                    enableBarycenters();
                } else {
                    disableBarycenters();
                }
            });
        }
    }

    private void updateCurvesShowing() {
        for (ObjectModel om : modelMap.values()) {
            if (om.object.isExist()) {
                boolean showMe = om.object.getMass() >= minimumMassShowing;

                if (showMe && showOrbit) {
                    rootNode.attachChild(om.orbitNode);
                } else {
                    rootNode.detachChild(om.orbitNode);
                }
                if (showMe && showTrace) {
                    rootNode.attachChild(om.trace);
                } else {
                    rootNode.detachChild(om.trace);
                }
                if (showMe && showFullPath) {
                    rootNode.attachChild(om.path);
                } else {
                    rootNode.detachChild(om.path);
                }

                om.setShowApPe(showOrbit && focusing == om.object);
            }
        }
    }

    private void updateLabelShowing() {
        List<CelestialObject> objects = simulator.getObjects();  // sorted from big to small

        // List to keep track of labeled areas
        List<float[]> drawnObjectPoses = new ArrayList<>();

        // Attempt to label each object
        for (ObjectModel om : modelMap.values()) {
            CelestialObject obj = om.object;
            if (!obj.isExist()) {
                continue;
            }
            if (om.object.getMass() < minimumMassShowing) {
                om.setShowLabel(false);
            } else if (showLabel) {
                float labelX = paneX(obj.getX());
                float labelY = paneY(obj.getY());

                float[] canvasPos = new float[]{labelX, labelY};
                if (canLabel(drawnObjectPoses, canvasPos)) {
                    om.setShowLabel(true);
                    drawnObjectPoses.add(canvasPos);
                } else {
                    om.setShowLabel(false);
                }
            } else {
                om.setShowLabel(false);
            }
        }
    }

    private boolean canLabel(List<float[]> drawnObjectPoses, float[] targetPos) {
        // Check if the label overlaps with any existing labels
        for (float[] area : drawnObjectPoses) {
            double dt = Math.hypot(area[0] - targetPos[0], area[1] - targetPos[1]);
            if (dt < 5) {
                return false; // Not enough space
            }
        }
        return true; // Enough space to label
    }

    private void simpleTest() {
        CelestialObject sun = CelestialObject.create2d("Sun", 512000, 5, -1, -1, 0, 0, "#ff0000");
        simulator.addObject(sun);
        CelestialObject planet1 = CelestialObject.create2d("Sun2", 256000, 4, 20, 20, "#ffff00");
        simulator.addObject(planet1);
        planet1.setVelocity(simulator.computeOrbitVelocity(sun, planet1, new double[]{0, 0, 1}));

        scale = 0.1f;

        reloadObjects();
    }

    private void simpleTest2() {
        CelestialObject earth = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.earth,
                new double[]{-2e6, 1e6, -1e6},
                new double[3],
                scale
        );
//        earth.forceSetMass(earth.getMass() * 10);
        earth.forcedSetRotation(new double[]{0, 0, 1}, earth.getAngularVelocity());
        simulator.addObject(earth);
        CelestialObject moon = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.moon,
                new double[]{1e8, 1e8, 1e7},
                new double[3],
                scale
        );
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(earth, moon,
                0.7,
                VectorOperations.normalize(new double[]{2, 3, 1}));
//        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        scale = 5e-7f;

        reloadObjects();
        ambientLight.setColor(ColorRGBA.White);
    }

    private void simpleTest3() {
        CelestialObject sun = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.sun,
                new double[]{0, 0, 0},
                new double[3],
                scale
        );
        sun.forceSetMass(1);
        simulator.addObject(sun);

        CelestialObject earth = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.earth,
                new double[]{-3e9, 0, 0},
                new double[3],
                scale
        );
        earth.forceSetMass(1);
//        earth.forceSetMass(earth.getMass() * 10);
        simulator.addObject(earth);
        CelestialObject moon = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.moon,
                new double[]{-3.05e9, 0, 0},
                new double[3],
                scale
        );
        moon.forceSetMass(1);
        simulator.addObject(moon);
//        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.8);
//        vel[2] = VectorOperations.magnitude(vel) * 0.1;
//        moon.setVelocity(vel);

        scale = 5e-9f;

        reloadObjects();
    }

    private void simpleTest4() {
        CelestialObject earth = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.saturn,
                new double[]{-5e6, 1e6, -1e6},
                new double[3],
                scale
        );
//        earth.forceSetMass(earth.getMass() * 10);
        simulator.addObject(earth);
        CelestialObject moon = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.earth,
                new double[]{5e8, 0, 2e7},
                new double[3],
                scale
        );
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.8, new double[]{0, 0, 1});
        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        CelestialObject comet = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.charon,
                new double[]{1e8, 0, 1e7},
                new double[3],
                scale
        );
        simulator.addObject(comet);
        double[] vel2 = simulator.computeVelocityOfN(earth, comet, 2.1, new double[]{0, 0, 1});
        comet.setVelocity(vel2);

        scale = 1e-7f;

        reloadObjects();
        ambientLight.setColor(ColorRGBA.White);
    }

    private void threeBodyTest() {
        scale = SystemPresets.threeBodyTest(simulator);

        reloadObjects();
    }

    private void saturnRingTest() {
        scale = (float) SystemPresets.saturnRingTest(simulator, 100);

        reloadObjects();
        DirectionalLight directionalLight = new DirectionalLight();
        directionalLight.setColor(ColorRGBA.White);
//        directionalLight.setDirection(new Vector3f(0, 0.5f, -0.2f));

        // Add shadow renderer
        DirectionalLightShadowRenderer plsr = new DirectionalLightShadowRenderer(getAssetManager(),
                1024, 4);
        plsr.setLight(directionalLight);
        plsr.setShadowIntensity(0.9f); // Adjust the shadow intensity
        plsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        getViewPort().addProcessor(plsr);

        // Add shadow filter for softer shadows
        DirectionalLightShadowFilter plsf = new DirectionalLightShadowFilter(getAssetManager(), 1024, 4);
        plsf.setLight(directionalLight);
        plsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(getAssetManager());
        fpp.addFilter(plsf);

        rootNode.addLight(directionalLight);
        getViewPort().addProcessor(fpp);
//        model.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    private void tidalTest() {
        CelestialObject jupiter = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.jupiter,
                new double[]{0, 0, 0},
                new double[3],
                scale
        );
//        earth.forceSetMass(earth.getMass() * 10);
        simulator.addObject(jupiter);
        CelestialObject moon = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.mars,
                new double[]{2e8, 0, 1e5},
                new double[3],
                scale
        );
        moon.forcedSetRotation(moon.getRotationAxis(), 1e-3);
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(jupiter, moon, -0.99, new double[]{0, 0, 1});
//        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        scale = 1e-7f;

        ambientLight.setColor(ColorRGBA.White);
    }

    private void solarSystemTest() {
        scale = Preset.SOLAR_SYSTEM.instantiate(simulator);
        scale *= 0.5;
    }

    private void pink() {
        CelestialObject sun = simulator.findByName("Sun");
        String color = "#ff55aa";
        sun.setColorCode(color);
        sun.setLightColorCode(color);
    }

    private void smallSolarSystemTest() {
        scale = SystemPresets.smallSolarSystem(simulator);
        scale *= 0.5;
    }

    private void solarSystemWithCometsTest() {
        scale = SystemPresets.solarSystemWithComets(simulator);
        scale *= 0.5;
    }

    private void ellipseClusterTest() {
        scale = SystemPresets.ellipseCluster(simulator, 180);
        simulator.setEnableDisassemble(false);

        ambientLight.setColor(ColorRGBA.White.mult(0.5f));
    }

    private void subStarTest() {
        scale = SystemPresets.dwarfStarTest(simulator);
    }

    private void chaosSolarSystemTest() {
        scale = SystemPresets.randomStarSystem(simulator, 100);
        simulator.setEnableDisassemble(false);

//        ambientLight.setColor(ColorRGBA.White.mult(0.5f));
    }

    private void twoChaosSolarSystemTest() {
        scale = SystemPresets.twoRandomStarSystems(simulator);
        simulator.setEnableDisassemble(false);

//        ambientLight.setColor(ColorRGBA.White.mult(0.5f));
    }

    private void twoChaosSystemTest() {
        scale = SystemPresets.twoRandomChaosSystems(simulator);
        simulator.setEnableDisassemble(false);

//        ambientLight.setColor(ColorRGBA.White.mult(0.5f));
    }

    private void rocheEffectTest() {
        CelestialObject earth = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.earth,
                new double[]{-5e6, 1e6, -1e6},
                new double[3],
                scale
        );
//        earth.forceSetMass(earth.getMass() * 10);
        simulator.addObject(earth);
        CelestialObject moon = SystemPresets.createObjectPreset(
                simulator,
                SystemPresets.moon,
                new double[]{5e7, 0, 5e6},
                new double[3],
                scale
        );
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.30, new double[]{0, 0, 1});
        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        scale = 5e-7f;

        reloadObjects();
        ambientLight.setColor(ColorRGBA.White);

        simulator.setEnableDisassemble(true);
    }

    private RefFrame getRefFrame() {
        return refFrame;
    }

    @Override
    public void stop() {
        super.stop();

        FxApp fxApp = getFxApp();
        System.out.println(fxApp + " stop");
        if (fxApp != null) {
            Platform.runLater(() -> {
                try {
                    fxApp.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    protected ObjectModel getObjectModel(CelestialObject object) {
        ObjectModel om = modelMap.get(object);
        if (om == null) {
            om = diedObjects.get(object);
            if (om == null) {
//                throw new IllegalArgumentException("Object " + object.getName() + " has no model.");
//                System.err.println("Object " + object.getName() + " has no model.");
                return null;
            }
        }
        return om;
    }

    // Controller interactions

    public Simulator getSimulator() {
        return simulator;
    }

    public CelestialObject getFocusing() {
        return focusing;
    }

    public void clearFocus() {
        enqueue(() -> {
            focusing = null;
            updateCurvesShowing();
        });
    }

    public void clearLand() {
        enqueue(() -> {
            CelestialObject object = firstPersonStar.objectModel.object;
            firstPersonStar.objectModel.rotatingNode.detachChild(firstPersonStar.cameraNode);
            firstPersonStar.objectModel.rotatingNode.detachChild(firstPersonStar.northNode);
            firstPersonStar = null;

            setCamera3rdPerson();
            double targetScale = get3rdPersonObjectViewScale(object);
            scale = targetScale;
//            double factor = targetScale / scale;
//            scaleScene((float) factor);

            focusOn(object, false);

            // reset to a top view
            cam.setLocation(lookAtPoint.add(new Vector3f(0, 0, 100)));
            cam.lookAt(lookAtPoint, worldUp);
        });
    }

    private void setSpeed() {
        simulator.setTimeStep(speed);
    }

    public void speedUpAction() {
        enqueue(() -> {
            speed *= 2;
            setSpeed();
        });
    }

    public void speedDownAction() {
        enqueue(() -> {
            speed /= 2;
            setSpeed();
        });
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void setTracePathOrbit(boolean showTrace, boolean showFullPath, boolean showOrbit) {
        enqueue(() -> {
            this.showTrace = showTrace;
            this.showFullPath = showFullPath;
            this.showOrbit = showOrbit;

            updateCurvesShowing();
        });
    }

    public void setEclipticOrbitOnly(boolean eclipticOrbitOnly) {
        this.eclipticOrbitOnly = eclipticOrbitOnly;
    }

    public void setPathLength(double pathLength) {
        this.pathLength = pathLength;
    }

    public void setRefFrame(RefFrame refFrame) {
        this.refFrame = refFrame;
    }

    public void setShowHillSphere(boolean show) {
        enqueue(() -> {
            for (CelestialObject object : simulator.getObjects()) {
                ObjectModel om = modelMap.get(object);
                om.setShowHillSphere(show);
            }
        });
    }

    public void setShowRocheLimit(boolean show) {
        enqueue(() -> {
            for (CelestialObject object : simulator.getObjects()) {
                ObjectModel om = modelMap.get(object);
                om.setShowRocheLimit(show);
            }
        });
    }

    public double getScale() {
        return scale;
    }

    public double getSimulationSpeed() {
        return speed;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isFirstPerson() {
        return firstPersonStar != null;
    }

    public boolean isInSpawningMode() {
        return spawning != null;
    }

    public void enterSpawningMode(CelestialObject co, double orbitSpeed) {
        enqueue(() -> {
            ObjectModel om = new ObjectModel(co, this);
            spawning = new SpawningObject(this, om, orbitSpeed);

            rootNode.attachChild(spawning.primaryLine);
            rootNode.attachChild(spawning.secondaryLine);

            reloadObjects();
        });
    }

    public void exitSpawningMode() {
        enqueue(() -> {
            if (spawning != null) {
                rootNode.detachChild(spawning.model.objectNode);
                rootNode.detachChild(spawning.model.orbitNode);
                rootNode.detachChild(spawning.primaryLine);
                rootNode.detachChild(spawning.secondaryLine);
                spawning.model.setShowApPe(false);

                gridPlaneNode.hide();

                spawning = null;
            }
        });
    }

    public void setScaleEnqueue(double scale) {
        enqueue(() -> {
            this.scale = scale;
            updateLabelShowing();
            updateCurvesShowing();
        });
    }

    public void setSimulatorEnqueue(Simulator simulator) {
        enqueue(() -> setSimulator(simulator));
    }

    private void setSimulator(Simulator simulator) {
        for (ObjectModel om : modelMap.values()) {
            detachObjectModel(om);
            rootNode.detachChild(om.path);
            rootNode.detachChild(om.trace);
        }
        modelMap.clear();

        this.simulator = simulator;
        screenCenter.set(0, 0, 0);
        reloadObjects();
    }

    public void updateMinimumMassShowing(double minimumMassShowing) {
        enqueue(() -> {
            this.minimumMassShowing = minimumMassShowing;
            updateCurvesShowing();
        });
    }

    public void gcDiedModels() {
        double now = simulator.getTimeStepAccumulator();
        for (var it = diedObjects.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (now - entry.getKey().getDieTime() > Simulator.MAX_TIME_AFTER_DIE) {
                // todo: orbit, path
                it.remove();
            }
        }
    }

    public enum RefFrame {
        STATIC,
        SYSTEM,
        TARGET
    }

    public class MouseManagement {
        boolean pressed;
        boolean dragging;
        Vector2f initClickPos;

        void press(boolean pressed) {
            this.pressed = pressed;
            if (pressed) {
                initClickPos = inputManager.getCursorPosition().clone();
            } else {
                dragging = false;
            }
        }

        void updateDragging() {
            if (initClickPos == null) return;

            Vector2f pos = inputManager.getCursorPosition();
            float dt = pos.distance(initClickPos);

            if (dt > 5f) dragging = true;
        }
    }
}
