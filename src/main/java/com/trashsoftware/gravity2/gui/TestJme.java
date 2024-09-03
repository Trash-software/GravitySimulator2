package com.trashsoftware.gravity2.gui;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

public class TestJme extends SimpleApplication {
    public static void main(String[] args) {
        TestJme app = new TestJme();
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        // Create the star geometry
        Sphere starMesh = new Sphere(32, 32, 2f); // Star with radius 2
        Geometry starGeo = new Geometry("Star", starMesh);

        Texture starText = assetManager.loadTexture("com/trashsoftware/gravity2/textures/sunmap.jpg");

        // Create a glowing material for the star
        Material starMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        starMat.setBoolean("UseMaterialColors", true);
        starMat.setTexture("DiffuseMap", starText);
//        starMat.setColor("Diffuse", ColorRGBA.Yellow); // Yellow star
//        starMat.setColor("Specular", ColorRGBA.White); // Specular highlights
        starMat.setFloat("Shininess", 64f); // Shininess factor
        starMat.setColor("GlowColor", ColorRGBA.Yellow); // Glow effect for the star

        // Apply the material to the star geometry
        starGeo.setMaterial(starMat);

        // Position the star in the scene
        starGeo.setLocalTranslation(0, 0, 0);

        // Attach the star to the root node
        rootNode.attachChild(starGeo);

        // Create a point light to simulate the star's light
        PointLight starLight = new PointLight();
        starLight.setPosition(new Vector3f(0, 0, 0)); // Same position as the star
        starLight.setColor(ColorRGBA.White.mult(5)); // White light emitted by the star
        starLight.setRadius(30f); // Radius of the light's effect

        // Add the light to the root node
        rootNode.addLight(starLight);

        // Create the planet geometry
        Sphere planetMesh = new Sphere(32, 32, 1f); // Planet with radius 1
        planetMesh.setTextureMode(Sphere.TextureMode.Projected);
        Geometry planetGeo = new Geometry("Planet", planetMesh);

        // Create a material for the planet
        Texture planetText = assetManager.loadTexture("com/trashsoftware/gravity2/textures/earthmap1k.jpg");
        
        Material planetMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        planetMat.setTexture("DiffuseMap", planetText);
//        planetMat.setBoolean("UseMaterialColors", true);
//        planetMat.setColor("Ambient", ColorRGBA.White);
//        planetMat.setColor("Diffuse", ColorRGBA.Blue); // Blue planet
//        planetMat.setColor("Specular", ColorRGBA.White); // Specular highlights
//        planetMat.setFloat("Shininess", 32f); // Less shiny than the star

        // Apply the material to the planet geometry
        planetGeo.setMaterial(planetMat);

        // Position the planet in the scene
        planetGeo.setLocalTranslation(5, 5, 0); // Position the planet

        // Attach the planet to the root node
        rootNode.attachChild(planetGeo);

        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White.mult(0.1f));

        rootNode.addLight(ambientLight);

        // Add bloom effect to enhance the star's glow
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.SceneAndObjects);
        bloom.setBloomIntensity(1.5f); // Adjust intensity for more or less glow
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
        
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(0, 0, 20));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Z);
    }
}
