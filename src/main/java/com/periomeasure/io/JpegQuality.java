package com.periomeasure.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Estimates the IJG ("libjpeg") quality factor of a JPEG file from its
 * luminance quantisation table, compared with the standard table of
 * ITU-T T.81 Annex K (= quality 50).
 */
public final class JpegQuality {

    private static final String NATIVE_FORMAT = "javax_imageio_jpeg_image_1.0";

    private JpegQuality() {
    }

    /** @return estimated quality 1-100, or -1 if the file is not a readable JPEG */
    public static int estimate(File f) {
        if (f == null || !f.isFile()) {
            return -1;
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
            if (in == null) {
                return -1;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    if (!"jpeg".equalsIgnoreCase(reader.getFormatName())) {
                        continue;
                    }
                    reader.setInput(in, true, false);
                    IIOMetadata md = reader.getImageMetadata(0);
                    int[] table = luminanceTable(md.getAsTree(NATIVE_FORMAT));
                    return table == null ? -1 : fromTable(table);
                } finally {
                    reader.dispose();
                }
            }
            return -1;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    /** IJG scaling inverted: scale = 5000/q for q < 50, 200 - 2q otherwise. */
    static int fromTable(int[] table) {
        int[] std = JPEGQTable.K1Luminance.getTable();
        double sumT = 0;
        double sumS = 0;
        for (int i = 0; i < 64; i++) {
            sumT += table[i];
            sumS += std[i];
        }
        double scale = 100 * sumT / sumS;
        double q = scale <= 100 ? (200 - scale) / 2 : 5000 / scale;
        return (int) Math.max(1, Math.min(100, Math.round(q)));
    }

    private static int[] luminanceTable(Node root) {
        NodeList tables = ((IIOMetadataNode) root).getElementsByTagName("dqtable");
        for (int i = 0; i < tables.getLength(); i++) {
            IIOMetadataNode t = (IIOMetadataNode) tables.item(i);
            if ("0".equals(t.getAttribute("qtableId")) && t.getUserObject() instanceof JPEGQTable) {
                return ((JPEGQTable) t.getUserObject()).getTable();
            }
        }
        return null;
    }
}
