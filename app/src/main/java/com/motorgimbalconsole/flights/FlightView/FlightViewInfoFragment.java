package com.motorgimbalconsole.flights.FlightView;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.GlobalConfig;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.flights.AccelerationUtils;
import com.motorgimbalconsole.flights.FlightData;
import com.motorgimbalconsole.flights.FlightFileExporter;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * FlightViewInfoFragment.java
 *
 * Fragment presenting the post-flight statistics of a model rocket flight: flight duration,
 * apogee and time to apogee, max velocity and burnout time, drogue descent rate, main
 * parachute deployment altitude, landing speed and max acceleration. Also manages the CSV
 * and ZIP telemetry exports.
 *
 * All the statistics except the acceleration are computed from the altitude curve by
 * the nested {@link FlightStatsCalculator} class (pure Java, unit-testable), which uses least-squares fits
 * over several samples instead of single noisy samples. This fragment only converts the
 * curve to arrays and displays the results.
 *
 * @author boris.dureau@neuf.fr
 */
public class FlightViewInfoFragment extends Fragment {

    private static final String TAG = "FlightViewInfoFragment";

    // Indexes of the raw accelerometer curves (see FlightData.createFlight)
    private static final int SERIE_ACCEL_X = 14;
    private static final int SERIE_ACCEL_Y = 15;
    private static final int SERIE_ACCEL_Z = 16;

    // Application context and flight repository handles
    private FlightData myflight;
    private XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;
    private String[] units;
    private String flightName;

    // Telemetry curves used for the statistics
    private XYSeries altitudeSeries;
    private XYSeries accelSeries;

    // UI TextView references for flight statistics
    private TextView nbrOfSamplesValue;
    private TextView flightNbrValue;
    private TextView apogeeAltitudeValue;
    private TextView flightDurationValue;
    private TextView burnTimeValue;
    private TextView maxVelociyValue;
    private TextView maxAccelerationValue;
    private TextView timeToApogeeValue;
    private TextView mainAltitudeValue;
    private TextView maxDescentValue;
    private TextView landingSpeedValue;

    // Action buttons
    private Button buttonExportToCsv;
    private Button butShareFiles;

    /**
     * Default empty constructor required by the Android framework.
     */
    public FlightViewInfoFragment() {
        super();
    }

    // Keys of the arguments Bundle (survives the re-creation of the fragment by Android)
    private static final String ARG_FLIGHT_NAME = "flightName";
    private static final String ARG_UNITS = "units";

