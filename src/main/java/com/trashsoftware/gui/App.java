package com.trashsoftware.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.*;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.trashsoftware.physics.*;
import de.lessvoid.nifty.Nifty;

import java.util.*;

public class App extends SimpleApplication {
    private static App instance;

    // Load the font for the labels
    protected BitmapFont font;

    private boolean leftButtonPressed = false;
    private boolean rightButtonPressed = false;
    private boolean middleButtonPressed = false;
    private float horizontalSpeed = 50.0f;
    private float verticalSpeed = 50.0f;
    private float rotationSpeed = 3.0f;
    private float zoomSpeed = 2f; // Speed of zooming in/out
    private Vector3f pivotPoint = Vector3f.ZERO;  // Assuming the object is at the origin

    boolean initialized = false;

    protected Simulator simulator;
    protected double speed = 1.0;
    protected boolean playing = true;
    private float scale = 1.0f;
    private float centerX, centerY;
    private double refOffsetX, refOffsetY;
    private double focusingLastX, focusingLastY;

    //    private final Map<CelestialObject, Geometry> lineGeometries = new HashMap<>();
    private final Map<CelestialObject, ObjectModel> modelMap = new HashMap<>();
    //    private List<Geometry> tempGeom = new ArrayList<>();
//    private Node rootLabelNode = new Node("RootLabelNode");
    private boolean showLabel = true;
    private CelestialObject focusing;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void simpleInitApp() {
        instance = this;
        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        // Setup Nifty GUI
        NiftyJmeDisplay niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();

        // Load the layout and the controller
        nifty.fromXml("com/trashsoftware/gui/mainView.xml", "start", new MainViewController(this));

        // Attach the nifty display to the gui view port as a processor
        guiViewPort.addProcessor(niftyDisplay);

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
        drawFullPaths();
        drawOrbits();

//        drawNameTexts();
//        System.out.println(tpf * 1000);
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

            // Create the material for the curve
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", om.getColor());

//            // Initialize the geometry for the curve (we'll reuse this each frame)
//            Geometry curveGeom = new Geometry("DynamicCurve" + object.getName(), new Mesh());
//            curveGeom.setMaterial(mat);

//            lineGeometries.put(object, curveGeom);
            rootNode.attachChild(om.path);
            rootNode.attachChild(om.orbit);
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
        cam.setFrustumFar(10000f);
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

//        // Add a mouse click listener
//        inputManager.addMapping("Select", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
//        inputManager.addListener(clickListener, "Select");
    }

