package com.periomeasure.core;

/** Small dense linear algebra (no external dependency). Inputs are never modified. */
final class LinAlg {

    /** Relative pivot threshold below which a matrix is treated as singular. */
    private static final double SINGULAR_TOL = 1e-13;

    private LinAlg() {
    }

    /**
     * Solves A x = b by Gaussian elimination with partial pivoting.
     *
     * @throws ArithmeticException if A is (numerically) singular
     */
    static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        double scale = 0;
        for (int i = 0; i < n; i++) {
            if (a[i].length != n) {
                throw new IllegalArgumentException("Matrix must be square");
            }
            for (int j = 0; j < n; j++) {
                m[i][j] = a[i][j];
                scale = Math.max(scale, Math.abs(a[i][j]));
            }
            m[i][n] = b[i];
        }
        if (scale == 0) {
            throw new ArithmeticException("Zero matrix");
        }
        for (int col = 0; col < n; col++) {
            int piv = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[piv][col])) {
                    piv = r;
                }
            }
            if (Math.abs(m[piv][col]) <= SINGULAR_TOL * scale) {
                throw new ArithmeticException("Singular matrix");
            }
            double[] tmp = m[col];
            m[col] = m[piv];
            m[piv] = tmp;
            for (int r = col + 1; r < n; r++) {
                double f = m[r][col] / m[col][col];
                if (f != 0) {
                    for (int c = col; c <= n; c++) {
                        m[r][c] -= f * m[col][c];
                    }
                }
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double s = m[i][n];
            for (int j = i + 1; j < n; j++) {
                s -= m[i][j] * x[j];
            }
            x[i] = s / m[i][i];
        }
        return x;
    }

    /** @throws ArithmeticException if A is (numerically) singular */
    static double[][] inverse(double[][] a) {
        int n = a.length;
        double[][] inv = new double[n][n];
        for (int c = 0; c < n; c++) {
            double[] e = new double[n];
            e[c] = 1;
            double[] col = solve(a, e);
            for (int r = 0; r < n; r++) {
                inv[r][c] = col[r];
            }
        }
        return inv;
    }
}
