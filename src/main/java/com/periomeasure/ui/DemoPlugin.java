package com.periomeasure.ui;

import java.awt.EventQueue;

import ij.plugin.PlugIn;

/** Plugins &gt; PerioMeasure &gt; Demo... — opens the window with demo face 1 (true values known). */
public class DemoPlugin implements PlugIn {

    @Override
    public void run(String arg) {
        MainWindow w = PerioMeasureApp.openFromAnyThread(false);
        if (w != null) {
            EventQueue.invokeLater(() -> w.loadDemo(0));
        }
    }
}
