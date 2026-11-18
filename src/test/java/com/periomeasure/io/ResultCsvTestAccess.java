package com.periomeasure.io;

/** Shares the record fixtures of {@link ResultCsvTest} with tests in other packages. */
public final class ResultCsvTestAccess {

    private ResultCsvTestAccess() {
    }

    public static MeasurementRecord bothEyes(String patientId) {
        return ResultCsvTest.bothEyes(patientId);
    }

    public static MeasurementRecord rightEyeOnly() {
        return ResultCsvTest.rightEyeOnly();
    }
}
