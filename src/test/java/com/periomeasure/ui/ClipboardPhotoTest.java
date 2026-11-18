package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClipboardPhotoTest {

    private File dir;
    private File jpg;
    private File txt;

    /** A transferable offering exactly the given flavour/value pairs. */
    private static Transferable of(Object... flavorValue) {
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                DataFlavor[] f = new DataFlavor[flavorValue.length / 2];
                for (int i = 0; i < f.length; i++) {
                    f[i] = (DataFlavor) flavorValue[2 * i];
                }
                return f;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                for (int i = 0; i < flavorValue.length; i += 2) {
                    if (flavorValue[i].equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                for (int i = 0; i < flavorValue.length; i += 2) {
                    if (flavorValue[i].equals(flavor)) {
                        return flavorValue[i + 1];
                    }
                }
                throw new UnsupportedFlavorException(flavor);
            }
        };
    }

    @BeforeEach
    void files() throws Exception {
        dir = new File("target/test-output/clipboard");
        dir.mkdirs();
        jpg = new File(dir, "P017 pre.jpg");
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "jpg", jpg);
        txt = new File(dir, "notes.txt");
        Files.write(txt.toPath(), "x".getBytes("UTF-8"));
    }

    @Test
    void copiedFileIsUsedAsTheOriginalFile() {
        ClipboardPhoto.Content c = ClipboardPhoto.read(of(DataFlavor.javaFileListFlavor, Arrays.asList(txt, jpg)));
        assertNotNull(c);
        assertEquals(jpg, c.file);
        assertNull(c.image);
    }

    @Test
    void aCopiedFileWinsOverAnImageOfIt() {
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        ClipboardPhoto.Content c = ClipboardPhoto.read(of(DataFlavor.imageFlavor, img,
                DataFlavor.javaFileListFlavor, Collections.singletonList(jpg)));
        assertEquals(jpg, c.file);
    }

    @Test
    void copiedPathTextIsUsedWhenItNamesAPhoto() {
        ClipboardPhoto.Content c = ClipboardPhoto.read(new StringSelection("  \"" + jpg.getAbsolutePath() + "\"  "));
        assertEquals(jpg.getAbsoluteFile(), c.file.getAbsoluteFile());
    }

    @Test
    void copiedImagePixelsAreKeptExactly() {
        BufferedImage img = new BufferedImage(7, 3, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(2, 1, 0xFF123456);
        img.setRGB(6, 2, 0xFFABCDEF);
        ClipboardPhoto.Content c = ClipboardPhoto.read(of(DataFlavor.imageFlavor, (Image) img));
        assertNull(c.file);
        assertEquals(7, c.image.getWidth());
        assertEquals(3, c.image.getHeight());
        assertEquals(0x123456, c.image.getRGB(2, 1) & 0xFFFFFF);
        assertEquals(0xABCDEF, c.image.getRGB(6, 2) & 0xFFFFFF);
        assertEquals(BufferedImage.TYPE_INT_RGB, c.image.getType());
    }

    @Test
    void heicFilesArePassedOnSoTheUserGetsTheExplanation() throws Exception {
        File heic = new File(dir, "IMG_0001.HEIC");
        Files.write(heic.toPath(), new byte[] {0});
        assertEquals(heic, ClipboardPhoto.read(of(DataFlavor.javaFileListFlavor, Collections.singletonList(heic))).file);
    }

    @Test
    void severalCopiedPhotosAreRefusedRatherThanGuessed() throws Exception {
        File second = new File(dir, "P017 post.jpg");
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "jpg", second);
        ClipboardPhoto.Content c = ClipboardPhoto.read(of(DataFlavor.javaFileListFlavor, Arrays.asList(jpg, txt, second)));
        assertNotNull(c);
        assertEquals(true, c.severalPhotos);
        assertNull(c.file);
        assertNull(c.image);
        ClipboardPhoto.Content lines = ClipboardPhoto.read(new StringSelection(
                "\"" + jpg.getAbsolutePath() + "\"\r\n\"" + second.getAbsolutePath() + "\""));
        assertEquals(true, lines.severalPhotos);
        assertNull(lines.file);
    }

    @Test
    void fileUriTextIsAccepted() {
        ClipboardPhoto.Content c = ClipboardPhoto.read(new StringSelection(jpg.toURI().toString()));
        assertEquals(jpg.getAbsoluteFile(), c.file.getAbsoluteFile());
    }

    @Test
    void aFailingFlavourFallsBackToTheNextOne() {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Transferable broken = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.javaFileListFlavor, DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return true;
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws java.io.IOException {
                if (flavor.equals(DataFlavor.imageFlavor)) {
                    return img;
                }
                throw new java.io.IOException("clipboard busy");
            }
        };
        ClipboardPhoto.Content c = ClipboardPhoto.read(broken);
        assertEquals(4, c.image.getWidth());
    }

    @Test
    void photoNamesAreRecognisedCaseInsensitively() {
        assertEquals(true, ClipboardPhoto.isPhotoName("A.JPEG"));
        assertEquals(true, ClipboardPhoto.isPhotoName("b.tif"));
        assertEquals(false, ClipboardPhoto.isPhotoName("c.csv"));
        assertEquals(false, ClipboardPhoto.isPhotoName("jpg"));
    }

    @Test
    void nothingUsableGivesNull() {
        assertNull(ClipboardPhoto.read(new StringSelection("hello")));
        assertNull(ClipboardPhoto.read(new StringSelection(new File(dir, "missing.jpg").getAbsolutePath())));
        assertNull(ClipboardPhoto.read(new StringSelection(txt.getAbsolutePath())));
        List<File> onlyText = Collections.singletonList(txt);
        assertNull(ClipboardPhoto.read(of(DataFlavor.javaFileListFlavor, onlyText)));
        assertNull(ClipboardPhoto.read(null));
    }
}