    private void putTestBox() {
        // Create a box shape
        Sphere sphereMesh = new Sphere(32, 32, 2);
        Geometry geom = new Geometry("Planet", sphereMesh);

        // Create a material for the box
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture = assetManager.loadTexture("com/trashsoftware/textures/earthmap1k.jpg");
        mat.setTexture("ColorMap", texture);
//        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        // Attach the box to the root node to make it visible
        rootNode.attachChild(geom);
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
                        if (spatial instanceof Node objectNode) {

                            // Check each child Geometry in the Node
                            for (Spatial child : objectNode.getChildren()) {
                                if (child instanceof Geometry geom) {

                                    // Only check collision for the sphere geometry
                                    if (geom.getMesh() instanceof Sphere) {
                                        geom.collideWith(ray, results);
                                    }
                                }
                            }
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
//            if (leftButtonPressed) {
//                System.out.println(name + " " + value + " " + tpf);
//            }
            if (leftButtonPressed) {
                if (name.equals("MouseMoveX-")) {
                    // Move the camera horizontally when dragging the left button
//                    Vector3f left = cam.getLeft().mult(-horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
                    centerX += horizontalSpeed * value;
                } else if (name.equals("MouseMoveX+")) {
                    // Move the camera horizontally when dragging the left button
//                    Vector3f left = cam.getLeft().mult(horizontalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(left));
                    centerX -= horizontalSpeed * value;
                } else if (name.equals("MouseMoveY-")) {
                    // Adjust the viewing angle vertically when dragging the right button
//                    Vector3f up = cam.getUp().mult(verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
                    centerY += horizontalSpeed * value;
                } else if (name.equals("MouseMoveY+")) {
                    // Adjust the viewing angle vertically when dragging the right button
//                    Vector3f up = cam.getUp().mult(-verticalSpeed * value);
//                    cam.setLocation(cam.getLocation().add(up));
                    centerY -= horizontalSpeed * value;
                }

            }
            if (rightButtonPressed) {
                float rotationAmount = rotationSpeed * value;
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
//                else if (name.equals("MouseMoveY-")) {
//                    rotateAroundPivot(rotationAmount, cam.getLeft());
//                } else if (name.equals("MouseMoveY+")) {
//                    rotateAroundPivot(-rotationAmount, cam.getLeft());
//                }
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
        Vector3f direction = cam.getLocation().subtract(pivotPoint);

        // Apply the rotation to the direction vector
        direction = rotation.mult(direction);

        // Calculate the new camera position based on the rotated direction
        Vector3f newCamPos = pivotPoint.add(direction);

        // Calculate the new up vector for the camera
        Vector3f newUp = cam.getUp();
        if (axis.equals(Vector3f.UNIT_Y)) {
            // For horizontal rotation, the up vector should remain consistent
            newUp = rotation.mult(newUp);
        } else {
            // For vertical rotation, the up vector is recalculated to avoid flipping
            newUp = axis.cross(direction).normalizeLocal();
        }

        // Set the new camera location and update its orientation
        cam.setLocation(newCamPos);
        cam.lookAt(pivotPoint, newUp);

//        System.out.println(cam.getLocation());
    }

    public void zoomInAction() {
        scale /= 0.8f;
        centerX /= 0.8f;
        centerY /= 0.8f;

        updateLabelShowing();

//        for (CelestialObject object : simulator.getObjects()) {
//            object.setScale(scale);
//        }
    }

    public void zoomOutAction() {
        scale *= 0.8f;
        centerX *= 0.8f;
        centerY *= 0.8f;

        updateLabelShowing();

//        for (CelestialObject object : simulator.getObjects()) {
//            object.setScale(scale);
//        }
    }

    // Method to handle the geometry click event
    private void onGeometryClicked(Geometry geom) {
        System.out.println("Clicked on: " + geom.getName());
        
        for (ObjectModel objectModel : modelMap.values()) {
            CelestialObject object = objectModel.object;
            if (object.isExist()) {
                if (object.getName().equals(geom.getName())) {
                    focusOn(object);
                }
            }
        }

//        // Change color of the clicked geometry
//        Material mat = geom.getMaterial();
//        mat.setColor("Color", ColorRGBA.Red);
    }
    
    private void focusOn(CelestialObject object) {
        System.out.println("Focused on " + object.getName());

        focusing = object;
        focusingLastX = focusing.getX() - refOffsetX;
        focusingLastY = focusing.getY() - refOffsetY;
        centerX = (float) (focusingLastX * scale);
        centerY = (float) (focusingLastY * scale);
    }

    private void moveScreenWithFocus() {
        double deltaX = (focusing.getX() - refOffsetX) - focusingLastX;
        double deltaY = (focusing.getY() - refOffsetY) - focusingLastY;

        centerX += (float) (deltaX * scale);
        centerY += (float) (deltaY * scale);

        focusingLastX = focusing.getX() - refOffsetX;
        focusingLastY = focusing.getY() - refOffsetY;
    }

    public float paneX(double realX) {
        return (float) ((realX - refOffsetX) * scale - centerX);
    }

    public float paneY(double realY) {
        return (float) ((realY - refOffsetY) * scale - centerY);
    }

    public float paneZ(double realZ) {
        return (float) (realZ * scale);
    }

    public double realXFromPane(float paneX) {
        return (paneX + centerX) / scale + refOffsetX;
    }

    public double realYFromPane(float paneY) {
        return (paneY + centerY) / scale + refOffsetY;
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
//                OrbitalElements specs = OrbitCalculator.computeOrbitSpecsCorrect(child,
//                        velocity,
//                        barycenter,
//                        child.getMass() + parent.getMass(),
//                        simulator.getG());
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

    }

    private void drawFullPaths() {
//        tempGeom.clear();
        for (Map.Entry<CelestialObject, Deque<double[]>> entry : simulator.getRecentPaths().entrySet()) {
            var obj = entry.getKey();
//            if (obj.getMass() < minimumMassShowing) continue;
            var path = entry.getValue();
//            Geometry geometry = lineGeometries.get(obj);
            if (obj == null) continue;
            
            ObjectModel om = modelMap.get(obj);

            int numPoints = path.size();

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
            drawPolyLine(vertices, om.getColor(), lineGeom);
        }
    }

    private void drawPolyLine(Vector3f[] vertices, ColorRGBA colorRGBA, Geometry lineGeom) {
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
            float labelX = paneX(obj.getX());
            float labelY = paneY(obj.getY());
            
            float[] canvasPos = new float[]{labelX, labelY};
            if (canLabel(drawnObjectPoses, canvasPos)) {
                om.setShowLabel(true);
                drawnObjectPoses.add(canvasPos);
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

        loadObjectsToView();
    }
}
