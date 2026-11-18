package com.periomeasure.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.Version;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.Formats;
import com.periomeasure.io.Settings;
import com.periomeasure.io.Theme;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.OptionalLandmark;
import com.periomeasure.session.SessionSetup;
import com.periomeasure.synthetic.GroundTruth;
import com.periomeasure.synthetic.SyntheticImageTool;
import com.periomeasure.synthetic.SyntheticRenderer;
import com.periomeasure.synthetic.SyntheticScene;

import ij.Prefs;

/**
 * The single PerioMeasure window: header bar on top, the large photo in the
 * middle, the guidance / form / results panel on the right, a status line at
 * the bottom, and a guided tour on first start. All state lives in
 * {@link AppModel}; this class only wires Swing to it.
 */
final class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    static final String PREF_LAST_DIR = "periomeasure.lastDir";
    static final String PREF_TOUR_DONE = "periomeasure.tourDone";
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
    private HeaderBar header;
    private JScrollPane sideScroll;
    private JComponent card;
    private transient AppModel.Phase shownPhase;
    private SetupPanel setupPanel;
    private StepsPanel stepsPanel;
    private TourOverlay tour;
    private transient LocalDate pendingDate;
    private String photoName = "";

    MainWindow(AppModel model, boolean standalone, Map<String, String> env) {
        this.model = model;
        this.env = env;
        this.standalone = standalone;
        this.viewer = new ImageViewer(this);
        this.loupe = new LoupeView(this);
        setIconImages(AppIcon.images()); // title bar, taskbar, Alt+Tab
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
        setMinimumSize(new Dimension(UIScale.scale(1100), UIScale.scale(700)));
        setSize(Math.min(UIScale.scale(1600), screen.width - 40), Math.min(UIScale.scale(1000), screen.height - 60));
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

    HeaderBar header() {
        return header;
    }

    SetupPanel setupPanel() {
        return setupPanel;
    }

    StepsPanel stepsPanel() {
        return stepsPanel;
    }

    TourOverlay tour() {
        return tour;
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
        setTitle("PerioMeasure " + Version.get());
        header = new HeaderBar(this);

        sideScroll = new JScrollPane(side, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Separator.foreground")));
        sideScroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(18));
        sideScroll.setPreferredSize(new Dimension(UIScale.scale(470), UIScale.scale(600)));

        status.setFont(status.getFont().deriveFont(Themes.font(-2)));
        status.setForeground(Themes.muted());
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(UIScale.scale(4), UIScale.scale(14), UIScale.scale(4),
                        UIScale.scale(14))));
        statusBar.add(status, BorderLayout.WEST);
        JLabel disclaimer = new JLabel(m(Msg.RES_DISCLAIMER));
        disclaimer.setFont(status.getFont());
        disclaimer.setForeground(Themes.muted());
        statusBar.add(disclaimer, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(viewer, BorderLayout.CENTER);
        getContentPane().add(sideScroll, BorderLayout.EAST);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        shownPhase = null;
        stepsPanel = null;
        card = null;
        revalidate();
    }

    private void bindKeys() {
        JComponent root = getRootPane();
        bind(root, "control Z", "undo", () -> {
            model.undo();
            refresh();
        });
        bind(root, "control O", "open", this::openPhoto);
        // Text fields keep their own Ctrl+V (focused-component bindings are tried first).
        bind(root, "control V", "paste", this::paste);
        bind(root, "shift INSERT", "paste2", this::paste);
        bind(root, "control EQUALS", "zoomIn", () -> viewer.zoom(1.5));
        bind(root, "control ADD", "zoomIn2", () -> viewer.zoom(1.5));
        bind(root, "control MINUS", "zoomOut", () -> viewer.zoom(1 / 1.5));
        bind(root, "control SUBTRACT", "zoomOut2", () -> viewer.zoom(1 / 1.5));
        bind(root, "control 0", "fit", viewer::fit);
        bind(root, "F1", "guide", this::startTour);
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
            JMenuItem item = new JMenuItem(m(DEMO_LABELS[i]), Icons.of(Icons.Kind.DEMO));
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
            sideScroll.setPreferredSize(new Dimension(UIScale.scale(p == AppModel.Phase.RESULTS ? 720 : 470),
                    UIScale.scale(600)));
            getContentPane().revalidate();
            JScrollPane sp = sideScroll;
            EventQueue.invokeLater(() -> sp.getVerticalScrollBar().setValue(0)); // after layout: start at the top
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

    void showAbout() {
        JTextArea t = Ui.text(Messages.get(lang(), Msg.ABOUT_BODY, Version.get()));
        t.setColumns(36);
        JOptionPane.showMessageDialog(this, t, m(Msg.ABOUT_TITLE), JOptionPane.INFORMATION_MESSAGE);
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

    // ------------------------------------------------------------------ language, theme, settings

    void setLanguage(Language l) {
        if (l == lang()) {
            return;
        }
        applySettings(saved(model.settings().withLanguage(l)));
    }

    void toggleTheme() {
        Theme t = Themes.dark() ? Theme.LIGHT : Theme.DARK;
        Settings s = saved(model.settings().withTheme(t));
        Themes.apply(t);
        applySettings(s);
    }

    private static Settings saved(Settings s) {
        s.save(new ImageJPrefsStore());
        Prefs.savePreferences();
        return s;
    }

    void openSettings() {
        Settings s = SettingsDialog.show(this, model.settings());
        if (s != null) {
            applySettings(s);
        }
    }

    /** Rebuilds the texts and colours (language / theme / settings changed); keeps all work. */
    void applySettings(Settings s) {
        model.setSettings(s);
        boolean tourOpen = tour != null && tour.isVisible();
        buildChrome();
        refresh();
        if (tourOpen) {
            tour.close();
            startTour();
        }
    }

    // ------------------------------------------------------------------ guided tour

    private List<TourOverlay.Stop> tourStops() {
        List<TourOverlay.Stop> s = new ArrayList<>();
        s.add(new TourOverlay.Stop(null, Msg.TOUR_1_TITLE, Msg.TOUR_1_BODY));
        s.add(new TourOverlay.Stop(() -> header.open, Msg.TOUR_2_TITLE, Msg.TOUR_2_BODY));
        s.add(new TourOverlay.Stop(() -> header.demo, Msg.TOUR_3_TITLE, Msg.TOUR_3_BODY));
        s.add(new TourOverlay.Stop(() -> viewer, Msg.TOUR_4_TITLE, Msg.TOUR_4_BODY));
        s.add(new TourOverlay.Stop(() -> sideScroll, Msg.TOUR_5_TITLE, Msg.TOUR_5_BODY));
        s.add(new TourOverlay.Stop(() -> header.viewGroup, Msg.TOUR_6_TITLE, Msg.TOUR_6_BODY));
        s.add(new TourOverlay.Stop(() -> header.language, Msg.TOUR_7_TITLE, Msg.TOUR_7_BODY));
        s.add(new TourOverlay.Stop(() -> header.theme, Msg.TOUR_8_TITLE, Msg.TOUR_8_BODY));
        s.add(new TourOverlay.Stop(() -> header.settings, Msg.TOUR_9_TITLE, Msg.TOUR_9_BODY));
        s.add(new TourOverlay.Stop(() -> header.help, Msg.TOUR_10_TITLE, Msg.TOUR_10_BODY));
        return s;
    }

    /** Opens the step-by-step guide (also with F1 or the "Guide" button). */
    void startTour() {
        tour = new TourOverlay(tourStops(), lang(), () -> {
            Prefs.set(PREF_TOUR_DONE, "true");
            Prefs.savePreferences();
        });
        setGlassPane(tour);
        tour.start();
        tour.requestFocusInWindow();
    }

    /** Shows the guide automatically the very first time PerioMeasure opens. */
    void startTourIfFirstTime() {
        if (!"true".equals(Prefs.get(PREF_TOUR_DONE, "false"))) {
            EventQueue.invokeLater(this::startTour);
        }
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

    /** Windows' own "Open" dialog (familiar, never clipped, with picture previews). */
    void openPhoto() {
        if (!confirmDiscard()) {
            return;
        }
        FileDialog fd = new FileDialog(this, m(Msg.CHOOSER_TITLE), FileDialog.LOAD);
        fd.setDirectory(startDir().getAbsolutePath());
        fd.setFile("*.jpg;*.jpeg;*.png;*.tif;*.tiff;*.bmp;*.gif;*.heic;*.heif");
        fd.setFilenameFilter((dir, name) -> ClipboardPhoto.isPhotoName(name));
        fd.setVisible(true);
        if (fd.getFile() != null) {
            loadFile(new File(fd.getDirectory(), fd.getFile()), true);
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

    /** "Paste" button / Ctrl+V: opens the photo on the system clipboard. */
    void paste() {
        Transferable t;
        try {
            t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        } catch (RuntimeException e) {
            t = null; // clipboard busy or unavailable
        }
        paste(t);
    }

    /**
     * Opens what {@code t} offers: a copied photo file is opened as the original
     * file; copied pixels are used exactly but flagged with the pasted-image
     * warning; several photos or nothing usable only explain what to do.
     */
    void paste(Transferable t) {
        open(ClipboardPhoto.read(t));
    }

    private void open(ClipboardPhoto.Content c) {
        if (c == null) {
            showError(m(Msg.ERR_PASTE_NOTHING));
        } else if (c.severalPhotos) {
            showError(m(Msg.ERR_PASTE_SEVERAL));
        } else if (c.file != null) {
            loadFile(c.file, false);
        } else {
            loadPasted(c.image);
        }
    }

    private void loadPasted(BufferedImage img) {
        if (!confirmDiscard()) {
            return;
        }
        PhotoLoader.Loaded l = PhotoLoader.fromPasted(img, env);
        install(new ImagePyramid(l.image), null, m(Msg.PASTED_NAME), l.warnings, null, null, null, null);
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

    /** Accepts a photo file (or image) dropped onto the window; same rules as pasting. */
    private final class FileDrop extends TransferHandler {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    || support.isDataFlavorSupported(DataFlavor.imageFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            // Dropped data is only readable during the drop: read it now, open it afterwards
            // so the source program is not kept waiting by any dialog.
            ClipboardPhoto.Content c = ClipboardPhoto.read(support.getTransferable());
            if (c == null) {
                return false;
            }
            EventQueue.invokeLater(() -> open(c));
            return true;
        }
    }

    /** Lets tests and the self-test reach a component by tour index. */
    Component tourTarget(int i) {
        TourOverlay.Stop s = tourStops().get(i);
        return s.target == null ? null : s.target.get();
    }
}
