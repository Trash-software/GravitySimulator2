package com.trashsoftware.gravity2.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
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
    private float horizontalSpeed = 50.0f;
    private float verticalSpeed = 50.0f;
    private float rotationSpeed = 3.0f;
    private float zoomSpeed = 2f; // Speed of zooming in/out
//    private Vector3f pivotPoint = Vector3f.ZERO;  // Assuming the object is at the origin

    private double pathLength = 5000.0;

    boolean initialized = false;

    protected Simulator simulator;
    protected double speed = 1.0;
    protected boolean playing = true;
    protected double scale = 1.0;
    private float centerX, centerY, centerZ;
    private double refOffsetX, refOffsetY, refOffsetZ;
    private double focusingLastX, focusingLastY, focusingLastZ;

    private Vector3f lookAtPoint = new Vector3f(0, 0, 0);
    private Vector3f worldUp = Vector3f.UNIT_Z;

    //    private final Map<CelestialObject, Geometry> lineGeometries = new HashMap<>();
    private final Map<CelestialObject, ObjectModel> modelMap = new HashMap<>();
    //    private List<Geometry> tempGeom = new ArrayList<>();
//    private Node rootLabelNode = new Node("RootLabelNode");
    private boolean showLabel = true;
    private boolean showTrace, showFullPath, showOrbit;
    private CelestialObject focusing;

    public static JmeApp getInstance() {
        return instance;
    }

    @Override
    public void simpleInitApp() {
        instance = this;

        font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        setupMouses();

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
            }

            if (focusing != null) {
                moveScreenWithFocus();
            }
        }

        updateModelPositions();

