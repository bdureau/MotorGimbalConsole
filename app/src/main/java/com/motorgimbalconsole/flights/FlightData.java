package com.motorgimbalconsole.flights;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.motorgimbalconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FlightData.java
 *
 * Manages collections of flight telemetry series and datasets for rocket avionics analysis.
 * Provides optimized thread-safe storage, O(1) retrieval, and series initialization.
 *
 * @author boris.dureau@neuf.fr
 */
public class FlightData {

    // Common series index constants to avoid magic numbers across activities
    public static final int SERIE_ALTITUDE = 0;
    public static final int SERIE_TEMPERATURE = 1;
    public static final int SERIE_PRESSURE = 2;
    public static final int SERIE_SPEED = 17;
    public static final int SERIE_ACCEL = 18;

    // Use Application Context to prevent Activity memory leaks
    private final Context appContext;

    // Thread-safe map with typed generics to ensure O(1) access and thread safety during flight imports
    private final Map<String, XYSeriesCollection> flightMap;

    /**
     * Initializes the FlightData store using application context.
     *
     * @param context Current Android Context
     */
    public FlightData(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.flightMap = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the total number of recorded flights.
     *
     * @return Number of flights currently stored.
     */
    public int getNbrOfFlight() {
        return flightMap.size();
    }

    /**
     * Returns a thread-safe copy of all recorded flight identifiers.
     *
     * @return List of flight names.
     */
    @NonNull
    public List<String> getAllFlightNames2() {
        return new ArrayList<>(flightMap.keySet());
    }

    /**
     * Clears all flight telemetry data from memory.
     */
    public void ClearFlight() {
        flightMap.clear();
    }

    /**
     * Retrieves the full XYSeriesCollection for a designated flight name in O(1) time.
     *
     * @param flightName Target flight identifier
     * @return Flight dataset or null if not found
     */
    @Nullable
    public XYSeriesCollection GetFlightData(@NonNull String flightName) {
        return flightMap.get(flightName);
    }

    /**
     * Checks if a specific flight dataset is present in memory.
     *
     * @param flightName Flight identifier to check
     * @return true if flight exists, false otherwise
     */
    public boolean FlightExist(@NonNull String flightName) {
        return flightMap.containsKey(flightName);
    }

    /**
     * Appends a data point to the primary series (altitude) of the specified flight.
     *
     * @param timestampMs Telemetry timestamp or X-value in milliseconds
     * @param altitude    Recorded altitude value
     * @param flightName  Target flight identifier
     */
    public void AddToFlight(long timestampMs, double altitude, @NonNull String flightName) {
        AddToFlight(timestampMs, altitude, flightName, SERIE_ALTITUDE);
    }

    /**
     * Appends a data point to a specific curve/channel index of the flight dataset.
     * Automatically initializes the flight curves if the flight record does not exist yet.
     *
     * @param timestampMs Telemetry timestamp or X-value
     * @param value       Sensor measurement value (Y-value)
     * @param flightName  Target flight identifier
     * @param serieIndex  Zero-based index of the telemetry series
     */
    public void AddToFlight(long timestampMs, double value, @NonNull String flightName, int serieIndex) {
        // Atomic compute-if-absent pattern for thread-safe initialization
        XYSeriesCollection flightData = flightMap.computeIfAbsent(flightName, this::createFlight);

        if (serieIndex >= 0 && serieIndex < flightData.getSeriesCount()) {
            flightData.getSeries(serieIndex).add((double) timestampMs, value);
        }
    }

    /**
     * Directly inserts or replaces a flight dataset into the storage.
     *
     * @param flightData Full XYSeriesCollection for the flight
     * @param flightName Unique flight name
     */
    public void AddFlightData(@NonNull XYSeriesCollection flightData, @NonNull String flightName) {
        flightMap.put(flightName, flightData);
    }

    /**
     * Creates and structures a default blank telemetry series template for a new rocket flight.
     *
     * @param name Name of the flight dataset
     * @return Initialized XYSeriesCollection containing all sensor channels
     */
    private XYSeriesCollection createFlight(final String name) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Environment & Altimeter curves
        dataset.addSeries(new XYSeries(appContext.getString(R.string.altitude)));
        dataset.addSeries(new XYSeries(appContext.getString(R.string.curve_temperature)));
        dataset.addSeries(new XYSeries(appContext.getString(R.string.curve_pressure)));

        // Inertial Measurement Unit (IMU) Gravity vectors
        dataset.addSeries(new XYSeries("Gravity X"));
        dataset.addSeries(new XYSeries("Gravity Y"));
        dataset.addSeries(new XYSeries("Gravity Z"));

        // Attitude Estimation (Euler angles)
        dataset.addSeries(new XYSeries("Euler X"));
        dataset.addSeries(new XYSeries("Euler Y"));
        dataset.addSeries(new XYSeries("Euler Z"));
        dataset.addSeries(new XYSeries("Yaw"));
        dataset.addSeries(new XYSeries("Pitch"));
        dataset.addSeries(new XYSeries("Roll"));

        // Gimbal / Actuator PID outputs
        dataset.addSeries(new XYSeries("outputX"));
        dataset.addSeries(new XYSeries("outputY"));

        // Raw Linear Accelerations
        dataset.addSeries(new XYSeries("accelX"));
        dataset.addSeries(new XYSeries("accelY"));
        dataset.addSeries(new XYSeries("accelZ"));

        // Derived Kinematics (Index 17 & 18)
        dataset.addSeries(new XYSeries(appContext.getString(R.string.curve_speed)));
        dataset.addSeries(new XYSeries(appContext.getString(R.string.curve_accel)));

        return dataset;
    }
}