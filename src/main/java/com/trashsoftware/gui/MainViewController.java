package com.trashsoftware.gui;

import com.jme3.math.ColorRGBA;
import com.trashsoftware.physics.CelestialObject;
import com.trashsoftware.physics.Simulator;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import org.w3c.dom.Text;
//import de.lessvoid.nifty.builder.CheckboxBuilder;

import javax.annotation.Nonnull;

public class MainViewController implements ScreenController {
    private Nifty nifty;
    private Screen screen;
    private App app;
    
    private Label speedLabel;
    private Label playPauseText;
    
    public MainViewController(App app) {
        this.app = app;
    }
    
    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
        Label infoLabel = screen.findNiftyControl("infoLabel", Label.class);
        if (infoLabel != null) {
            infoLabel.setText("Updated Info Pane Text");
        }
        
        speedLabel = screen.findNiftyControl("speedLabel", Label.class);
        playPauseText = screen.findNiftyControl("playPauseText", Label.class);
        System.out.println(playPauseText);
    }

    @Override
    public void onEndScreen() {

    }
    
    private void setSpeed() {
        app.simulator.setTimeStep(app.speed);
        speedLabel.setText(app.speed + "x");
    }

    public void speedUpAction() {
        app.speed *= 2;
        setSpeed();
    }

    public void speedDownAction() {
        app.speed /= 2;
        setSpeed();
    }
    
    public void playPauseAction() {
        System.out.println("play");
        app.playing = !app.playing;
        if (app.playing) {
            playPauseText.setText("||");
        } else {
            playPauseText.setText(">");
        }
    }
    
}