    /**
     * Creates the statistics tab of a downloaded flight. The flight name and units are stored
     * in the arguments Bundle; the former constructor parameters were lost when Android
     * re-created the fragment, and the tab then stayed empty.
     *
     * @param pFlightName name of the flight in the application FlightData
     * @param pUnits      unit label of each telemetry curve (used in the CSV headers)
     * @return the fragment
     */
    public static FlightViewInfoFragment newInstance(String pFlightName, String[] pUnits) {
        FlightViewInfoFragment fragment = new FlightViewInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FLIGHT_NAME, pFlightName);
        args.putStringArray(ARG_UNITS, pUnits);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            flightName = args.getString(ARG_FLIGHT_NAME);
            units = args.getStringArray(ARG_UNITS);
        }
        myBT = ConsoleApplication.getInstance();
        if (myBT != null) {
            myflight = myBT.getFlightData();
            if (flightName != null) {
                allFlightData = myflight.GetFlightData(flightName);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flightview_info, container, false);

        initializeViews(view);

        if (myBT == null && getActivity() != null) {
            myBT = (ConsoleApplication) getActivity().getApplication();
        }

        if (myflight == null && myBT != null) {
            myflight = myBT.getFlightData();
        }

        computeAndDisplayFlightStatistics();
        setupActionListeners();

        return view;
    }

    /**
     * Binds UI widgets from layout to class fields.
     *
     * @param view Fragment root view
     */
    private void initializeViews(View view) {
        buttonExportToCsv = view.findViewById(R.id.butExportToCsv);
        butShareFiles = view.findViewById(R.id.butShareFiles);

        apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
        flightDurationValue = view.findViewById(R.id.flightDurationValue);
        burnTimeValue = view.findViewById(R.id.burnTimeValue);
        maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
        maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
        timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
        mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
        maxDescentValue = view.findViewById(R.id.maxDescentValue);
        landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
        nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
        flightNbrValue = view.findViewById(R.id.flightNbrValue);
    }

    /**
     * Computes the flight statistics from the altitude curve and displays them.
     */
    private void computeAndDisplayFlightStatistics() {
        if (myflight == null || flightName == null || myBT == null) {
            return;
        }

        XYSeriesCollection flightData = myflight.GetFlightData(flightName);
        if (flightData == null || flightData.getSeriesCount() == 0) {
            return;
        }

        if (allFlightData == null) {
            allFlightData = flightData;
        }

        altitudeSeries = allFlightData.getSeries(getString(R.string.altitude));
        accelSeries = allFlightData.getSeries(getString(R.string.curve_accel));

        int sampleCount = (altitudeSeries != null) ? altitudeSeries.getItemCount() : 0;
        flightNbrValue.setText(flightName);
        nbrOfSamplesValue.setText(String.valueOf(sampleCount));

        if (sampleCount == 0) {
            return;
        }

        String altUnit = myBT.getAppConf().getUnitsValue();
        boolean isMeters = (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS);
        String speedUnit = isMeters ? "m/s" : "ft/s";

        // Altitude curve as arrays for the calculator
        double[] timeMs = new double[sampleCount];
        double[] altitude = new double[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            timeMs[i] = altitudeSeries.getX(i).doubleValue();
            altitude[i] = altitudeSeries.getY(i).doubleValue();
        }

        FlightStatsCalculator.FlightStats stats =
                FlightStatsCalculator.compute(timeMs, altitude, isMeters);

        Log.d(TAG, String.format(Locale.US,
                "liftoff=%d apogee=%d touchdown=%d (recorded=%b) ground=%.1f",
                stats.liftoffIndex, stats.apogeeIndex, stats.touchdownIndex,
                stats.touchdownRecorded, stats.groundAltitude));

        // 1. Flight duration (liftoff to touchdown; "~" if the recording stopped before touchdown)
        String duration = format("%.2f secs", stats.flightDurationSec);
        flightDurationValue.setText(stats.flightDurationEstimated ? "~" + duration : duration);

        // 2. Apogee and time to apogee (from liftoff)
        apogeeAltitudeValue.setText(format("%.0f " + altUnit, stats.apogeeAltitude));
        timeToApogeeValue.setText(format("%.2f secs", stats.timeToApogeeSec));

        // 3. Max velocity and burnout time (peak of the smoothed ascent speed)
        maxVelociyValue.setText(format("%.0f " + speedUnit, stats.maxVelocity));
        burnTimeValue.setText(format("%.2f secs", stats.burnoutTimeSec));

        // 4. Descent rate under drogue (or under the single parachute)
        maxDescentValue.setText(format("%.1f " + speedUnit, stats.maxDescentRate));

        // 5. Main parachute deployment altitude (break in the descent curve)
        mainAltitudeValue.setText(format("%.0f " + altUnit, stats.mainDeployAltitude));
        if (!Double.isNaN(stats.mainDeployAltitude)) {
            Log.d(TAG, String.format(Locale.US, "Main deploy: t=%.2fs alt=%.1f main rate=%.2f",
                    stats.mainDeployTimeSec, stats.mainDeployAltitude, stats.mainDescentRate));
        }

        // 6. Landing speed (regression over the last 30 m / 100 ft of the descent)
        landingSpeedValue.setText(format("%.1f " + speedUnit, stats.landingSpeed));

        // 7. Maximum acceleration, in G. The acceleration curve is filled when the flight is
        // downloaded (ConsoleApplication, "$data" sentence) with the magnitude of the IMU
        // acceleration vector, already converted to G: no unit conversion is needed here.
        displayMaxAcceleration();
    }

    /**
     * Displays the maximum acceleration of the flight.
     * With an MPU6050 board, if an axis hit the end of the sensor range during the flight the
     * real peak was higher than the recorded one, so the value is shown as a lower bound (">").
     */
    private void displayMaxAcceleration() {
        if (accelSeries == null || accelSeries.getItemCount() == 0) {
            maxAccelerationValue.setText("N/A");
            return;
        }
        double maxG = accelSeries.getMaxY();
        String text = String.format(Locale.US, "%.1f G", maxG);
        if (isAccelerometerSaturated()) {
            text = ">" + text;
        }
        maxAccelerationValue.setText(text);
    }

    /**
     * Tells whether at least one raw accelerometer sample of the flight reached the end of the
     * MPU6050 range (always false for a BNO055 board).
     *
     * @return true if the recorded acceleration is clipped
     */
    private boolean isAccelerometerSaturated() {
        if (allFlightData == null || allFlightData.getSeriesCount() <= SERIE_ACCEL_Z || myBT == null) {
            return false;
        }
        String boardName = myBT.getGimbalConfigData().getAltimeterName();
        XYSeries ax = allFlightData.getSeries(SERIE_ACCEL_X);
        XYSeries ay = allFlightData.getSeries(SERIE_ACCEL_Y);
        XYSeries az = allFlightData.getSeries(SERIE_ACCEL_Z);
        int count = Math.min(ax.getItemCount(), Math.min(ay.getItemCount(), az.getItemCount()));
        for (int i = 0; i < count; i++) {
            if (AccelerationUtils.isSaturated(ax.getY(i).doubleValue(), ay.getY(i).doubleValue(),
                    az.getY(i).doubleValue(), boardName)) {
                return true;
            }
        }
        return false;
    }

    /** Formats a value, or returns "N/A" if it could not be determined (NaN). */
    private static String format(String pattern, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "N/A";
        return String.format(Locale.US, pattern, value);
    }

    /**
     * Attaches click handlers for CSV curve file exportation and ZIP archive sharing.
     */
    private void setupActionListeners() {
        buttonExportToCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportFlightCurvesToCsv();
            }
        });

        butShareFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAndShareFlightZipArchive();
            }
        });
    }

    /**
     * Exports every non empty curve of the flight as one CSV file in
     * Download/MotorGimbalConsoleFlights (see FlightFileExporter for the storage rules).
     */
    private void exportFlightCurvesToCsv() {
        if (allFlightData == null) return;

        // One timestamp for the whole export, so that the files of one export sort together
        String dateString = exportTimestamp();
        StringBuilder savedFiles = new StringBuilder();
        boolean allSaved = true;
        for (int j = 0; j < allFlightData.getSeriesCount(); j++) {
            String csv = buildCsv(j, allFlightData);
            if (csv == null) continue; // empty curve
            String fileName = csvFileName(j, dateString);
            if (FlightFileExporter.saveToDownloads(requireContext(), fileName, csv, FlightFileExporter.MIME_CSV)) {
                savedFiles.append(fileName).append("\n");
            } else {
                allSaved = false;
            }
        }
        if (!allSaved) {
            Log.w(TAG, "Some curves could not be saved");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage(getString(R.string.save_curve_msg) + Environment.DIRECTORY_DOWNLOADS +
                        File.separator + FlightFileExporter.EXPORT_FOLDER + "\n" + savedFiles)
                .setTitle(getString(R.string.save_curves_title))
                .setCancelable(false)
                .setPositiveButton(R.string.save_curve_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
        msg(getString(R.string.curves_saved_msg));
    }

    /**
     * Zips every non empty curve of the flight in the application cache and opens the Android
     * share sheet. Nothing is written to the Download folder any more.
     */
    private void exportAndShareFlightZipArchive() {
        if (allFlightData == null) return;

        String dateString = exportTimestamp();
        Map<String, String> entries = new LinkedHashMap<>();
        for (int j = 0; j < allFlightData.getSeriesCount(); j++) {
            String csv = buildCsv(j, allFlightData);
            if (csv != null) {
                entries.put(csvFileName(j, dateString), csv);
            }
        }

        try {
            File zipFile = FlightFileExporter.writeZipToSharedCache(requireContext(), "gimbalData.zip", entries);
            shareFile(zipFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed creating flight ZIP archive", e);
            msg("Unable to create the flight archive");
        }
    }

    /** @return the date suffix of the exported file names, e.g. "_24-05-2026_14-03-59" */
    private static String exportTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss", Locale.US);
        return sdf.format(System.currentTimeMillis());
    }

    /**
     * @param seriesIndex index of the curve
     * @param dateString  date suffix of the export
     * @return CSV file name, e.g. "Flight 01-altitude_24-05-2026_14-03-59.csv"
     */
    private String csvFileName(int seriesIndex, String dateString) {
        return flightName + "-" + allFlightData.getSeries(seriesIndex).getKey().toString() + dateString + ".csv";
    }

    /**
     * Serializes one curve as CSV: a "time(ms),name unit" header, then one "time,value" line per
     * sample. Numbers use the dot as decimal separator whatever the phone language.
     *
     * @param seriesIndex Index of the target series
     * @param dataSet     Dataset collection holding telemetry series
     * @return the CSV text, or null if the curve is empty or does not exist
     */
    private String buildCsv(int seriesIndex, XYSeriesCollection dataSet) {
        if (dataSet == null || seriesIndex < 0 || seriesIndex >= dataSet.getSeriesCount()) {
            return null;
        }

        XYSeries series = dataSet.getSeries(seriesIndex);
        int sampleCount = series.getItemCount();
        if (sampleCount == 0) {
            return null; // e.g. the speed curve, not computed yet
        }
        String valHeader = getSeriesHeaderName(seriesIndex);
        String unitSuffix = (units != null && seriesIndex < units.length && units[seriesIndex] != null)
                ? units[seriesIndex] : "";

        StringBuilder csvData = new StringBuilder();
        csvData.append("time(ms),").append(valHeader).append(" ").append(unitSuffix).append("\n");
        for (int i = 0; i < sampleCount; i++) {
            csvData.append(series.getX(i).doubleValue())
                    .append(",")
                    .append(series.getY(i).doubleValue())
                    .append("\n");
        }
        return csvData.toString();
    }

    /**
     * Resolves human-readable column headers for telemetry channels.
     *
     * @param index Curve index
     * @return Readable telemetry channel descriptor
     */
    private String getSeriesHeaderName(int index) {
        switch (index) {
            case 0: return getString(R.string.curve_altitude);
            case 1: return getString(R.string.curve_temperature);
            case 2: return getString(R.string.curve_pressure);
            case 3: return "Gravity X";
            case 4: return "Gravity Y";
            case 5: return "Gravity Z";
            case 6: return "Euler X";
            case 7: return "Euler Y";
            case 8: return "Euler Z";
            case 9: return "Yaw";
            case 10: return "Pitch";
            case 11: return "Roll";
            case 12: return "outputX";
            case 13: return "outputY";
            case 14: return "accelX";
            case 15: return "accelY";
            case 16: return "accelZ";
            case 17: return getString(R.string.curve_speed);
            case 18: return getString(R.string.curve_accel);
            default: return "Channel_" + index;
        }
    }

    /**
     * Launches the Android system file sharing chooser for the target archive.
     *
     * @param file File to be shared
     */
    private void shareFile(File file) {
        if (getContext() == null || !file.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TEXT, "Motor Gimbal telemetry flight package attached.");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        // The URI is also attached as ClipData: this is what makes Android pass the temporary
        // read permission through the chooser to the application picked by the user. The
        // former loop on queryIntentActivities() returns nothing on Android 11+ (package
        // visibility rules), so no permission was granted.
        intent.setClipData(ClipData.newRawUri("", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Share Telemetry File");

        try {
            startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No application available to share file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a toast message safely on the UI thread.
     *
     * @param message Text to display
     */
    private void msg(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    // ---- FlightStatsCalculator (begin) ----
    /**
     * Computes the post-flight statistics of a rocket flight from its altitude curve.
     *
     * Pure Java (no Android dependency): it can be copied out and unit tested on a desktop JVM.
     *
     * Why everything is derived from the altitude curve:
     *  the barometric altitude is the only clean measurement. A speed obtained by differentiating
     *  it sample by sample is very noisy, and taking the maximum of a noisy signal always
     *  over-estimates it. So instead of picking single extreme samples, every value below is
     *  obtained from least-squares straight lines fitted over several samples:
     *
     *  - Ground reference: the altitudes are relative to the launch pad, so the ground is 0
     *    (or the pre-liftoff level if the recording starts below 0). The last recorded sample is
     *    NOT used, because the recording often stops a few meters above the ground.
     *  - Liftoff: start of the climb (last point within 3 m / 10 ft of the ground before the
     *    rocket is clearly flying). It is the origin of every duration.
     *  - Apogee altitude: maximum after a 3-point median filter (removes isolated spikes).
     *    Apogee instant: smoothed vertical speed crossing zero (the top of the curve is flat).
     *  - Max velocity / burnout: vertical speed from sliding linear regressions, peak searched
     *    between liftoff and apogee only. Burnout = time of max velocity.
     *  - Descent: two-segment linear regression on the descent (drogue then main). Gives the
     *    drogue descent rate, the main descent rate and the main deployment altitude at the
     *    intersection of the two lines. With a single parachute, one line is fitted instead.
     *  - Landing speed: linear regression over the last 30 m (100 ft) of the descent.
     *  - Flight duration: liftoff to touchdown. If the recording stops before touchdown, the
     *    remaining time is estimated from the landing speed (flagged as estimated).
     *
     * All rates are positive values, expressed in altitude units per second.
     */
    static final class FlightStatsCalculator {

        /** Result of the computation. Values that cannot be determined are Double.NaN. */
        public static final class FlightStats {
            public double groundAltitude = Double.NaN;
            public int liftoffIndex = -1;
            public int apogeeIndex = -1;
            public int touchdownIndex = -1;
            /** false if the recording stopped before the rocket reached the ground. */
            public boolean touchdownRecorded = false;

            public double flightDurationSec = Double.NaN;
            /** true if flightDurationSec includes an estimated part (recording stopped early). */
            public boolean flightDurationEstimated = false;
            public double apogeeAltitude = Double.NaN;
            public double timeToApogeeSec = Double.NaN;
            public double maxVelocity = Double.NaN;
            public double burnoutTimeSec = Double.NaN;
            /** Descent rate under drogue (or under the single parachute if there is no main). */
            public double maxDescentRate = Double.NaN;
            public double mainDescentRate = Double.NaN;
            public double mainDeployAltitude = Double.NaN;
            /** Main deployment time, counted from liftoff. */
            public double mainDeployTimeSec = Double.NaN;
            public double landingSpeed = Double.NaN;
        }

        // Thresholds expressed in meters; converted when the altitudes are in feet
        private static final double LIFTOFF_THRESHOLD_M = 3.0;
        private static final double GROUND_MARGIN_M = 3.0;
        private static final double LANDING_WINDOW_M = 30.0;
        private static final double METERS_TO_FEET = 3.28084;

        /** Number of first samples used to estimate the pre-liftoff level. */
        private static final int GROUND_SAMPLES = 5;
        /** Half width of the sliding window used to compute the reported max velocity. */
        private static final double SPEED_HALF_WINDOW_SEC = 0.25;
        /** Wider window used to locate the burnout and apogee instants (less sensitive to noise). */
        private static final double TIMING_HALF_WINDOW_SEC = 0.5;
        /** Time skipped after apogee (rounded top of the curve, rocket turning over). */
        private static final double POST_APOGEE_SKIP_SEC = 1.0;
        /** Minimum number of samples on each side of the main deployment breakpoint. */
        private static final int MIN_SEGMENT_SAMPLES = 8;
        /** The main descent rate must be at most this fraction of the drogue descent rate. */
        private static final double MAX_MAIN_TO_DROGUE_RATE_RATIO = 0.75;
        /** Minimum data required for the landing speed regression. */
        private static final int MIN_LANDING_SAMPLES = 5;
        private static final double MIN_LANDING_DURATION_SEC = 1.0;

        private final double[] t;   // seconds since the first sample
        private final double[] y;   // altitude
        private final int n;
        private final double liftoffThreshold;
        private final double groundMargin;
        private final double landingWindow;

        // Prefix sums of t, y, t^2, t*y, y^2 (index i = sum of samples [0, i))
        private final double[] sT, sY, sTT, sTY, sYY;

        private FlightStatsCalculator(double[] timeMs, double[] altitude, boolean isMeters) {
            n = Math.min(timeMs.length, altitude.length);
            t = new double[n];
            y = new double[n];
            for (int i = 0; i < n; i++) {
                t[i] = (timeMs[i] - timeMs[0]) / 1000.0;
                y[i] = altitude[i];
            }
            double scale = isMeters ? 1.0 : METERS_TO_FEET;
            liftoffThreshold = LIFTOFF_THRESHOLD_M * scale;
            groundMargin = GROUND_MARGIN_M * scale;
            landingWindow = LANDING_WINDOW_M * scale;

            sT = new double[n + 1];
            sY = new double[n + 1];
            sTT = new double[n + 1];
            sTY = new double[n + 1];
            sYY = new double[n + 1];
            for (int i = 0; i < n; i++) {
                sT[i + 1] = sT[i] + t[i];
                sY[i + 1] = sY[i] + y[i];
                sTT[i + 1] = sTT[i] + t[i] * t[i];
                sTY[i + 1] = sTY[i] + t[i] * y[i];
                sYY[i + 1] = sYY[i] + y[i] * y[i];
            }
        }

        /**
         * Computes the flight statistics.
         *
         * @param timeMs   sample times in milliseconds (increasing)
         * @param altitude altitudes relative to the launch pad, in meters or feet
         * @param isMeters true if the altitudes are in meters, false if in feet
         * @return the statistics (never null; undetermined values are NaN)
         */
        public static FlightStats compute(double[] timeMs, double[] altitude, boolean isMeters) {
            FlightStats s = new FlightStats();
            if (timeMs == null || altitude == null || Math.min(timeMs.length, altitude.length) < 3) {
                return s;
            }
            new FlightStatsCalculator(timeMs, altitude, isMeters).fill(s);
            return s;
        }

        private void fill(FlightStats s) {
            // Ground and liftoff
            s.groundAltitude = Math.min(0.0, medianOfFirstSamples(GROUND_SAMPLES));
            s.liftoffIndex = findLiftoffIndex(s.groundAltitude);
            double liftoffT = t[s.liftoffIndex];

            // Apogee altitude: maximum after a 3-point median filter (ignores isolated spikes)
            int highestIndex = s.liftoffIndex;
            double best = median3(s.liftoffIndex);
            for (int i = s.liftoffIndex + 1; i < n; i++) {
                double v = median3(i);
                if (v > best) {
                    best = v;
                    highestIndex = i;
                }
            }
            s.apogeeAltitude = best;

            // Burnout (max velocity) and apogee instant (speed crossing zero), both located
            // on the smoothed speed: the top of the curve is flat, so the highest sample alone
            // can be off by more than a second.
            s.apogeeIndex = computeAscent(s, liftoffT, highestIndex);
            s.timeToApogeeSec = t[s.apogeeIndex] - liftoffT;

            // Touchdown: first point back near the ground after apogee (median filtered so that
            // a single noisy sample does not end the flight early)
            s.touchdownIndex = n - 1;
            for (int i = s.apogeeIndex + 1; i < n; i++) {
                if (median3(i) <= s.groundAltitude + groundMargin) {
                    s.touchdownIndex = i;
                    s.touchdownRecorded = true;
                    break;
                }
            }

            int descentEnd = computeDescent(s, liftoffT);
            computeLandingSpeed(s, descentEnd);
            computeDuration(s, liftoffT);
        }

        /**
         * Max velocity, burnout time and apogee instant.
         *  - The burnout instant is the peak of the speed smoothed over +/-0.5 s, searched between
         *    liftoff and the highest sample. The reported max velocity is the +/-0.25 s speed at
         *    that instant (taking a maximum of the less smoothed speed would over-estimate it).
         *  - The apogee instant is the first point after burnout where the smoothed speed is <= 0.
         *
         * @return index of the apogee instant
         */
        private int computeAscent(FlightStats s, double liftoffT, int highestIndex) {
            if (highestIndex <= s.liftoffIndex) return highestIndex;

            double maxV = Double.NEGATIVE_INFINITY;
            int burnoutIdx = -1;
            for (int i = s.liftoffIndex; i <= highestIndex; i++) {
                double v = verticalSpeedAt(i, TIMING_HALF_WINDOW_SEC);
                if (!Double.isNaN(v) && v > maxV) {
                    maxV = v;
                    burnoutIdx = i;
                }
            }
            if (burnoutIdx < 0 || maxV <= 0) return highestIndex;

            // Refine: the heavily smoothed peak lags the real one, so take the peak of the less
            // smoothed speed, but only within +/-0.5 s of it (a bounded search keeps noise from
            // inflating the value or moving the instant far away).
            int refinedIdx = burnoutIdx;
            double refinedV = verticalSpeedAt(burnoutIdx, SPEED_HALF_WINDOW_SEC);
            for (int i = burnoutIdx; i >= s.liftoffIndex && t[burnoutIdx] - t[i] <= TIMING_HALF_WINDOW_SEC; i--) {
                double v = verticalSpeedAt(i, SPEED_HALF_WINDOW_SEC);
                if (v > refinedV) { refinedV = v; refinedIdx = i; }
            }
            for (int i = burnoutIdx + 1; i <= highestIndex && t[i] - t[burnoutIdx] <= TIMING_HALF_WINDOW_SEC; i++) {
                double v = verticalSpeedAt(i, SPEED_HALF_WINDOW_SEC);
                if (v > refinedV) { refinedV = v; refinedIdx = i; }
            }
            s.maxVelocity = Double.isNaN(refinedV) ? maxV : refinedV;
            s.burnoutTimeSec = t[refinedIdx] - liftoffT;

            // Apogee: smoothed speed crossing zero, searched up to 3 s past the highest sample
            for (int i = burnoutIdx; i < n && t[i] <= t[highestIndex] + 3.0; i++) {
                if (verticalSpeedAt(i, TIMING_HALF_WINDOW_SEC) <= 0) return i;
            }
            return highestIndex;
        }

        /**
         * Descent rates and main deployment, by two-segment linear regression.
         *
         * @return index of the last descent sample used (ground tail excluded)
         */
        private int computeDescent(FlightStats s, double liftoffT) {
            // Start: skip the rounded top of the curve
            int start = s.apogeeIndex;
            while (start < n - 1 && t[start] < t[s.apogeeIndex] + POST_APOGEE_SKIP_SEC) start++;

            // End: exclude the samples lying on the ground
            int end = s.touchdownIndex;
            while (end > start && median3(end) <= s.groundAltitude + groundMargin) end--;

            int count = end - start + 1;
            if (count < 3) return end;

            // One straight line over the whole descent (used if no main deployment is found)
            double[] single = fitLine(start, end + 1);
            double singleRate = (single != null && single[0] < 0) ? -single[0] : Double.NaN;

            if (count >= 2 * MIN_SEGMENT_SAMPLES) {
                double bestError = Double.MAX_VALUE;
                double[] bestDrogue = null, bestMain = null;
                int bestK = -1;
                // k = first sample of the main segment; it is shared by both segments
                for (int k = start + MIN_SEGMENT_SAMPLES; k <= end + 1 - MIN_SEGMENT_SAMPLES; k++) {
                    double[] drogue = fitLine(start, k + 1);
                    double[] main = fitLine(k, end + 1);
                    if (drogue == null || main == null) continue;
                    double error = drogue[2] + main[2];
                    if (error < bestError) {
                        bestError = error;
                        bestDrogue = drogue;
                        bestMain = main;
                        bestK = k;
                    }
                }

                if (bestK >= 0 && bestDrogue[0] < 0 && bestMain[0] < 0
                        && Math.abs(bestMain[0]) <= MAX_MAIN_TO_DROGUE_RATE_RATIO * Math.abs(bestDrogue[0])) {
                    // Deployment at the intersection of the two lines (robust to noise)
                    double tDeploy = (bestMain[1] - bestDrogue[1]) / (bestDrogue[0] - bestMain[0]);
                    if (!(tDeploy > t[start] && tDeploy < t[end])) tDeploy = t[bestK];
                    double altDeploy = bestDrogue[0] * tDeploy + bestDrogue[1];

                    if (altDeploy > s.groundAltitude + groundMargin && altDeploy < s.apogeeAltitude) {
                        s.maxDescentRate = -bestDrogue[0];
                        s.mainDescentRate = -bestMain[0];
                        s.mainDeployAltitude = altDeploy;
                        s.mainDeployTimeSec = tDeploy - liftoffT;
                        return end;
                    }
                }
            }

            // No distinct main deployment: single descent rate
            s.maxDescentRate = singleRate;
            return end;
        }

        /** Landing speed: linear regression over the last part of the descent. */
        private void computeLandingSpeed(FlightStats s, int descentEnd) {
            int from = -1;
            for (int i = s.apogeeIndex + 1; i <= descentEnd; i++) {
                if (y[i] <= s.groundAltitude + landingWindow) {
                    from = i;
                    break;
                }
            }
            if (from >= 0 && descentEnd - from + 1 >= MIN_LANDING_SAMPLES
                    && t[descentEnd] - t[from] >= MIN_LANDING_DURATION_SEC) {
                double[] line = fitLine(from, descentEnd + 1);
                if (line != null && line[0] < 0) {
                    s.landingSpeed = -line[0];
                    return;
                }
            }
            // Not enough data close to the ground: use the last descent rate found
            s.landingSpeed = !Double.isNaN(s.mainDescentRate) ? s.mainDescentRate : s.maxDescentRate;
        }

        /** Flight duration from liftoff to touchdown (estimated if the recording stopped early). */
        private void computeDuration(FlightStats s, double liftoffT) {
            double duration = t[s.touchdownIndex] - liftoffT;
            if (!s.touchdownRecorded && s.apogeeIndex < n - 1
                    && !Double.isNaN(s.landingSpeed) && s.landingSpeed > 0) {
                double remainingAlt = y[n - 1] - s.groundAltitude;
                if (remainingAlt > 0) {
                    duration += remainingAlt / s.landingSpeed;
                    s.flightDurationEstimated = true;
                }
            }
            s.flightDurationSec = Math.max(0.0, duration);
        }

        // ---------------------------------------------------------------------------------------
        // Helpers
        // ---------------------------------------------------------------------------------------

        private double medianOfFirstSamples(int count) {
            int c = Math.min(count, n);
            double[] v = new double[c];
            System.arraycopy(y, 0, v, 0, c);
            Arrays.sort(v);
            return (c % 2 == 1) ? v[c / 2] : (v[c / 2 - 1] + v[c / 2]) / 2.0;
        }

        /**
         * Liftoff = start of the climb: first find a point clearly in flight (10 m / 30 ft above
         * the ground, median filtered), then walk back until the altitude is back near the pad.
         * A noisy sample on the pad therefore cannot trigger an early liftoff.
         * If the recording starts in flight, liftoff is the first sample.
         */
        private int findLiftoffIndex(double ground) {
            double clearlyFlying = ground + 10.0 * (liftoffThreshold / LIFTOFF_THRESHOLD_M);
            int i = 0;
            while (i < n && median3(i) <= clearlyFlying) i++;
            if (i >= n) return 0;
            while (i > 0 && median3(i - 1) > ground + liftoffThreshold) i--;
            return (i > 0) ? i - 1 : 0;
        }

        /** Median of samples i-1, i, i+1 (raw value at both ends of the curve). */
        private double median3(int i) {
            if (i <= 0 || i >= n - 1) return y[i];
            double a = y[i - 1], b = y[i], c = y[i + 1];
            return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
        }

        /** Vertical speed at sample i: slope of a linear regression over +/- halfWindowSec. */
        private double verticalSpeedAt(int i, double halfWindowSec) {
            int lo = i, hi = i;
            while (lo > 0 && (t[i] - t[lo - 1] <= halfWindowSec || i - lo < 2)) lo--;
            while (hi < n - 1 && (t[hi + 1] - t[i] <= halfWindowSec || hi - i < 2)) hi++;
            double[] line = fitLine(lo, hi + 1);
            return line == null ? Double.NaN : line[0];
        }

        /**
         * Least-squares straight line y = a * t + b over samples [from, to).
         *
         * @return {a, b, sumOfSquaredErrors}, or null if the segment is degenerate
         */
        private double[] fitLine(int from, int to) {
            int count = to - from;
            if (count < 2) return null;

            double st = sT[to] - sT[from];
            double sy = sY[to] - sY[from];
            double stt = sTT[to] - sTT[from];
            double sty = sTY[to] - sTY[from];
            double syy = sYY[to] - sYY[from];

            double varT = stt - st * st / count;  // centered sum of t^2
            if (varT <= 1e-9) return null;
            double covTY = sty - st * sy / count; // centered sum of t*y
            double varY = syy - sy * sy / count;  // centered sum of y^2

            double a = covTY / varT;
            double b = (sy - a * st) / count;
            double sse = Math.max(0.0, varY - covTY * covTY / varT);
            return new double[]{a, b, sse};
        }
    }
    // ---- FlightStatsCalculator (end) ----
}