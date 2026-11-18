package com.periomeasure.ui;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.UIManager;

import com.periomeasure.io.Settings;

/**
 * Opens the single PerioMeasure window: from the Fiji menu (inside Fiji)
 * or stand-alone from {@code PerioMeasure.bat} (Fiji's Java + ImageJ, no
 * Fiji window). Only one window is open at a time.
 *
 * <p>{@code --selftest <reportFile>} runs {@link SelfTest} and exits (0 = pass).
 */
public final class PerioMeasureApp {

    private static MainWindow instance;

    private PerioMeasureApp() {
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // keep the default look and feel
        }
        if (args.length >= 2 && "--selftest".equals(args[0])) {
            System.exit(SelfTest.run(args[1]) ? 0 : 1);
            return;
        }
        EventQueue.invokeLater(() -> open(true));
    }

    /** Must run on the event dispatch thread. */
    static MainWindow open(boolean standalone) {
        if (instance != null && instance.isDisplayable()) {
            instance.toFront();
            return instance;
        }
        Settings settings = Settings.load(new ImageJPrefsStore());
        File local = new File(new File(System.getProperty("user.home"), "PerioMeasure"), "output");
        AppModel model = new AppModel(settings, System.getenv(), local);
        instance = new MainWindow(model, standalone, System.getenv());
        instance.setVisible(true);
        return instance;
    }

    /** Opens (or brings forward) the window from any thread. */
    static MainWindow openFromAnyThread(boolean standalone) {
        if (EventQueue.isDispatchThread()) {
            return open(standalone);
        }
        MainWindow[] w = new MainWindow[1];
        try {
            EventQueue.invokeAndWait(() -> w[0] = open(standalone));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
        return w[0];
    }

    /** Applies settings changed from the Fiji menu to an open window. */
    static void settingsChanged(Settings s) {
        EventQueue.invokeLater(() -> {
            if (instance != null && instance.isDisplayable()) {
                instance.applySettings(s);
            }
        });
    }
}
