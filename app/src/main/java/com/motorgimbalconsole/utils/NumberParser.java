package com.motorgimbalconsole.utils;

import java.util.regex.Pattern;

/**
 * NumberParser.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: lenient and crash-free parsing of the numeric fields received from the gimbal
 * board in the "$telemetry", "$data" and "$alticonfig" sentences.
 *
 * Why this class exists:
 *  - The firmware sends signed values (sprintf "%i" for raw gyro / accelerometer / PID output,
 *    dtostrf for the BNO055 values). The previous regex "\d+(?:\.\d+)?" rejected the minus
 *    sign, so every negative reading was replaced by 0 or silently dropped.
 *  - That regex accepted decimals, but the values were then converted with Integer.valueOf or
 *    Long.valueOf, which throw NumberFormatException on "12.5": the whole sentence was lost,
 *    or the application crashed when this happened on the UI thread.
 *
 * Every conversion below accepts an optional sign and an optional decimal part, rounds to the
 * nearest integer when an integer is requested, and never throws.
 */
public final class NumberParser {

    /** Optional sign, integer part, optional decimal part (e.g. "-12", "3.25", "+0.5"). */
    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

    private NumberParser() {
        // Static helper class: no instance
    }

    /**
     * Tells whether a field received from the board holds a valid number.
     *
     * @param value raw field (may be null or padded with spaces)
     * @return true if the field is a signed integer or decimal number
     */
    public static boolean isNumber(String value) {
        return value != null && NUMBER.matcher(value.trim()).matches();
    }

    /** Optional sign and digits only (e.g. "-12"), for the integer fields typed by the user. */
    private static final Pattern INTEGER = Pattern.compile("[-+]?\\d+");

    /**
     * Tells whether a field typed by the user holds an integer.
     *
     * @param value raw text (may be null or padded with spaces)
     * @return true for a signed integer without decimal part
     */
    public static boolean isInteger(String value) {
        return value != null && INTEGER.matcher(value.trim()).matches();
    }

    /**
     * Tells whether a decimal value typed by the user is valid, accepting a comma as decimal
     * separator (many keyboards, e.g. French, type "0,29" in a decimal field).
     *
     * @param value raw text
     * @return true if the text is a number once the comma is replaced by a dot
     */
    public static boolean isDecimalInput(String value) {
        return value != null && isNumber(value.replace(',', '.'));
    }

    /**
     * Converts a decimal value typed by the user, accepting a comma as decimal separator.
     *
     * @param value        raw text
     * @param defaultValue value returned if the text is not a number
     * @return the parsed value, or defaultValue
     */
    public static double decimalInputToDouble(String value, double defaultValue) {
        if (value == null) return defaultValue;
        return toDouble(value.replace(',', '.'), defaultValue);
    }

    /**
     * Converts a field to a double.
     *
     * @param value        raw field
     * @param defaultValue value returned if the field is not a number
     * @return the parsed value, or defaultValue
     */
    public static double toDouble(String value, double defaultValue) {
        if (!isNumber(value)) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Converts a field to a double, 0 if it is not a number. */
    public static double toDouble(String value) {
        return toDouble(value, 0.0);
    }

    /**
     * Converts a field to a long, rounding a decimal value to the nearest integer
     * (truncation would bias every value toward zero).
     *
     * @param value        raw field
     * @param defaultValue value returned if the field is not a number
     * @return the parsed and rounded value, or defaultValue
     */
    public static long toLong(String value, long defaultValue) {
        if (!isNumber(value)) return defaultValue;
        return Math.round(toDouble(value, defaultValue));
    }

    /** Converts a field to a long, 0 if it is not a number. */
    public static long toLong(String value) {
        return toLong(value, 0L);
    }

    /**
     * Converts a field to an int, rounding a decimal value to the nearest integer.
     * Values outside the int range are clamped instead of overflowing.
     *
     * @param value        raw field
     * @param defaultValue value returned if the field is not a number
     * @return the parsed and rounded value, or defaultValue
     */
    public static int toInt(String value, int defaultValue) {
        if (!isNumber(value)) return defaultValue;
        long l = toLong(value, defaultValue);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, l));
    }

    /** Converts a field to an int, 0 if it is not a number. */
    public static int toInt(String value) {
        return toInt(value, 0);
    }
}