//        rootNode.getChildren().removeAll(tempGeom);
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
//        drawNameTexts();
//        System.out.println(tpf * 1000);
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
//        simpleTest2();
        solarSystemTest();
    }

    void loadObjectsToView() {
        rootNode.detachAllChildren();
//        lineGeometries.clear();

        List<CelestialObject> objects = simulator.getObjects();

        // garbage collect for those destroyed things
//        modelMap.entrySet().removeIf(entry -> !objects.contains(entry.getKey()));

        for (CelestialObject object : objects) {
            ObjectModel om = modelMap.computeIfAbsent(object, o -> new ObjectModel(o, this));

            om.notifyObjectChanged();

            rootNode.attachChild(om.objectNode);

            // Initialize the geometry for the curve (we'll reuse this each frame)
            rootNode.attachChild(om.path);
            rootNode.attachChild(om.orbit);
            rootNode.attachChild(om.pathGradient);
        }

        updateModelPositions();
    }

    void updateModelPositions() {
        for (CelestialObject object : simulator.getObjects()) {
            ObjectModel objectModel = modelMap.get(object);
            if (objectModel == null) throw new RuntimeException(object.getName());
            objectModel.updateModelPosition(
                    this::paneX,
                    this::paneY,
                    this::paneZ,
                    scale
            );
        }
    }

    private void setupMouses() {
        cam.setFrustumNear(1f);
        cam.setFrustumFar(1e7f);

        cam.setLocation(new Vector3f(0, 0, 100));

        // Disable flyCam
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true); // Show the mouse cursor

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

        // Add listeners for the new mappings
        inputManager.addListener(analogListener, "MouseMoveX+", "MouseMoveX-", "MouseMoveY+", "MouseMoveY-");

        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false)); // Zoom in (scroll up)
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true)); // Zoom out (scroll down)

        // Add listeners for zooming
        inputManager.addListener(analogListener, "ZoomIn", "ZoomOut");
    }

    // Action listener to track button press/release
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("LeftClick")) {
                leftButtonPressed = isPressed;
                if (!isPressed) {
                    // Get the mouse click position
                    Vector2f click2d = inputManager.getCursorPosition();
                    Vector3f click3d = cam.getWorldCoordinates(click2d, 0f).clone();
                    Vector3f dir = cam.getWorldCoordinates(click2d, 1f).subtractLocal(click3d).normalizeLocal();

                    // Adjust the ray's origin to match the camera's actual position
                    click3d = cam.getLocation().clone();

                    // Create a ray from the camera's position in the direction of the click
                    Ray ray = new Ray(click3d, dir);

                    // Collect intersections between the ray and the scene
                    CollisionResults results = new CollisionResults();

                    // Iterate through all root node children (which are the object Nodes)
                    for (Spatial spatial : rootNode.getChildren()) {
                        if (spatial instanceof ObjectNode objectNode) {
                            objectNode.collideWith(ray, results);
                        }
                    }

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
            }
        }
    };

    private AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (leftButtonPressed) {
                if (name.equals("MouseMoveX-")) {
                    // Move the camera horizontally when dragging the left button
                    Vector3f left = cam.getLeft().mult(-horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
//                    lookAtPoint.addLocal(left);
                    centerX += left.x;
                    centerY += left.y;
                    centerZ += left.z;
                } else if (name.equals("MouseMoveX+")) {
                    // Move the camera horizontally when dragging the left button
                    Vector3f left = cam.getLeft().mult(horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
//                    lookAtPoint.addLocal(left);
                    centerX += left.x;
                    centerY += left.y;
                    centerZ += left.z;
                } else if (name.equals("MouseMoveY-")) {
                    // Move the camera vertically when dragging the left button
                    Vector3f up = cam.getUp().mult(verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
//                    lookAtPoint.addLocal(up);
                    centerX += up.x;
                    centerY += up.y;
                    centerZ += up.z;
                } else if (name.equals("MouseMoveY+")) {
                    // Move the camera vertically when dragging the left button
                    Vector3f up = cam.getUp().mult(-verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
//                    lookAtPoint.addLocal(up);
                    centerX += up.x;
                    centerY += up.y;
                    centerZ += up.z;
                }
                cam.lookAt(lookAtPoint, worldUp);

            }
            if (rightButtonPressed) {
                float rotationAmount = rotationSpeed * value;
//                System.out.println(cam.getLeft());
                if (name.equals("MouseMoveY-")) {
                    rotateAroundPivot(rotationAmount, cam.getLeft());
                } else if (name.equals("MouseMoveY+")) {
                    rotateAroundPivot(-rotationAmount, cam.getLeft());
                }
            }
            if (middleButtonPressed) {
                // Rotate the camera around the object when dragging the middle button
                float rotationAmount = rotationSpeed * value;
                if (name.equals("MouseMoveX-")) {
                    rotateAroundPivot(rotationAmount, Vector3f.UNIT_Z);
                } else if (name.equals("MouseMoveX+")) {
                    rotateAroundPivot(-rotationAmount, Vector3f.UNIT_Z);
                }
            }

            // Handle zooming
            if (name.equals("ZoomIn")) {
                zoomInAction();
            } else if (name.equals("ZoomOut")) {
                zoomOutAction();
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
        double newScale = scale * scaleFactor;
//        Vector3f newLookAt = lookAtPoint.mult(scaleFactor);
//        moveCameraWithLookAtPoint(lookAtPoint, newLookAt);
//        lookAtPoint = newLookAt;
//        System.out.println(newScale);
//        System.out.println(lookAtPoint + " " + cam.getLocation());

        scale = newScale;

        centerX *= scaleFactor;
        centerY *= scaleFactor;
        centerZ *= scaleFactor;

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

    private void focusOn(CelestialObject object) {
        System.out.println("Focused on " + object.getName());

        focusing = object;
        focusingLastX = focusing.getX() - refOffsetX;
        focusingLastY = focusing.getY() - refOffsetY;
        focusingLastZ = focusing.getZ() - refOffsetZ;

        centerX = (float) (focusingLastX * scale);
        centerY = (float) (focusingLastY * scale);
        centerZ = (float) (focusingLastZ * scale);

//        Vector3f newLookAt = new Vector3f((float) (focusingLastX * scale),
//                (float) (focusingLastY * scale),
//                (float) (focusingLastZ * scale));
//        moveCameraWithLookAtPoint(lookAtPoint, newLookAt);
//        lookAtPoint = newLookAt;

        FxApp.getInstance().getControlBar().setFocus();
    }

    private void moveScreenWithFocus() {
        float deltaX = (float) ((focusing.getX() - refOffsetX - focusingLastX) * scale);
        float deltaY = (float) ((focusing.getY() - refOffsetY - focusingLastY) * scale);
        float deltaZ = (float) ((focusing.getZ() - refOffsetZ - focusingLastZ) * scale);
//        Vector3f delta = new Vector3f(deltaX, deltaY, deltaZ);

        centerX += deltaX;
        centerY += deltaY;
        centerZ += deltaZ;

        focusingLastX = focusing.getX() - refOffsetX;
        focusingLastY = focusing.getY() - refOffsetY;
        focusingLastZ = focusing.getZ() - refOffsetZ;

//        cam.setLocation(cam.getLocation().add(delta));
//        lookAtPoint.addLocal(delta);
    }

    private void moveCameraWithLookAtPoint(Vector3f oldLookAt, Vector3f newLookAt) {
        float dt = cam.getLocation().distance(oldLookAt);

        Vector3f direction = cam.getDirection();
        Vector3f newLocation = newLookAt.subtract(direction.mult(dt));
        cam.setLocation(newLocation);
        cam.lookAt(newLookAt, worldUp);
    }

    public float paneX(double realX) {
        return (float) ((realX - refOffsetX) * scale - centerX);
    }

    public float paneY(double realY) {
        return (float) ((realY - refOffsetY) * scale - centerY);
    }

    public float paneZ(double realZ) {
        return (float) ((realZ - refOffsetZ) * scale - centerZ);
    }

    public double realXFromPane(float paneX) {
        return (paneX + centerX) / scale + refOffsetX;
    }

    public double realYFromPane(float paneY) {
        return (paneY + centerY) / scale + refOffsetY;
    }

    public double realZFromPane(float paneZ) {
        return (paneZ + centerZ) / scale + refOffsetZ;
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

//        System.out.println(co.getName());
//        System.out.println(oe);

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
        for (Map.Entry<CelestialObject, Deque<double[]>> entry : simulator.getRecentPaths().entrySet()) {
            var obj = entry.getKey();
//            if (obj.getMass() < minimumMassShowing) continue;
            var path = entry.getValue();
            if (obj == null) continue;

            Vector3f[] vertices = new Vector3f[path.size()];
            int index = 0;
            for (double[] pos : path) {

                Vector3f vector3f = new Vector3f(
                        paneX(pos[0]),
                        paneY(pos[1]),
                        paneZ(pos[2])
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
        updateLabelShowing();
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
    }

    private void solarSystemTest() {
        scale = (float) SystemPresets.solarSystem(simulator);
        scale *= 1;

        loadObjectsToView();
    }

    private RefFrame getRefFrame() {

        return RefFrame.STATIC;

    }

    @Override
    public void stop() {
        super.stop();

        FxApp fxApp = FxApp.getInstance();
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

    public void clearFocus() {
        focusing = null;
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

    public double getScale() {
        return scale;
    }

    public double getSimulationSpeed() {
        return speed;
    }

    public boolean isPlaying() {
        return playing;
    }

    public enum RefFrame {
        STATIC,
        SYSTEM,
        TARGET
    }
}
