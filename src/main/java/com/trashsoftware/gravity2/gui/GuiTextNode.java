package com.trashsoftware.gravity2.gui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

public class GuiTextNode extends Node {
    protected BitmapText text;
    protected JmeApp jmeApp;

    public GuiTextNode(JmeApp jmeApp) {
        this.jmeApp = jmeApp;
        
        createMesh();
    }

    private void createMesh() {
        // Create a bitmap text for the compass number/label
        BitmapFont font = jmeApp.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        text = new BitmapText(font);
        text.setText("α");
        text.setColor(ColorRGBA.White);
        
        attachChild(text);
    }
    
    public void setText(String string) {
        text.setText(string);
    }
}
