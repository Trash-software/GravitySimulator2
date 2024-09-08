package com.trashsoftware.gravity2.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.fxml.FxApp;
import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.physics.*;
import javafx.application.Platform;

import java.util.*;

public class JmeApp extends SimpleApplication {
    private static JmeApp instance;

    // Load the font for the labels
    protected BitmapFont font;
    protected ColorRGBA backgroundColor = ColorRGBA.Black;

    private boolean leftButtonPressed = false;
    private boolean rightButtonPressed = false;
    private boolean middleButtonPressed = false;
    private boolean keyWPressed, keyUpPressed, keyDownPressed;

    private float horizontalSpeed = 50.0f;
    private float verticalSpeed = 50.0f;
    private float rotationSpeed = 3.0f;

    private float azimuthSensitivity = 50.0f;
    private float altitudeAngleSensitivity = 50.0f;
//    private Vector3f pivotPoint = Vector3f.ZERO;  // Assuming the object is at the origin

    private double pathLength = 5000.0;

    boolean initialized = false;

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
    private CompassNode compassNode;
    private GuiTextNode lonLatTextNode;
    private boolean showLabel = true;
    private boolean showBarycenter = false;
    private boolean showTrace, showFullPath, showOrbit;
    private CelestialObject focusing;
    private FirstPersonMoving firstPersonStar;
    private final FxApp fxApp;
    //    private final Set<Spatial> eachFrameErase = new HashSet<>();
    private AmbientLight ambientLight;
    private RefFrame refFrame = RefFrame.STATIC;
    
    protected ObjectModel spawning;

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

        setupMouses();
        initLights();
        initMarks();

//        putTestBox();
        initializeSimulator();

        setCamera3rdPerson();
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
        axisMarkNode = create3DCrossAt(
                Vector3f.ZERO,
                0.01f, true, true);

        int screenWidth = settings.getWidth();
        int screenHeight = settings.getHeight();

        compassNode = new CompassNode(this);
        compassNode.setLocalTranslation(100, screenHeight - 100, 0);

        lonLatTextNode = new GuiTextNode(this);
        lonLatTextNode.setLocalTranslation(20, screenHeight - 200, 0);
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

