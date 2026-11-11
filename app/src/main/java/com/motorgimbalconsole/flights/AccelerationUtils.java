package com.motorgimbalconsole.flights;

/**
 * AccelerationUtils.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: converts the accelerometer values recorded by the gimbal board (accelX, accelY,
 * accelZ of the "$data" sentence) into an acceleration magnitude expressed in G, used to fill
 * the "acceleration" curve (FlightData.SERIE_ACCEL) and the max acceleration statistic.
 *
 * The meaning of the recorded values depends on the board firmware:
 *  - RocketMotorGimbal (MPU6050): raw 16-bit readings of mpu.getAccelerationX/Y/Z(). The scale
 *    depends on the full-scale range (config "acceleroRange", 0..3 = +/-2, 4, 8, 16 g), i.e.
 *    16384 >> range LSB per g. Gravity is included, so the magnitude is the load factor felt by
 *    the rocket (about 1 G on the pad).
 *  - RocketMotorGimbal_bno055 (BNO055): VECTOR_LINEARACCEL in m/s^2, gravity removed, stored as
 *    whole numbers by the firmware (1 m/s^2 resolution). The magnitude is about 0 G on the pad.
 *
 * Pure Java (no Android dependency) so that it can be unit tested on a desktop JVM.
 */
public final class AccelerationUtils {

    /** Standard gravity in m/s^2. */
    public static final double STANDARD_GRAVITY = 9.80665;

    /** Raw MPU6050 sensitivity at +/-2 g; divided by 2 for each higher range step. */
    private static final double MPU6050_LSB_PER_G_2G = 16384.0;

    /** Raw readings at or beyond this value mean the MPU6050 axis is saturated. */
    private static final int MPU6050_SATURATION_RAW = 32700;

    private AccelerationUtils() {
        // Static helper class: no instance
    }

    /**
     * Tells whether the board sends BNO055 linear accelerations (m/s^2) rather than raw MPU6050
     * readings. The board name comes from the "$alticonfig" sentence.
     *
     * @param boardName board firmware name, e.g. "RocketMotorGimbal_bno055"
     * @return true for a BNO055 based board
     */
    public static boolean isBno055(String boardName) {
        return boardName != null && boardName.toLowerCase().contains("bno055");
    }

    /**
     * Number of raw MPU6050 units per g for the configured full-scale range.
     *
     * @param acceleroRange configured range index (0 = +/-2 g ... 3 = +/-16 g); out of range
     *                      values fall back to +/-2 g, the sensor power-on default
     * @return sensitivity in LSB per g
     */
    public static double mpu6050LsbPerG(int acceleroRange) {
        int range = (acceleroRange >= 0 && acceleroRange <= 3) ? acceleroRange : 0;
        return MPU6050_LSB_PER_G_2G / (1 << range);
    }

    /**
     * Converts one accelerometer sample to an acceleration magnitude in G.
     *
     * @param ax            recorded X value
     * @param ay            recorded Y value
     * @param az            recorded Z value
     * @param boardName     board firmware name (selects the conversion)
     * @param acceleroRange configured MPU6050 range index (ignored for the BNO055)
     * @return magnitude of the acceleration vector in G
     */
    public static double magnitudeInG(double ax, double ay, double az,
                                      String boardName, int acceleroRange) {
        double norm = Math.sqrt(ax * ax + ay * ay + az * az);
        if (isBno055(boardName)) {
            return norm / STANDARD_GRAVITY;
        }
        return norm / mpu6050LsbPerG(acceleroRange);
    }

    /**
     * Tells whether a raw MPU6050 sample hit the end of the sensor range. When it does, the
     * real acceleration was higher than the recorded one and the maximum is only a lower bound.
     *
     * @param ax        recorded X value
     * @param ay        recorded Y value
     * @param az        recorded Z value
     * @param boardName board firmware name (always false for the BNO055)
     * @return true if at least one axis is saturated
     */
    public static boolean isSaturated(double ax, double ay, double az, String boardName) {
        if (isBno055(boardName)) return false;
        return Math.abs(ax) >= MPU6050_SATURATION_RAW
                || Math.abs(ay) >= MPU6050_SATURATION_RAW
                || Math.abs(az) >= MPU6050_SATURATION_RAW;
    }
}
