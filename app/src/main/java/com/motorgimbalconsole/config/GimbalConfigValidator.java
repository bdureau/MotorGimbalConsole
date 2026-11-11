package com.motorgimbalconsole.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * GimbalConfigValidator.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: checks a gimbal configuration before it is sent to the board.
 *
 * Every value of the configuration screen used to be sent as typed: an empty field became 0,
 * a servo minimum could be higher than its maximum, a negative PID gain (which inverts the
 * correction of a thrust vectoring gimbal) was accepted. The board stores what it receives in
 * EEPROM, so an aberrant value stays active for the next flight. This class lists every problem
 * so that the screen can refuse the upload and tell the user what to fix.
 *
 * The limits are those of the hardware and of the board protocol:
 *  - servo angles: 0..180 degrees (Arduino Servo.write), minimum strictly lower than maximum;
 *  - PID gains: 0..100, sent as hundredths in an integer;
 *  - MPU6050 offsets: 16-bit signed registers, -32768..32767;
 *  - gyro / accelerometer ranges: index 0..3 of the sensor full-scale settings;
 *  - beep frequency: audible range 20..20000 Hz;
 *  - end of recording altitude and recording timeout: 0..10000 and 0..32767.
 *
 * Pure Java (no Android dependency) so that it is unit tested on a desktop JVM.
 */
public final class GimbalConfigValidator {

    public static final int SERVO_MIN_ANGLE = 0;
    public static final int SERVO_MAX_ANGLE = 180;
    public static final double PID_GAIN_MIN = 0.0;
    public static final double PID_GAIN_MAX = 100.0;
    public static final long OFFSET_MIN = Short.MIN_VALUE;
    public static final long OFFSET_MAX = Short.MAX_VALUE;
    public static final int SENSOR_RANGE_MIN = 0;
    public static final int SENSOR_RANGE_MAX = 3;
    public static final int BEEP_FREQUENCY_MIN = 20;
    public static final int BEEP_FREQUENCY_MAX = 20000;
    public static final int END_RECORD_ALTITUDE_MAX = 10000;
    public static final int RECORDING_TIMEOUT_MAX = Short.MAX_VALUE;

    /** Kind of problem, used by the screen to pick the translated message. */
    public enum Rule {
        /** Servo limits: min must be lower than max, both within 0..180. */
        SERVO_LIMITS,
        /** PID gain outside 0..100, or not a finite number. */
        PID_GAIN,
        /** Accelerometer / gyroscope offset outside the 16-bit range. */
        SENSOR_OFFSET,
        /** Gyroscope or accelerometer range index outside 0..3. */
        SENSOR_RANGE,
        /** Beep frequency outside 20..20000 Hz. */
        BEEP_FREQUENCY,
        /** End of recording altitude outside 0..10000. */
        END_RECORD_ALTITUDE,
        /** Recording timeout outside 0..32767. */
        RECORDING_TIMEOUT
    }

    /** One problem found in the configuration. */
    public static final class Problem {
        /** Kind of problem. */
        public final Rule rule;
        /** Name of the setting, e.g. "Kp X" or "X" for a servo. */
        public final String field;
        /** Values to display in the message (one, or min and max for a servo). */
        public final String[] values;

        Problem(Rule rule, String field, String... values) {
            this.rule = rule;
            this.field = field;
            this.values = values;
        }

        @Override
        public String toString() {
            return rule + " " + field + " " + String.join("/", values);
        }
    }

    private GimbalConfigValidator() {
        // Static helper class: no instance
    }

    /**
     * Checks a configuration.
     *
     * @param cfg          configuration about to be sent
     * @param checkOffsets true when the board uses the MPU6050 offsets (tab shown only for the
     *                     "RocketMotorGimbal" board)
     * @return the problems found, empty if the configuration can be sent
     */
    public static List<Problem> validate(GimbalConfigData cfg, boolean checkOffsets) {
        if (cfg == null) return Collections.emptyList();
        List<Problem> problems = new ArrayList<>();

        checkServo(problems, "X", cfg.getServoXMin(), cfg.getServoXMax());
        checkServo(problems, "Y", cfg.getServoYMin(), cfg.getServoYMax());

        checkGain(problems, "Kp X", cfg.getKpX());
        checkGain(problems, "Ki X", cfg.getKiX());
        checkGain(problems, "Kd X", cfg.getKdX());
        checkGain(problems, "Kp Y", cfg.getKpY());
        checkGain(problems, "Ki Y", cfg.getKiY());
        checkGain(problems, "Kd Y", cfg.getKdY());

        if (checkOffsets) {
            checkOffset(problems, "ax offset", cfg.getAxOffset());
            checkOffset(problems, "ay offset", cfg.getAyOffset());
            checkOffset(problems, "az offset", cfg.getAzOffset());
            checkOffset(problems, "gx offset", cfg.getGxOffset());
            checkOffset(problems, "gy offset", cfg.getGyOffset());
            checkOffset(problems, "gz offset", cfg.getGzOffset());
        }

        checkRange(problems, Rule.SENSOR_RANGE, "Gyro", cfg.getGyroRange(), SENSOR_RANGE_MIN, SENSOR_RANGE_MAX);
        checkRange(problems, Rule.SENSOR_RANGE, "Accelero", cfg.getAcceleroRange(), SENSOR_RANGE_MIN, SENSOR_RANGE_MAX);
        checkRange(problems, Rule.BEEP_FREQUENCY, "Beep", cfg.getBeepingFrequency(),
                BEEP_FREQUENCY_MIN, BEEP_FREQUENCY_MAX);
        checkRange(problems, Rule.END_RECORD_ALTITUDE, "End record altitude", cfg.getEndRecordAltitude(),
                0, END_RECORD_ALTITUDE_MAX);
        checkRange(problems, Rule.RECORDING_TIMEOUT, "Recording timeout", cfg.getRecordingTimeout(),
                0, RECORDING_TIMEOUT_MAX);
        return problems;
    }

    /** Servo travel: inside 0..180 degrees and a strictly positive span. */
    private static void checkServo(List<Problem> problems, String axis, int min, int max) {
        if (min < SERVO_MIN_ANGLE || max > SERVO_MAX_ANGLE || min >= max) {
            problems.add(new Problem(Rule.SERVO_LIMITS, axis, String.valueOf(min), String.valueOf(max)));
        }
    }

    /** PID gain: finite and within 0..100 (a negative gain inverts the correction). */
    private static void checkGain(List<Problem> problems, String name, double gain) {
        if (Double.isNaN(gain) || Double.isInfinite(gain) || gain < PID_GAIN_MIN || gain > PID_GAIN_MAX) {
            problems.add(new Problem(Rule.PID_GAIN, name, String.format(Locale.US, "%.2f", gain)));
        }
    }

    /** Sensor offset: must fit the 16-bit MPU6050 offset register. */
    private static void checkOffset(List<Problem> problems, String name, long offset) {
        if (offset < OFFSET_MIN || offset > OFFSET_MAX) {
            problems.add(new Problem(Rule.SENSOR_OFFSET, name, String.valueOf(offset)));
        }
    }

    /** Generic inclusive range check. */
    private static void checkRange(List<Problem> problems, Rule rule, String name, long value, long min, long max) {
        if (value < min || value > max) {
            problems.add(new Problem(rule, name, String.valueOf(value)));
        }
    }
}
