package io.onemfive.dapp;


import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public class Tray {

    private CoreDApp app;
    private SystemTray systemTray;

    private MenuItem startMenuItem;
    private MenuItem restartMenuItem;
    private MenuItem stopMenuItem;
    private MenuItem uiMenuItem;
    private MenuItem quitMenuItem;

    private URL icon;
    private URL iconGreen;
    private URL iconYellow;
    private URL iconOrange;
    private URL iconRed;
    private URL iconBlue;
    private URL iconWhite;

    public void start(CoreDApp app) {
        this.app = app;
        SystemTray.SWING_UI = new UIConfig();

        systemTray = SystemTray.get();
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }
        icon = this.getClass().getClassLoader().getResource("favicon-black.png");
        iconGreen = this.getClass().getClassLoader().getResource("favicon-black-green.png");
        iconYellow = this.getClass().getClassLoader().getResource("favicon-black-yellow.png");
        iconOrange = this.getClass().getClassLoader().getResource("favicon-black-orange.png");
        iconRed = this.getClass().getClassLoader().getResource("favicon-black-red.png");
        iconBlue = this.getClass().getClassLoader().getResource("favicon-black-blue.png");
        iconWhite = this.getClass().getClassLoader().getResource("favicon-white.png");

        updateStatus("Initializing");

        // Setup Menus
        // UI
        uiMenuItem = new MenuItem("UI", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread() {
                    @Override
                    public void run() {
                        app.launchUI();
                    }
                }.start();
            }
        });
        uiMenuItem.setEnabled(false);
        systemTray.getMenu().add(uiMenuItem).setShortcut('u');

        // Start
        startMenuItem = new MenuItem("Start", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread() {
                    @Override
                    public void run() {
                        app.start();
                    }
                }.start();
            }
        });
        startMenuItem.setEnabled(true);
        systemTray.getMenu().add(startMenuItem).setShortcut('s');

        // Restart
        startMenuItem = new MenuItem("Restart", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread() {
                    @Override
                    public void run() {
                        app.shutdown();
                        app.start();
                    }
                }.start();
            }
        });
        startMenuItem.setEnabled(false);
        systemTray.getMenu().add(startMenuItem).setShortcut('r');

        // Stop
        stopMenuItem = new MenuItem("Stop", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread() {
                    @Override
                    public void run() {
                        app.shutdown();
                    }
                }.start();
            }
        });
        stopMenuItem.setEnabled(false);
        systemTray.getMenu().add(stopMenuItem).setShortcut('t');

        // Quit
        quitMenuItem = new MenuItem("Quit", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        systemTray.setImage(iconWhite);
                        updateStatus("Quitting");
                        app.shutdown();
                        systemTray.shutdown();
                        app.exit();
                    }
                }.start();
            }
        });
        quitMenuItem.setEnabled(true);
        systemTray.getMenu().add(quitMenuItem).setShortcut('q'); // case does not matter

        updateStatus("Initialized");
    }

    public void updateStatus(String status) {
        switch(status) {
            case "Initializing": {
                systemTray.setImage(icon);
                break;
            }
            case "Starting": {
                systemTray.setImage(iconYellow);
                break;
            }
            case "Warming": {
                systemTray.setImage(iconOrange);
                break;
            }
            case "Running": {
                uiMenuItem.setEnabled(true);
                systemTray.setImage(iconWhite);
                break;
            }
            case "Connected": {
                uiMenuItem.setEnabled(true);
                systemTray.setImage(iconGreen);
                break;
            }
            case "Disconnected": {
                systemTray.setImage(iconYellow);
                break;
            }
            case "Blocked": {
                systemTray.setImage(iconBlue);
                break;
            }
            case "Error": {
                systemTray.setImage(iconRed);
                break;
            }
            case "Stopping": {
                systemTray.setImage(iconYellow);
                uiMenuItem.setEnabled(false);
                break;
            }
            case "Stopped": {
                systemTray.setImage(icon);
                break;
            }
            case "Quitting": {
                uiMenuItem.setEnabled(false);
                quitMenuItem.setEnabled(false);
                systemTray.setImage(icon);
            }
        }
        systemTray.setStatus(status);
    }
}
