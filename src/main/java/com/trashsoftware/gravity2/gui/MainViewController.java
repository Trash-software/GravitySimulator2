package com.trashsoftware.gravity2.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
//import de.lessvoid.nifty.builder.CheckboxBuilder;

import javax.annotation.Nonnull;

public class MainViewController implements ScreenController {
    private Nifty nifty;
    private Screen screen;
    private JmeApp jmeApp;
    
    private Label speedLabel;
    private Label playPauseText;
    
    public MainViewController(JmeApp jmeApp) {
        this.jmeApp = jmeApp;
    }
    
    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
//        Label infoLabel = screen.findNiftyControl("infoLabel", Label.class);
//        if (infoLabel != null) {
//            infoLabel.setText("Updated Info Pane Text");
//        }
        
        speedLabel = screen.findNiftyControl("speedLabel", Label.class);
        playPauseText = screen.findNiftyControl("playPauseText", Label.class);
        System.out.println(playPauseText);
    }

    @Override
    public void onEndScreen() {

    }
    
    private void setSpeed() {
        jmeApp.simulator.setTimeStep(jmeApp.speed);
        speedLabel.setText(jmeApp.speed + "x");
    }

    public void speedUpAction() {
        jmeApp.speed *= 2;
        setSpeed();
    }

    public void speedDownAction() {
        jmeApp.speed /= 2;
        setSpeed();
    }
    
    public void playPauseAction() {
        System.out.println("play");
        jmeApp.playing = !jmeApp.playing;
        if (jmeApp.playing) {
            playPauseText.setText("||");
        } else {
            playPauseText.setText(">");
        }
    }
    
}
