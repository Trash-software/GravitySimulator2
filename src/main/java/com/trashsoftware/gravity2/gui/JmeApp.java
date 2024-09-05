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
import com.jme3.scene.*;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.fxml.FxApp;
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
    private boolean keyWPressed = false;
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
    //    private List<Geometry> tempGeom = new ArrayList<>();
//    private Node rootLabelNode = new Node("RootLabelNode");
    private boolean showLabel = true;
    private boolean showBarycenter = false;
    private boolean showTrace, showFullPath, showOrbit;
    private CelestialObject focusing;
    private FirstPersonMoving firstPersonStar;
    private final FxApp fxApp;
    //    private final Set<Spatial> eachFrameErase = new HashSet<>();
    private AmbientLight ambientLight;
    private RefFrame refFrame = RefFrame.STATIC;

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
    }

    @Override
    public void simpleUpdate(float tpf) {
//        super.simpleUpdate(tpf);

        if (playing) {
            int nPhysicalFrames = Math.round(tpf * 1000);
            Simulator.SimResult sr = simulator.simulate(nPhysicalFrames, false);
            if (sr == Simulator.SimResult.NUM_CHANGED) {
                loadObjectsToView();
                getFxApp().notifyObjectCountChanged(simulator);
            } else if (sr == Simulator.SimResult.TOO_FAST) {
                getFxApp().getControlBar().speedDownAction();
                loadObjectsToView();
                getFxApp().notifyObjectCountChanged(simulator);
            }

            updateRefFrame();
            if (firstPersonStar != null) {
                if (keyWPressed) {
                    firstPersonStar.moveForward(1000);
                }
                moveCameraWithFirstPerson();
            } else if (focusing != null) {
                moveScreenWithFocus();
            }
        }

        updateModelPositions();

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
            drawBarycenter();
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
//        Mesh cross = create3DCrossAt(new Vector3f(20, 20,), 10);
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
    }

    private void initializeSimulator() {
        initialized = true;
        simulator = new Simulator();

//        simpleTest();
        simpleTest2();
//        simpleTest3();
//        solarSystemTest();
//        ellipseClusterTest();

        getFxApp().notifyObjectCountChanged(simulator);
    }

    void loadObjectsToView() {
        List<CelestialObject> objects = simulator.getObjects();
        Set<CelestialObject> objectSet = new HashSet<>(objects);

        // garbage collect for those destroyed things
//        modelMap.entrySet().removeIf(entry -> !objects.contains(entry.getKey()));

        for (ObjectModel om : modelMap.values()) {
            if (!objectSet.contains(om.object)) {
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
                modelMap.put(object, om);
                rootNode.attachChild(om.objectNode);

                // Initialize the geometry for the curve (we'll reuse this each frame)
                rootNode.attachChild(om.path);
                rootNode.attachChild(om.orbit);
                rootNode.attachChild(om.pathGradient);
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

    private void setupMouses() {
//        cam.setFrustumNear(1f);
//        cam.setFrustumFar(1e7f);

        cam.setFrustumPerspective(45f,
                (float) cam.getWidth() / cam.getHeight(),
                0.01f,
                1e5f);

        cam.setLocation(new Vector3f(0, 0, 100));

        // Disable flyCam
//        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true); // Show the mouse cursor
        
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addListener(actionListener, "MoveForward");

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

    // Action listener to track button press/release
    private final ActionListener actionListener = (name, isPressed, tpf) -> {
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
        } else if (name.equals("MoveForward")) {
            System.out.println("W pressed: " + isPressed);
            keyWPressed = isPressed;
//            if (isPressed && firstPersonStar != null) {
//                firstPersonStar.moveForward(1000);
//            }
        }
    };

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
            scale = 100 / object.getEquatorialRadius();
            System.out.println("New scale: " + scale);

            firstPersonStar = new FirstPersonMoving(om);
//            Vector3f surfacePos = firstPersonStar.getCurrentLocalPos().toVector3f();
//            firstPersonStar.cameraNode.setLocalTranslation(surfacePos);
//            Vector3f sightPos = firstPersonStar.getSightLocalPos();
//            firstPersonStar.sightPoint.setLocalTranslation(sightPos);

            om.rotatingNode.attachChild(firstPersonStar.cameraNode);
            om.rotatingNode.attachChild(firstPersonStar.eastNode);
            
            firstPersonStar.updateCamera(cam);

//            Vector3f camPos = firstPersonStar.cameraNode.getWorldTranslation();
//            cam.setLocation(camPos);
//
//            cam.lookAtDirection(firstPersonStar.getLookingDirection(cam),
//                    firstPersonStar.getUpVector());

//            Vector3f centerPos = firstPersonStar.objectModel.rotatingNode.getWorldTranslation();
//            Vector3f realUp = camPos.subtract(centerPos).normalize();
//            cam.lookAt(firstPersonStar.sightPoint.getWorldTranslation(), firstPersonStar.getUpVector());
//            flyCam.setEnabled(true);

            getFxApp().getControlBar().setLand();
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
        if (refFrame == RefFrame.SYSTEM) {
            double[] barycenter = simulator.barycenter();
            refOffsetX = barycenter[0];
            refOffsetY = barycenter[1];
            refOffsetZ = barycenter[2];
        } else if (refFrame == RefFrame.TARGET) {
            if (firstPersonStar != null) {
                double[] pos = firstPersonStar.getObject().getPosition();
                refOffsetX = pos[0];
                refOffsetY = pos[1];
                refOffsetZ = pos[2];
            } else if (focusing != null) {
                double[] pos = focusing.getPosition();
                refOffsetX = pos[0];
                refOffsetY = pos[1];
                refOffsetZ = pos[2];
            }
        }
    }

    private void drawBarycenter() {
        List<HieraticalSystem> roots = simulator.getRootSystems();
        for (HieraticalSystem hs : roots) {
            drawSystemBarycenter(hs, 2);
        }

        // overall barycenter
        if (roots.size() > 1) {
            double[] barycenter = simulator.barycenter();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);
            // todo
//            gc2d.setStroke(TEXT);
//            gc2d.strokeLine(x - 5, y, x + 5, y);
//            gc2d.strokeLine(x, y - 5, x, y + 5);


        }
    }

    private void drawSystemBarycenter(HieraticalSystem hs, double markSize) {
        if (!hs.isObject()) {
            double[] barycenter = hs.getPosition();
            float x = paneX(barycenter[0]);
            float y = paneY(barycenter[1]);
            float z = paneZ(barycenter[2]);

            Node cross = create3DCrossAt(new Vector3f(x, y, z), (float) markSize);
//            rootNode.attachChild(cross);
//            eachFrameErase.add(cross);

            for (HieraticalSystem child : hs.getChildrenSorted()) {
                drawSystemBarycenter(child, Math.max(2, markSize - 0.5));
            }
        }
    }

    public Node create3DCrossAt(Vector3f center, float length) {
        Node crossNode = new Node("3D Cross at " + center.toString());
        // X-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(length, 0, 0)), ColorRGBA.Red));
        crossNode.attachChild(createLine(center, center.add(new Vector3f(-length, 0, 0)), ColorRGBA.Red));

        // Y-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, length, 0)), ColorRGBA.Green));
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, -length, 0)), ColorRGBA.Green));

        // Z-axis lines
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, 0, length)), ColorRGBA.Blue));
        crossNode.attachChild(createLine(center, center.add(new Vector3f(0, 0, -length)), ColorRGBA.Blue));

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
//                System.out.println(parentSystem + " " + Arrays.toString(parentSystem.getPosition()) + " " +
//                        Arrays.toString(parentSystem.getVelocity()));
//                double[] barycenter = parentSystem.getPosition();

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

        Mesh mesh = om.createOrbitMesh(barycenter,
                oe,
                360);
        Geometry orbitGeom = om.orbit;
        orbitGeom.setMesh(mesh);
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
            ObjectModel om = modelMap.get(obj);

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
            Mesh mesh = new Mesh();
            mesh.setMode(Mesh.Mode.LineStrip);

            // Set the vertices
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));

            // Set the vertex colors
            mesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(colors));

            // Update the mesh to generate it
            mesh.updateBound();
            mesh.updateCounts();

            om.pathGradient.setMesh(mesh);
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

            Geometry lineGeom = modelMap.get(obj).path;
            drawPolyLine(vertices, lineGeom);
        }
    }

    private void drawPolyLine(Vector3f[] vertices, Geometry lineGeom) {
        int numPoints = vertices.length;

        // Create a mesh and set it to line mode
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);

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

        lineGeom.setMesh(mesh);
    }

    public void toggleLabelShowing(boolean showing) {
        showLabel = showing;
        enqueue(this::updateLabelShowing);
    }

    public void toggleBarycenterShowing(boolean showing) {
        showBarycenter = showing;
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

        loadObjectsToView();
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
                new double[]{5e7, 0, 5e6},
                new double[3],
                scale
        );
        simulator.addObject(moon);
        double[] vel = simulator.computeVelocityOfN(earth, moon, 0.8);
        vel[2] = VectorOperations.magnitude(vel) * 0.1;
        moon.setVelocity(vel);

        scale = 5e-7f;

        loadObjectsToView();
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

        loadObjectsToView();
    }

    private void solarSystemTest() {
        scale = SystemPresets.solarSystem(simulator);
        scale *= 0.5;

        loadObjectsToView();
    }

    private void ellipseClusterTest() {
        scale = SystemPresets.ellipseCluster(simulator, 100);

        loadObjectsToView();

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
            firstPersonStar.objectModel.rotatingNode.detachChild(firstPersonStar.eastNode);
            firstPersonStar = null;
            flyCam.setEnabled(false);

//            scale = 
            focusOn(object);
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

    public enum RefFrame {
        STATIC,
        SYSTEM,
        TARGET
    }
}
