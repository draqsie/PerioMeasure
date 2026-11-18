package com.periomeasure.ui;

import java.awt.EventQueue;
import java.io.File;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.plugin.PlugIn;

/**
 * Plugins &gt; PerioMeasure &gt; Measure... — opens the PerioMeasure window.
 * If an image is open in Fiji it is loaded (from its file when it has one,
 * so the EXIF orientation is honoured).
 */
public class MeasurePlugin implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        MainWindow w = PerioMeasureApp.openFromAnyThread(false, true);
        if (imp == null || w == null) {
            return;
        }
        File f = photoFile(imp);
        EventQueue.invokeLater(() -> {
            if (f != null && f.isFile()) {
                w.loadFile(f, false);
            } else {
                w.loadImage(imp.getBufferedImage(), imp.getTitle());
            }
        });
    }

    /** @return the file the image was opened from, or null if it was never saved */
    static File photoFile(ImagePlus imp) {
        FileInfo fi = imp.getOriginalFileInfo();
        if (fi == null || fi.directory == null || fi.fileName == null || fi.fileName.isEmpty()) {
            return null;
        }
        return new File(fi.directory, fi.fileName);
    }
}
