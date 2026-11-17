package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LinAlgTest {

    @Test
    void solvesKnownSystem() {
        double[][] a = {{2, 1, -1}, {-3, -1, 2}, {-2, 1, 2}};
        double[] b = {8, -11, -3};
        assertArrayEquals(new double[] {2, 3, -1}, LinAlg.solve(a, b), 1e-12);
    }

    @Test
    void pivotsWhenLeadingEntryIsZero() {
        double[][] a = {{0, 1}, {1, 0}};
        assertArrayEquals(new double[] {5, 7}, LinAlg.solve(a, new double[] {7, 5}), 0);
    }

    @Test
    void singularMatrixIsRejected() {
        double[][] a = {{1, 2}, {2, 4}};
        assertThrows(ArithmeticException.class, () -> LinAlg.solve(a, new double[] {1, 2}));
    }

    @Test
    void inputsAreNotModified() {
        double[][] a = {{0, 1}, {1, 0}};
        double[] b = {7, 5};
        LinAlg.solve(a, b);
        assertArrayEquals(new double[] {0, 1}, a[0], 0);
        assertArrayEquals(new double[] {7, 5}, b, 0);
    }

    @Test
    void inverseTimesMatrixIsIdentity() {
        Random rnd = new Random(42);
        double[][] a = new double[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                a[i][j] = rnd.nextGaussian();
            }
            a[i][i] += 5; // well conditioned
        }
        double[][] inv = LinAlg.inverse(a);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                double s = 0;
                for (int k = 0; k < 5; k++) {
                    s += inv[i][k] * a[k][j];
                }
                assertEquals(i == j ? 1 : 0, s, 1e-12);
            }
        }
    }
}
