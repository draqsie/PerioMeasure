package com.periomeasure.synthetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.periomeasure.core.Ellipse;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.Vec2;

/**
 * A {@link FaceModel} placed in an image: face frame (mm, Y up) -> optional
 * left-right mirror -> image orientation (y down) -> roll rotation -> scale
 * (1 / mmPerPx) -> translation. The forehead sticker is drawn as an ellipse
 * whose MAJOR axis keeps the true diameter (oblique view foreshortens only
 * the minor axis, which is placed along the face horizontal).
 */
public final class SyntheticScene {

    private static final double MARGIN_MM = 15;

    public final FaceModel face;
    public final double mmPerPx;
    public final double rollDeg;
    public final boolean mirrored;
    public final double stickerAxisRatio;
    public final int width;
    public final int height;
    private final Vec2 offset;

    private SyntheticScene(FaceModel face, double mmPerPx, double rollDeg, boolean mirrored, double ratio) {
        this.face = face;
        this.mmPerPx = mmPerPx;
        this.rollDeg = rollDeg;
        this.mirrored = mirrored;
        this.stickerAxisRatio = ratio;
        // Bounding box of everything that is drawn, in the untranslated image frame.
        List<Vec2> pts = new ArrayList<>();
        for (Eye eye : Eye.values()) {
            EyeLandmarks e = face.eye(eye);
            pts.addAll(e.contour);
            pts.add(e.browLowerBorder.plus(new Vec2(-18, 6)));
            pts.add(e.browLowerBorder.plus(new Vec2(18, 6)));
            pts.add(e.pupil.plus(new Vec2(0, -FaceModel.LIMBUS_RADIUS_MM)));
        }
        double r = face.stickerDiameterMm / 2;
        for (int k = 0; k < 4; k++) {
            double t = k * Math.PI / 2;
            pts.add(face.stickerCenterMm.plus(new Vec2(r * Math.cos(t), r * Math.sin(t))));
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Vec2 p : pts) {
            Vec2 q = linear(p);
            minX = Math.min(minX, q.x);
            minY = Math.min(minY, q.y);
            maxX = Math.max(maxX, q.x);
            maxY = Math.max(maxY, q.y);
        }
        double margin = MARGIN_MM / mmPerPx;
        this.offset = new Vec2(margin - minX, margin - minY);
        this.width = (int) Math.ceil(maxX - minX + 2 * margin);
        this.height = (int) Math.ceil(maxY - minY + 2 * margin);
    }

    /**
     * @param mmPerPx          image scale
     * @param rollDeg          head roll in the image (degrees)
     * @param mirrored         left-right mirrored photo (patient's right eye on the image right)
     * @param stickerAxisRatio minor/major axis ratio of the sticker (1 = frontal)
     */
    public static SyntheticScene of(FaceModel face, double mmPerPx, double rollDeg, boolean mirrored,
            double stickerAxisRatio) {
        Objects.requireNonNull(face, "face");
        if (!(mmPerPx > 0) || Double.isInfinite(mmPerPx)) {
            throw new IllegalArgumentException("mmPerPx must be positive");
        }
        if (!(stickerAxisRatio > 0 && stickerAxisRatio <= 1)) {
            throw new IllegalArgumentException("Sticker axis ratio must be in (0, 1]");
        }
        return new SyntheticScene(face, mmPerPx, rollDeg, mirrored, stickerAxisRatio);
    }

    /** Face frame (mm) -> image (px) without the translation. */
    private Vec2 linear(Vec2 mm) {
        double x = mirrored ? -mm.x : mm.x;
        double y = -mm.y;
        double c = Math.cos(Math.toRadians(rollDeg));
        double s = Math.sin(Math.toRadians(rollDeg));
        return new Vec2((c * x - s * y) / mmPerPx, (s * x + c * y) / mmPerPx);
    }

    /** Face frame (mm) -> image coordinates (px). */
    public Vec2 toImage(Vec2 mm) {
        return linear(mm).plus(offset);
    }

    /** @return the eye's landmarks (including optional ones and the contour) in image px */
    public EyeLandmarks landmarksPx(Eye eye) {
        EyeLandmarks e = face.eye(eye);
        List<Vec2> contour = new ArrayList<>();
        for (Vec2 p : e.contour) {
            contour.add(toImage(p));
        }
        return EyeLandmarks.builder()
                .pupil(toImage(e.pupil))
                .upperLid(toImage(e.upperLid))
                .lowerLid(toImage(e.lowerLid))
                .medialCanthus(toImage(e.medialCanthus))
                .lateralCanthus(toImage(e.lateralCanthus))
                .upperLidCrease(toImage(e.upperLidCrease))
                .browLowerBorder(toImage(e.browLowerBorder))
                .limbusUpper(toImage(e.limbusUpper))
                .limbusLower(toImage(e.limbusLower))
                .contour(contour)
                .build();
    }

    /** @return the sticker outline in image px; major axis along the face vertical */
    public Ellipse stickerPx() {
        Vec2 c = toImage(face.stickerCenterMm);
        Vec2 up = linear(new Vec2(0, 1));
        double a = face.stickerDiameterMm / 2 / mmPerPx;
        return new Ellipse(c.x, c.y, a, a * stickerAxisRatio, Math.atan2(up.y, up.x));
    }

    /**
     * Simulated manual clicks on the sticker edge.
     *
     * @param n            number of points, evenly spread around the edge
     * @param noiseSigmaPx isotropic Gaussian click noise (px); 0 for exact points
     * @param seed         random seed (results are reproducible)
     */
    public List<Vec2> stickerEdgePoints(int n, double noiseSigmaPx, long seed) {
        Ellipse e = stickerPx();
        Random rnd = new Random(seed);
        double phase = 0.1 + 2 * Math.PI * rnd.nextDouble() / n;
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Vec2 p = e.pointAt(phase + 2 * Math.PI * i / n);
            if (noiseSigmaPx > 0) {
                p = p.plus(new Vec2(rnd.nextGaussian() * noiseSigmaPx, rnd.nextGaussian() * noiseSigmaPx));
            }
            pts.add(p);
        }
        return pts;
    }
}