            lonLatTextNode.setText(String.format("%s\n%s\n%s",
                    lon, lat, uc.distance(firstPersonStar.getAltitude())));
        }
    }

    private void clearUnUsedMeshes() {
        for (ObjectModel om : modelMap.values()) {
            if (!showFullPath) {
                om.path.setMesh(om.blank);
            }
            if (!showTrace) {
                om.pathGradient.setMesh(om.blank);
            }
            if (!showOrbit) {
                om.orbit.setMesh(om.blank);
            }
        }
        // todo: destroyed object's mesh still on screen
//        for (ObjectModel om : diedObjects.values()) {
//            if (!showFullPath) {
//                om.path.setMesh(om.blank);
//            }
//            if (!showTrace) {
//                om.pathGradient.setMesh(om.blank);
//            }
//            if (!showOrbit) {
//                om.orbit.setMesh(om.blank);
//            }
//        }
//        List<CelestialObject> newDestroyed = simulator.getAndClearNewlyDestroyed();
//        for (CelestialObject co : newDestroyed) {
//            ObjectModel om = getObjectModel(co);
//            if (om != null) {
//                if (!showFullPath) {
//                    om.path.setMesh(om.blank);
//                }
//                if (!showTrace) {
//                    om.pathGradient.setMesh(om.blank);
//                }
//                if (!showOrbit) {
//                    om.orbit.setMesh(om.blank);
//                }
//            }
//        }
    }

    private void initializeSimulator() {
        initialized = true;
        simulator = new Simulator();

//        simpleTest();
//        simpleTest2();
//        simpleTest3();
//        rocheEffectTest();
//        solarSystemTest();
        solarSystemWithCometsTest();
//        ellipseClusterTest();

        getFxApp().notifyObjectCountChanged(simulator);
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

                // died object
                rootNode.detachChild(om.objectNode);
                rootNode.detachChild(om.orbit);

                // left its paths continues alive
                if (om.emissionLight != null) {
                    rootNode.removeLight(om.emissionLight);
//                    viewPort.removeProcessor(om.plsr);
                    viewPort.removeProcessor(om.fpp);
                }
            }
        }

        for (CelestialObject object : objects) {
            ObjectModel om = modelMap.get(object);
            if (om == null) {
                om = new ObjectModel(object, this);
                System.out.println("Creating " + object.getName());
                modelMap.put(object, om);
                rootNode.attachChild(om.objectNode);

                // Initialize the geometry for the curve (we'll reuse this each frame)
                rootNode.attachChild(om.path);
                rootNode.attachChild(om.orbit);
                rootNode.attachChild(om.pathGradient);
                
                // Synchronize the global label showing status to the new object
                om.setShowLabel(showLabel);
            }
            om.notifyObjectChanged();
        }

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

    private void setCamera1stPerson() {
        cam.setFrustumPerspective(45f,
                (float) cam.getWidth() / cam.getHeight(),
                0.1f,
                1e8f);

        rootNode.detachChild(axisMarkNode);
        guiNode.attachChild(compassNode);
        guiNode.attachChild(lonLatTextNode);
    }

    private void setCamera3rdPerson() {
        cam.setFrustumPerspective(45f,
                (float) cam.getWidth() / cam.getHeight(),
                0.1f,
                1e6f);

        rootNode.attachChild(axisMarkNode);
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
            leftButtonPressed = isPressed;
            if (!isPressed) {
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
        } else if (name.equals("RightClick")) {
            rightButtonPressed = isPressed;
        } else if (name.equals("MiddleClick")) {
            middleButtonPressed = isPressed;
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
            if (leftButtonPressed) {
                if (firstPersonStar == null) {
                    if (name.equals("MouseMoveX-")) {
                        // Move the camera horizontally when dragging the left button
                        Vector3f left = cam.getLeft().mult(-horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
//                    lookAtPoint.addLocal(left);
                        screenCenter.addLocal(left);
                        if (focusing != null) centerRelToFocus.addLocal(left);
                    } else if (name.equals("MouseMoveX+")) {
                        // Move the camera horizontally when dragging the left button
                        Vector3f left = cam.getLeft().mult(horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
//                    lookAtPoint.addLocal(left);
                        screenCenter.addLocal(left);
                        if (focusing != null) centerRelToFocus.addLocal(left);
                    } else if (name.equals("MouseMoveY-")) {
                        // Move the camera vertically when dragging the left button
                        Vector3f up = cam.getUp().mult(verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
//                    lookAtPoint.addLocal(up);
                        screenCenter.addLocal(up);
                        if (focusing != null) centerRelToFocus.addLocal(up);
                    } else if (name.equals("MouseMoveY+")) {
                        // Move the camera vertically when dragging the left button
                        Vector3f up = cam.getUp().mult(-verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
//                    lookAtPoint.addLocal(up);
                        screenCenter.addLocal(up);
                        if (focusing != null) centerRelToFocus.addLocal(up);
                    }
                    cam.lookAt(lookAtPoint, worldUp);
                } else {
                    if (name.equals("MouseMoveX-")) {
                        // Move the camera horizontally when dragging the left button
                        firstPersonStar.azimuthChange(-value * azimuthSensitivity);
                    } else if (name.equals("MouseMoveX+")) {
                        // Move the camera horizontally when dragging the left button
                        firstPersonStar.azimuthChange(value * azimuthSensitivity);
                    } else if (name.equals("MouseMoveY-")) {
                        // Move the camera vertically when dragging the left button
                        firstPersonStar.lookingAltitudeChange(-value * altitudeAngleSensitivity);
                    } else if (name.equals("MouseMoveY+")) {
                        // Move the camera vertically when dragging the left button
                        firstPersonStar.lookingAltitudeChange(value * altitudeAngleSensitivity);
                    }
//                    Vector3f sightPos = firstPersonStar.getSightLocalPos();
//                    firstPersonStar.sightPoint.setLocalTranslation(sightPos);
                }
            }
            if (rightButtonPressed) {
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
            if (middleButtonPressed) {
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
            }
        }
    };

    private void rotateAroundPivot(float amount, Vector3f axis) {
        // Create a quaternion for rotation based on the given axis and amount
        Quaternion rotation = new Quaternion().fromAngleAxis(amount, axis);

        // Calculate the direction vector from the pivot point to the camera
        Vector3f direction = cam.getLocation().subtract(lookAtPoint);

        // Apply the rotation to the direction vector
        direction = rotation.mult(direction);

        // Calculate the new camera position based on the rotated direction
        Vector3f newCamPos = lookAtPoint.add(direction);

        // Set the new camera location and update its orientation
        cam.setLocation(newCamPos);
        cam.lookAt(lookAtPoint, worldUp);

//        System.out.println(cam.getLocation());
    }

    private void scaleScene(float scaleFactor) {
        scale = scale * scaleFactor;

        screenCenter.multLocal(scaleFactor);

        updateLabelShowing();
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
                    focusOn(object);
                    break;
                }
                BitmapText bt = (BitmapText) objectModel.labelNode.getChildren().get(0);
                if (geom == bt.getChildren().get(0)) {
                    focusOn(object);
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

    public void focusOn(CelestialObject object) {
        System.out.println("Focused on " + object.getName());

        focusing = object;
        double focusingLastX = focusing.getX() - refOffsetX;
        double focusingLastY = focusing.getY() - refOffsetY;
        double focusingLastZ = focusing.getZ() - refOffsetZ;

        centerRelToFocus.set(0, 0, 0);

        screenCenter.setX(focusingLastX * scale);
        screenCenter.setY(focusingLastY * scale);
        screenCenter.setZ(focusingLastZ * scale);

        getFxApp().getControlBar().setFocus();
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

    private void moveCameraWithLookAtPoint(Vector3f oldLookAt, Vector3f newLookAt) {
        float dt = cam.getLocation().distance(oldLookAt);

        Vector3f direction = cam.getDirection();
        Vector3f newLocation = newLookAt.subtract(direction.mult(dt));
        cam.setLocation(newLocation);
        cam.lookAt(newLookAt, worldUp);
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
            updateBarycenterNode(hs);
        }
        if (globalBarycenterNode != null) {
            double[] barycenter = simulator.barycenter();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);
            globalBarycenterNode.setLocalTranslation(x, y, z);
        }
    }

    private void updateBarycenterNode(HieraticalSystem hs) {
        if (!hs.isObject()) {
            double[] barycenter = hs.getPosition();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);

            ObjectModel om = modelMap.get(hs.master);
            if (om.barycenterMark == null) {
                System.err.println("System " + hs.master.getName() + " does not have valid barycenter mark");
            } else {
                om.barycenterMark.setLocalTranslation(x, y, z);
                for (HieraticalSystem child : hs.getChildrenSorted()) {
                    updateBarycenterNode(child);
                }
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
                globalBarycenterNode = create3DCrossAt(Vector3f.ZERO, 3, false, false);
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
                om.barycenterMark = create3DCrossAt(Vector3f.ZERO, (float) markSize, false, false);
                om.barycenterMark.setLocalTranslation(x, y, z);
            }

            rootNode.attachChild(om.barycenterMark);

            for (HieraticalSystem child : hs.getChildrenSorted()) {
                enableBarycenterNodes(child, Math.max(0.5, markSize - 0.5));
            }
        }
    }

    public Node create3DCrossAt(Vector3f center, float length,
                                boolean axisColor, boolean half) {
        Node crossNode = new Node("3D Cross at " + center.toString());
        ColorRGBA xColor, yColor, zColor;
        if (axisColor) {
            xColor = ColorRGBA.Red;
            yColor = ColorRGBA.Green;
            zColor = ColorRGBA.Blue;
        } else {
            xColor = ColorRGBA.White;
            yColor = ColorRGBA.White;
            zColor = ColorRGBA.White;
        }

        // X-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(length, 0, 0)), xColor));
        if (!half)
            crossNode.attachChild(createLine(center, center.add(new Vector3f(-length, 0, 0)), xColor));

        // Y-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, length, 0)), yColor));
        if (!half)
            crossNode.attachChild(createLine(center, center.add(new Vector3f(0, -length, 0)), yColor));

        // Z-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, 0, length)), zColor));
        if (!half)
            crossNode.attachChild(createLine(center, center.add(new Vector3f(0, 0, -length)), zColor));

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

    private void drawOrbits() {
        for (CelestialObject object : simulator.getObjects()) {
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
                OrbitalElements specs = OrbitCalculator.computeOrbitSpecs3d(child,
                        velocity,
                        barycenter,
                        child.getMass() + parent.getMass(),
                        simulator.getG());

                if (specs.isElliptical()) {
                    drawEllipticalOrbit(object, barycenter, specs);
                }
            }
        }
    }

    private void drawEllipticalOrbit(CelestialObject co, double[] barycenter, OrbitalElements oe) {
        ObjectModel om = modelMap.get(co);

        Mesh mesh = om.orbit.getMesh();
        if (mesh == null || mesh == om.blank) {
            mesh = new Mesh();
            mesh.setMode(Mesh.Mode.LineStrip);
            om.orbit.setMesh(mesh);
        }

        om.makeOrbitMesh(mesh,
                barycenter,
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
//            if (obj.getMass() < minimumMassShowing) continue;
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
            Mesh mesh = om.pathGradient.getMesh();
            if (mesh == null || mesh == om.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                om.pathGradient.setMesh(mesh);
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
//            if (obj.getMass() < minimumMassShowing) continue;
            var path = entry.getValue();
            if (obj == null) continue;

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

            ObjectModel om = getObjectModel(obj);
            if (om == null) continue;
            drawPolyLine(vertices, om);
        }
    }

    private void drawPolyLine(Vector3f[] vertices, ObjectModel om) {
        int numPoints = vertices.length;

        // Create a mesh and set it to line mode
        Mesh mesh = om.path.getMesh();

        if (mesh == null || mesh == om.blank) {
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

    private void updateLabelShowing() {
        List<CelestialObject> objects = simulator.getObjects();  // sorted from big to small

        // List to keep track of labeled areas
        List<float[]> drawnObjectPoses = new ArrayList<>();

        // Attempt to label each object
        for (CelestialObject obj : objects) {
            ObjectModel om = modelMap.get(obj);

            if (showLabel) {
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
        planet1.setVelocity(simulator.computeOrbitVelocity(sun, planet1));

        scale = 0.1f;

        reloadObjects();
    }

    private void simpleTest2() {
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
                new double[]{1e8, 0, 5e6},
                new double[3],
                scale
        );
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.8);
        vel[2] = VectorOperations.magnitude(vel) * 0.1;
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

    private void solarSystemTest() {
        scale = SystemPresets.solarSystem(simulator);
        scale *= 0.5;

        reloadObjects();
    }

    private void solarSystemWithCometsTest() {
        scale = SystemPresets.solarSystemWithComets(simulator);
        scale *= 0.5;

        reloadObjects();
    }

    private void ellipseClusterTest() {
        scale = SystemPresets.ellipseCluster(simulator, 150);

        reloadObjects();

        ambientLight.setColor(ColorRGBA.White);
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
        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.30);
        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        scale = 5e-7f;

        reloadObjects();
        ambientLight.setColor(ColorRGBA.White);
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
        focusing = null;
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

            focusOn(object);

            // reset to a top view
            cam.setLocation(lookAtPoint.add(new Vector3f(0, 0, 100)));
            cam.lookAt(lookAtPoint, worldUp);
        });
    }

    private void setSpeed() {
        simulator.setTimeStep(speed);
    }

    public void speedUpAction() {
        speed *= 2;
        setSpeed();
    }

    public void speedDownAction() {
        speed /= 2;
        setSpeed();
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void setTracePathOrbit(boolean showTrace, boolean showFullPath, boolean showOrbit) {
        this.showTrace = showTrace;
        this.showFullPath = showFullPath;
        this.showOrbit = showOrbit;
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

    public void setSpawning(CelestialObject co) {
        // todo
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

    public void gcDiedModels() {
        double now = simulator.getTimeStepAccumulator();
        for (var it = diedObjects.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (now - entry.getKey().getDieTime() > Simulator.MAX_TIME_AFTER_DIE) {
                it.remove();
            }
        }
    }

    public enum RefFrame {
        STATIC,
        SYSTEM,
        TARGET
    }
}
