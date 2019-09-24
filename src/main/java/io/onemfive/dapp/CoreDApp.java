package io.onemfive.dapp;

import io.onemfive.core.Config;
import io.onemfive.core.OneMFiveAppContext;
import io.onemfive.core.client.Client;
import io.onemfive.core.client.ClientAppManager;
import io.onemfive.core.client.ClientStatusListener;
import io.onemfive.core.util.BrowserUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *  This is the class called by the 1m5.sh script on linux.
 *
 *  Not recommended for embedded use, get your own context instance and client.
 *
 * @author objectorange
 */
public class CoreDApp {

    private static final Logger LOG = Logger.getLogger(CoreDApp.class.getName());

    public enum Status {Shutdown, Initializing, Initialized, Starting, Running, ShuttingDown, Errored, Exiting}

    private static final CoreDApp launcher = new CoreDApp();

    static OneMFiveAppContext oneMFiveAppContext;
    private ClientAppManager manager;
    private ClientAppManager.Status clientAppManagerStatus;
    private Client client;

    private static Properties config;
    private static boolean waiting = true;
    private static boolean running = false;
    private static Scanner scanner;
    private static Status status = Status.Shutdown;
    private static boolean useHTMLUI = false;
    private static boolean useTray = false;
    private static Tray tray;
    private static int uiPort;

    public static void main(String args[]) {
        try {
            init(args);
        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
            System.exit(-1);
        }
    }

    public static void init(String[] args) throws Exception {
        System.out.println("Welcome to 1M5 DApp. Initializing 1M5 Core...");
        status = Status.Initializing;
        Properties config = new Properties();
        String[] parts;
        for(String arg : args) {
            parts = arg.split("=");
            config.setProperty(parts[0],parts[1]);
        }
        try {
            config = Config.loadFromClasspath("1m5-dapp.config", config, false);
        } catch (Exception e) {
            LOG.warning(e.getLocalizedMessage());
        }

        // Launch Tray
        useTray = Boolean.parseBoolean(config.getProperty("tray"));
        if(useTray) {
            tray = new Tray();
        }

        // UI port
        useHTMLUI = Boolean.parseBoolean(config.getProperty("1m5.sensors.clearnet.server"));
        if(useHTMLUI) {
            String clConfig = config.getProperty("1m5.sensors.clearnet.server.config");
            if (clConfig != null) {
                String[] clConfigParams = clConfig.split(",");
                String uiPortStr = clConfigParams[2];
                uiPort = Integer.parseInt(uiPortStr);
                LOG.info("UI Port: " + uiPortStr);
            }
        }

        if(useTray) {
            tray.start(launcher);
            tray.updateStatus("Initialized");
        }

        status = Status.Initialized;
        launcher.start();
    }

    public void start() {
        try {
            status = Status.Starting;
            launcher.launch();
            running = true;
            if(useTray) {
                tray.updateStatus("Running");
            }
            status = Status.Running;
            // Check periodically to see if 1M5 stopped
            while (launcher.clientAppManagerStatus != ClientAppManager.Status.STOPPED && running) {
                launcher.waitABit(2 * 1000);
            }
            if(oneMFiveAppContext.getServiceBus().gracefulShutdown()) {
                status = Status.Shutdown;
                if(useTray) {
                    tray.updateStatus("Stopped");
                }
                LOG.info("1M5 DApp Stopped.");
            } else {
                status = Status.Errored;
                if(useTray) {
                    tray.updateStatus("Error");
                }
                System.out.println("1M5 DApp Errored on Shutdown.");
            }
            OneMFiveAppContext.clearGlobalContext(); // Make sure we don't use the old context when restarting
        } catch (Exception e) {
            LOG.severe(e.getLocalizedMessage());
            System.exit(-1);
        }
    }

    public void shutdown() {
        status = Status.ShuttingDown;
        System.out.println("1M5 DApp Shutting Down...");
        running = false;
    }

    public void exit() {
        System.out.println("1M5 DApp Exiting...");
        status = Status.Exiting;
        running = false;
        waiting = false;
        System.exit(0);
    }

    public void launchUI() {
        BrowserUtil.launch("http://127.0.0.1:"+uiPort+"/");
    }

    private void launch() throws Exception {
        // Getting ClientAppManager starts 1M5 Bus
        oneMFiveAppContext = OneMFiveAppContext.getInstance(config);
        // Starts 1M5 Service Bus
        manager = oneMFiveAppContext.getClientAppManager(config);
        manager.setShutdownOnLastUnregister(false);
        client = manager.getClient(true);

        ClientStatusListener clientStatusListener = new ClientStatusListener() {
            @Override
            public void clientStatusChanged(ClientAppManager.Status clientStatus) {
                clientAppManagerStatus = clientStatus;
                LOG.info("Client Status changed: "+clientStatus.name());
                switch(clientAppManagerStatus) {
                    case INITIALIZING: {
                        LOG.info("Bus reports initializing...");
                        break;
                    }
                    case READY: {
                        LOG.info("Bus reports ready.");
                        break;
                    }
                    case STOPPING: {
                        LOG.info("Bus reports stopping...");
                        break;
                    }
                    case STOPPED: {
                        LOG.info("Bus reports it stopped.");
                        break;
                    }
                }
            }
        };
        client.registerClientStatusListener(clientStatusListener);
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                manager.unregister(client);
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        tray.updateStatus("Running");
    }

    private static void waitABit(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {}
    }

    private static boolean loadLoggingProperties(Properties p) {
        String logPropsPathStr = p.getProperty("java.util.logging.config.file");
        if(logPropsPathStr != null) {
            File logPropsPathFile = new File(logPropsPathStr);
            if(logPropsPathFile.exists()) {
                try {
                    FileInputStream logPropsPath = new FileInputStream(logPropsPathFile);
                    LogManager.getLogManager().readConfiguration(logPropsPath);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
