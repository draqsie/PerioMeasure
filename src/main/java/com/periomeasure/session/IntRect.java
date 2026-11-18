package com.periomeasure.session;

/** Integer rectangle (kept free of java.awt so the session package stays pure). */
public final class IntRect {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public IntRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "IntRect[" + x + ", " + y + ", " + width + " x " + height + "]";
    }
}
