package com.periomeasure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Landmarks of one eye in image coordinates (px). The five required points
 * are always present; optional points are null when not marked.
 */
public final class EyeLandmarks {

    /** Pupil centre or corneal light reflex. */
    public final Vec2 pupil;
    /** Upper lid margin on the vertical line through the pupil. */
    public final Vec2 upperLid;
    /** Lower lid margin on the vertical line through the pupil. */
    public final Vec2 lowerLid;
    public final Vec2 medialCanthus;
    public final Vec2 lateralCanthus;

    public final Vec2 upperLidCrease;
    public final Vec2 browLowerBorder;
    public final Vec2 limbusUpper;
    public final Vec2 limbusLower;
    /** Palpebral fissure contour polygon (>= 3 vertices), or null. */
    public final List<Vec2> contour;

    private EyeLandmarks(Builder b) {
        this.pupil = b.pupil;
        this.upperLid = b.upperLid;
        this.lowerLid = b.lowerLid;
        this.medialCanthus = b.medialCanthus;
        this.lateralCanthus = b.lateralCanthus;
        this.upperLidCrease = b.upperLidCrease;
        this.browLowerBorder = b.browLowerBorder;
        this.limbusUpper = b.limbusUpper;
        this.limbusLower = b.limbusLower;
        this.contour = b.contour == null ? null : Collections.unmodifiableList(new ArrayList<>(b.contour));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder; {@link #build()} checks that every required landmark is set. */
    public static final class Builder {
        private Vec2 pupil;
        private Vec2 upperLid;
        private Vec2 lowerLid;
        private Vec2 medialCanthus;
        private Vec2 lateralCanthus;
        private Vec2 upperLidCrease;
        private Vec2 browLowerBorder;
        private Vec2 limbusUpper;
        private Vec2 limbusLower;
        private List<Vec2> contour;

        private Builder() {
        }

        public Builder pupil(Vec2 p) {
            this.pupil = p;
            return this;
        }

        public Builder upperLid(Vec2 p) {
            this.upperLid = p;
            return this;
        }

        public Builder lowerLid(Vec2 p) {
            this.lowerLid = p;
            return this;
        }

        public Builder medialCanthus(Vec2 p) {
            this.medialCanthus = p;
            return this;
        }

        public Builder lateralCanthus(Vec2 p) {
            this.lateralCanthus = p;
            return this;
        }

        public Builder upperLidCrease(Vec2 p) {
            this.upperLidCrease = p;
            return this;
        }

        public Builder browLowerBorder(Vec2 p) {
            this.browLowerBorder = p;
            return this;
        }

        public Builder limbusUpper(Vec2 p) {
            this.limbusUpper = p;
            return this;
        }

        public Builder limbusLower(Vec2 p) {
            this.limbusLower = p;
            return this;
        }

        public Builder contour(List<Vec2> polygon) {
            this.contour = polygon;
            return this;
        }

        /**
         * @throws IllegalStateException    if a required landmark is missing
         * @throws IllegalArgumentException if the contour has fewer than 3 vertices
         */
        public EyeLandmarks build() {
            require(pupil, "pupil");
            require(upperLid, "upperLid");
            require(lowerLid, "lowerLid");
            require(medialCanthus, "medialCanthus");
            require(lateralCanthus, "lateralCanthus");
            if (contour != null) {
                if (contour.size() < 3) {
                    throw new IllegalArgumentException("Contour needs at least 3 vertices, got " + contour.size());
                }
                for (Vec2 p : contour) {
                    if (p == null) {
                        throw new IllegalArgumentException("Contour contains a null vertex");
                    }
                }
            }
            return new EyeLandmarks(this);
        }

        private static void require(Vec2 p, String name) {
            if (p == null) {
                throw new IllegalStateException("Required landmark missing: " + name);
            }
        }
    }
}
