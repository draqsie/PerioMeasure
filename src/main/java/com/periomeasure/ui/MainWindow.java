package com.periomeasure.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.periomeasure.Version;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.Formats;
import com.periomeasure.io.Settings;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.OptionalLandmark;
import com.periomeasure.session.SessionSetup;
import com.periomeasure.synthetic.GroundTruth;
import com.periomeasure.synthetic.SyntheticImageTool;
import com.periomeasure.synthetic.SyntheticRenderer;
import com.periomeasure.synthetic.SyntheticScene;

import ij.Prefs;

/**
 * The single PerioMeasure window: toolbar on top, the large photo in the
 * middle, the guidance / form / results panel on the right, a status line
 * at the bottom. Photos can be opened with the file chooser (starting in the
 * Pictures folder) or dropped onto the window. All state lives in
 * {@link AppModel}; this class only wires Swing to it.
 */
final class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String PREF_LAST_DIR = "periomeasure.lastDir";
    static final String[] DEMO_SCENES = {"01_typical_frontal", "02_typical_roll7_oblique", "03_right_ptosis",
        "04_asymmetric_mirrored"};
    private static final Msg[] DEMO_LABELS = {Msg.DEMO_SCENE_1, Msg.DEMO_SCENE_2, Msg.DEMO_SCENE_3, Msg.DEMO_SCENE_4};

    private final transient AppModel model;
    private final transient Map<String, String> env;
    private final boolean standalone;
    private final ImageViewer viewer;
    private final LoupeView loupe;
    private final JPanel side = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel(" ");
    private JScrollPane sideScroll;
    private JComponent card;
    private transient AppModel.Phase shownPhase;
    private SetupPanel setupPanel;
    private StepsPanel stepsPanel;
    private transient java.time.LocalDate pendingDate;
    private String photoName = "";

    MainWindow(AppModel model, boolean standalone, Map<String, String> env) {
        this.model = model;
        this.env = env;
        this.standalone = standalone;
        this.viewer = new ImageViewer(this);
        this.loupe = new LoupeView(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmDiscard()) {
                    dispose();
                    if (MainWindow.this.standalone) {
                        System.exit(0);
                    }
                }
            }
        });
        TransferHandler drop = new FileDrop();
        setTransferHandler(drop);
        viewer.setTransferHandler(drop);
        bindKeys();
        buildChrome();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1500, screen.width - 40), Math.min(950, screen.height - 60));
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        refresh();
    }

    // ------------------------------------------------------------------ accessors

    AppModel model() {
        return model;
    }

    ImageViewer viewer() {
        return viewer;
    }

    LoupeView loupe() {
        return loupe;
    }

    SetupPanel setupPanel() {
        return setupPanel;
    }

    StepsPanel stepsPanel() {
        return stepsPanel;
    }

    private Language lang() {
        return model.settings().language;
    }

    private String m(Msg msg) {
        return Messages.get(lang(), msg);
    }

    // ------------------------------------------------------------------ layout

    private void buildChrome() {
        getContentPane().removeAll();
        setTitle(m(Msg.APP_TITLE) + " " + Version.get());
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(Ui.button(m(Msg.TB_OPEN), e -> openPhoto()));
        JButton demo = Ui.button(m(Msg.TB_DEMO), null);
        demo.addActionListener(e -> demoMenu().show(demo, 0, demo.getHeight()));
        tb.add(demo);
        tb.addSeparator();
        tb.add(Ui.button(m(Msg.TB_FIT), e -> viewer.fit()));
        tb.add(Ui.button(m(Msg.TB_ZOOM_IN), e -> viewer.zoom(1.5)));
        tb.add(Ui.button(m(Msg.TB_ZOOM_OUT), e -> viewer.zoom(1 / 1.5)));
        tb.addSeparator();
        tb.add(Ui.button(m(Msg.TB_SETTINGS), e -> openSettings()));
        tb.add(Ui.button(m(Msg.TB_ABOUT), e -> JOptionPane.showMessageDialog(this,
                Messages.get(lang(), Msg.ABOUT_BODY, Version.get()), m(Msg.ABOUT_TITLE),
                JOptionPane.INFORMATION_MESSAGE)));

        sideScroll = new JScrollPane(side, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, java.awt.Color.LIGHT_GRAY));
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        sideScroll.setPreferredSize(new Dimension(420, 600));

        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tb, BorderLayout.NORTH);
        getContentPane().add(viewer, BorderLayout.CENTER);
        getContentPane().add(sideScroll, BorderLayout.EAST);
        getContentPane().add(status, BorderLayout.SOUTH);
        shownPhase = null;
        stepsPanel = null;
        revalidate();
    }

    private void bindKeys() {
        JComponent root = getRootPane();
        bind(root, "control Z", "undo", () -> {
            model.undo();
            refresh();
        });
        bind(root, "control O", "open", this::openPhoto);
        bind(root, "control EQUALS", "zoomIn", () -> viewer.zoom(1.5));
        bind(root, "control ADD", "zoomIn2", () -> viewer.zoom(1.5));
        bind(root, "control MINUS", "zoomOut", () -> viewer.zoom(1 / 1.5));
        bind(root, "control SUBTRACT", "zoomOut2", () -> viewer.zoom(1 / 1.5));
        bind(root, "control 0", "fit", viewer::fit);
    }

    private static void bind(JComponent c, String key, String name, Runnable r) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    JPopupMenu demoMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < DEMO_LABELS.length; i++) {
            final int k = i;
            JMenuItem item = new JMenuItem(m(DEMO_LABELS[i]));
            item.addActionListener(e -> loadDemo(k));
            menu.add(item);
        }
        return menu;
    }

    /** Re-reads the model: switches the right-hand card on phase changes and repaints. */
    void refresh() {
        AppModel.Phase p = model.phase();
        if (p != shownPhase || card == null) {
            JComponent next;
            switch (p) {
                case SETUP:
                    setupPanel = new SetupPanel(this, photoName, pendingDate);
                    next = setupPanel;
                    break;
                case MEASURING:
                    stepsPanel = new StepsPanel(this);
                    next = stepsPanel;
                    break;
                case RESULTS:
                    next = new ResultsPanel(this, model.lastOutcome());
                    break;
                default:
                    next = new WelcomePanel(this);
            }
            side.removeAll();
            side.add(next, BorderLayout.NORTH);
            card = next;
            shownPhase = p;
            sideScroll.setPreferredSize(new Dimension(p == AppModel.Phase.RESULTS ? 660 : 420, 600));
            getContentPane().revalidate();
        } else if (p == AppModel.Phase.MEASURING && stepsPanel != null) {
            stepsPanel.update();
        }
        refreshViews();
        updateStatus();
    }

    void refreshViews() {
        viewer.repaint();
        loupe.repaint();
    }

    void hoverChanged() {
        loupe.repaint();
        updateStatus();
    }

    private void updateStatus() {
        ViewState v = viewer.view();
        if (v == null) {
            status.setText(" ");
            return;
        }
        String pct = Formats.number(v.scale * 100, 0, lang());
        String x = "–";
        String y = "–";
        if (viewer.hover() != null) {
            x = Formats.number(viewer.hover().x, 1, lang());
            y = Formats.number(viewer.hover().y, 1, lang());
        }
        status.setText(Messages.get(lang(), Msg.STATUS, pct, x, y));
    }

    void placed(AppModel.PlaceResult r) {
        if (r == AppModel.PlaceResult.FULL) {
            Toolkit.getDefaultToolkit().beep();
        }
        refresh();
    }

    void finish() {
        AppModel.Outcome o = model.finish(LocalDateTime.now());
        if (!o.ok) {
            showError(Messages.get(lang(), o.record == null ? Msg.ERR_COMPUTE : Msg.ERR_SAVE, o.error));
        }
        refresh();
    }

    void showError(String message) {
        JTextArea t = Ui.text(message);
        t.setColumns(42);
        JOptionPane.showMessageDialog(this, t, m(Msg.ERR_TITLE), JOptionPane.ERROR_MESSAGE);
    }

    /** @return true if nothing would be lost, or the user agreed to discard the marked points */
    boolean confirmDiscard() {
        if (model.phase() != AppModel.Phase.MEASURING || model.session() == null) {
            return true;
        }
        boolean anyPoint = false;
        for (int i = 0; i < model.session().steps().size(); i++) {
            anyPoint |= !model.session().points(i).isEmpty();
        }
        return !anyPoint || StepsPanel.confirm(this, lang(), m(Msg.CONFIRM_DISCARD));
    }

    // ------------------------------------------------------------------ opening images

    private File startDir() {
        String last = Prefs.get(PREF_LAST_DIR, "");
        if (!last.isEmpty() && new File(last).isDirectory()) {
            return new File(last);
        }
        File pictures = new File(System.getProperty("user.home"), "Pictures");
        return pictures.isDirectory() ? pictures : new File(System.getProperty("user.home"));
    }

    void openPhoto() {
        if (!confirmDiscard()) {
            return;
        }
        JFileChooser ch = new JFileChooser(startDir());
        ch.setDialogTitle(m(Msg.CHOOSER_TITLE));
        ch.setFileFilter(new FileNameExtensionFilter(m(Msg.CHOOSER_FILTER), "jpg", "jpeg", "png", "tif", "tiff",
                "bmp", "gif", "heic", "heif"));
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(ch.getSelectedFile(), false);
        }
    }

    /** Loads a photo in the background (large photos take a moment). */
    void loadFile(File f, boolean alreadyConfirmed) {
        if (!alreadyConfirmed && !confirmDiscard()) {
            return;
        }
        if (f.getAbsoluteFile().getParentFile() != null) {
            Prefs.set(PREF_LAST_DIR, f.getAbsoluteFile().getParent());
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        status.setText(m(Msg.LOADING));
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                PhotoLoader.Loaded l = PhotoLoader.load(f, env);
                return new Object[] {l, new ImagePyramid(l.image)};
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Object[] r = get();
                    PhotoLoader.Loaded l = (PhotoLoader.Loaded) r[0];
                    install((ImagePyramid) r[1], l.file, f.getName(), l.warnings, l.dateTaken, null, null, null);
                } catch (ExecutionException e) {
                    Throwable c = e.getCause();
                    if (c instanceof PhotoLoader.LoadException && ((PhotoLoader.LoadException) c).heic) {
                        showError(m(Msg.ERR_HEIC));
                    } else {
                        showError(Messages.get(lang(), Msg.ERR_UNSUPPORTED, f.getAbsolutePath()));
                    }
                    refresh();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Uses an image that is already in memory (e.g. open in Fiji without a file). */
    void loadImage(BufferedImage img, String name) {
        if (!confirmDiscard()) {
            return;
        }
        PhotoLoader.Loaded l = PhotoLoader.fromImage(img, env);
        install(new ImagePyramid(l.image), null, name, l.warnings, null, null, null, null);
    }

    /** Opens demo face {@code index} and starts measuring at once. */
    void loadDemo(int index) {
        if (!confirmDiscard()) {
            return;
        }
        SyntheticScene scene = SyntheticImageTool.standardScenes().get(DEMO_SCENES[index]);
        BufferedImage img = SyntheticRenderer.render(scene);
        SessionSetup setup = new SessionSetup("DEMO", LocalDate.now(), Timing.PRE_OP, EyeSelection.BOTH,
                AxisMode.IMAGE_HORIZONTAL, EnumSet.noneOf(OptionalLandmark.class));
        File demoOut = new File(new File(System.getProperty("user.home"), "PerioMeasure"), "demo-output");
        install(new ImagePyramid(img), null, m(DEMO_LABELS[index]), Collections.<Warning>emptyList(), null,
                GroundTruth.of(scene.face), demoOut, setup);
    }

    private void install(ImagePyramid p, File file, String name, List<Warning> warnings, LocalDate dateTaken,
            GroundTruth truth, File outputOverride, SessionSetup autoStart) {
        photoName = name;
        pendingDate = dateTaken;
        viewer.setImage(p);
        model.imageLoaded(file, p.width(), p.height(), warnings, truth, outputOverride);
        if (autoStart != null) {
            model.start(autoStart);
        }
        shownPhase = null;
        refresh();
    }

    private void openSettings() {
        Settings s = SettingsDialog.show(model.settings());
        if (s != null) {
            applySettings(s);
        }
    }

    void applySettings(Settings s) {
        model.setSettings(s);
        buildChrome();
        refresh();
    }

    /** Accepts photo files dropped onto the window. */
    private final class FileDrop extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            try {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (files.isEmpty()) {
                    return false;
                }
                File f = files.get(0);
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (f.isDirectory() || n.endsWith(".csv") || n.endsWith(".txt")) {
                    return false;
                }
                loadFile(f, false);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
