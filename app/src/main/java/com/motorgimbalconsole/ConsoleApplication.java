package com.motorgimbalconsole;
/**
 * @description: This manages all the data retrieval
 * @author: boris.dureau@neuf.fr
 **/

import com.motorgimbalconsole.utils.NumberParser;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.motorgimbalconsole.config.AppConfigData;
import com.motorgimbalconsole.config.GimbalConfigData;
import com.motorgimbalconsole.connection.BluetoothConnection;
import com.motorgimbalconsole.connection.UsbConnection;
import com.motorgimbalconsole.flights.AccelerationUtils;
import com.motorgimbalconsole.flights.FlightData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


/**
 *   @description: This is quite a major class used everywhere because it can point to your connection, appconfig
 *   @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private boolean isConnected = false;
    // Written by the reader threads and read by the UI thread: volatile so that every thread
    // sees the latest value
    private static volatile boolean DataReady = false;
    // Store number of flight
    public volatile int NbrOfFlight = 0;
    private volatile FlightData MyFlight = null;
    private GlobalConfig AppConf = null;
    private String address, moduleName;
    private volatile String myTypeOfConnection = "bluetooth";// "USB";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;
    private GimbalConfigData GimbalCfg = null;
    private TestTrame testTrame = null;
    // Handler of the screen currently displaying live values; read by the reader thread
    private volatile Handler mHandler;

    /**
     * Registers the handler of the screen that receives the parsed telemetry values.
     *
     * @param mHandler UI thread handler of the screen, or null
     */
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * Unregisters a screen handler, but only if it is still the current one: a screen being
     * destroyed must not remove the handler that the next screen has already registered.
     *
     * @param handler handler of the screen being destroyed
     */
    public void clearHandler(Handler handler) {
        if (this.mHandler == handler) {
            this.mHandler = null;
        }
    }

    /** "AppLanguage" setting: follow the phone language (index 0 of the language spinner). */
    public static final int LANGUAGE_PHONE = 0;
    /** "AppLanguage" setting: force English (index 1 of the language spinner). */
    public static final int LANGUAGE_ENGLISH = 1;

    /**
     * Single source of truth for the "AppLanguage" setting, shared by every activity.
     * The values match the order of AppConfigData.itemsLanguages:
     * 0 = phone language, 1 = English.
     *
     * The phone language is read from the system resources and not from Locale.getDefault():
     * once English has been forced, Locale.getDefault() stays English for the whole process,
     * so switching back to "phone language" would otherwise only work after a restart.
     *
     * @param appLanguage value of the "AppLanguage" setting
     * @return the locale the user interface must use
     */
    public static Locale resolveAppLocale(int appLanguage) {
        if (appLanguage == LANGUAGE_ENGLISH) {
            return Locale.ENGLISH;
        }
        return Resources.getSystem().getConfiguration().getLocales().get(0);
    }

    /**
     * Returns a context whose resources use the language selected in the application settings.
     *
     * @param context     base context to wrap
     * @param appLanguage value of the "AppLanguage" setting (0 = phone language, 1 = English)
     * @return the localized context
     */
    public static Context updateBaseContextLocale(Context context, int appLanguage) {
        Locale locale = resolveAppLocale(appLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = base.getSharedPreferences("GimbalConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(updateBaseContextLocale(base, lang));
    }

    private volatile boolean exit = false;

    private static final String TAG = "ConsoleApplication";
    /** Max wait for the rest of a sentence once '$' has been received. */
    private static final long SENTENCE_TIMEOUT_MS = 1000;
    /** Settle delay before the "h;" handshake. */
    private static final long VALIDATION_SETTLE_MS = 500;
    private static final long VALIDATION_TIMEOUT_MS = 2000;
    private static final int VALIDATION_ATTEMPTS = 3;
    public long lastReceived = 0;
    public String lastData;
    public int currentFlightNbr = 0;
    public String commandRet = "";
    private double FEET_IN_METER = 1;

    /** The single application object, set in onCreate() before any activity or fragment exists. */
    private static volatile ConsoleApplication instance;

    /**
     * Returns the application object (link, configuration, downloaded flights).
     * Fragments use it instead of receiving it as a constructor parameter: Android re-creates
     * fragments with their no-argument constructor (rotation, process restore), so values
     * passed to another constructor were lost and the fragment crashed.
     *
     * @return the application object, never null once the application has started
     */
    public static ConsoleApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        MyFlight = new FlightData(this);
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        AppConf = new GlobalConfig(this);
        AppConf.ReadConfig();
        GimbalCfg = new GimbalConfigData();
        testTrame = new TestTrame();
        myTypeOfConnection = AppConf.getConnectionTypeValue();

    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    public String getConnectionType() {
        return myTypeOfConnection;
    }

    public void setAddress(String bTAddress) {
        address = bTAddress;
    }

    public String getAddress() {
        return address;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String bTmoduleName) {
        moduleName = bTmoduleName;
    }

    public int getNbrOfFlights () {
        return NbrOfFlight;
    }
    public InputStream getInputStream() {
        InputStream tmpIn = null;
        if (myTypeOfConnection.equals("bluetooth")) {
            tmpIn = BTCon.getInputStream();
        } else {
            tmpIn = UsbCon.getInputStream();
        }
        return tmpIn;
    }

    public void setConnected(boolean Connected) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public boolean getConnected() {
        boolean ret = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            ret = BTCon.getBTConnected();
        } else {
            ret = UsbCon.getUSBConnected();
        }
        return ret;
    }

    public GimbalConfigData getGimbalConfigData() {
        return GimbalCfg;
    }

    public TestTrame getTestTrame() {
        return testTrame;
    }
    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            Log.d(TAG, "BT socket open: " + state);
            if (!state || !isConnectionValid()) {
                safeDisconnect();
                state = false;
            }
        }
        return state;
    }

    public FlightData getFlightData() {
        return MyFlight;
    }

    // connect to the USB
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            Log.d(TAG, "USB port open: " + state);
            if (!state || !isConnectionValid()) {
                safeDisconnect();
                state = false;
            }
        }
        return state;
    }

    /**
     * Checks that the board answers "h;" with "$OK;".
     * Bounded in time (no busy-wait), with retries: right after a reconnection the
     * Bluetooth module / board may not relay the first bytes yet.
     */
    public boolean isConnectionValid() {
        // Let the link settle (BT module relay, USB board reset)
        sleepQuietly(VALIDATION_SETTLE_MS);

        for (int attempt = 1; attempt <= VALIDATION_ATTEMPTS; attempt++) {
            if (getInputStream() == null) {
                Log.w(TAG, "isConnectionValid: no input stream");
                return false;
            }
            setDataReady(false);
            flush();
            clearInput();
            drainStream(150); // throw away telemetry / stale replies from a previous session

            write("h;");
            flush();

            String myMessage = ReadResult(VALIDATION_TIMEOUT_MS);
            Log.d(TAG, "h; attempt " + attempt + " reply: '" + myMessage + "'");
            if ("OK".equals(myMessage)) {
                return true;
            }
            sleepQuietly(300);
        }
        return false;
    }

    private void safeDisconnect() {
        try {
            Disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Disconnect failed", e);
        }
    }

    /** Reads one byte, waiting at most until deadline. Returns -1 on timeout. */
    private int readByte(long deadline) throws IOException {
        InputStream in = getInputStream();
        if (in == null) throw new IOException("Input stream is null (link closed)");
        while (in.available() <= 0) {
            if (System.currentTimeMillis() > deadline) return -1;
            if (Thread.currentThread().isInterrupted()) throw new IOException("Interrupted");
            sleepQuietly(2);
        }
        int b = in.read();
        if (b < 0) throw new IOException("End of stream (link closed)");
        return b;
    }

    /** Discards whatever is pending on the input stream after a short settle delay. */
    private void drainStream(long settleMs) {
        sleepQuietly(settleMs);
        try {
            InputStream in = getInputStream();
            if (in == null) return;
            long deadline = System.currentTimeMillis() + 500;
            int n;
            while ((n = in.available()) > 0 && System.currentTimeMillis() < deadline) {
                //noinspection ResultOfMethodCallIgnored
                in.skip(n);
                sleepQuietly(20);
            }
        } catch (IOException ignored) {}
        clearInput();
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void Disconnect() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.Disconnect();
        } else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.write(data);
        } else {
            UsbCon.write(data);
        }
    }

    public void clearInput() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.clearInput();
        } else {
            UsbCon.clearInput();
        }
    }

    public void initFlightData() {
        MyFlight = new FlightData(this);

        if(AppConf.getUnits()== GlobalConfig.AltitudeUnit.METERS) {
            //if (AppConf.getUnitsValue().equals("Meters")) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }

    public void setExit(boolean b) {
        this.exit = b;
    }



    public long calculateSentenceCHK(String currentSentence[]) {
        long chk =0;
        String sentence="";

        for (int i=0; i< currentSentence.length-1; i++) {
            sentence = sentence + currentSentence[i] +",";
        }
        //Log.d("calculateSentenceCHK", sentence);
        chk = generateCheckSum(sentence);
        return chk;
    }

    public static Integer generateCheckSum(String value)  {

        byte[] data = value.getBytes();
        long checksum = 0L;

        for( byte b : data )  {
            checksum += b;
        }

        checksum = checksum % 256;

        return (int) checksum; // same value as before: fits in an int after the modulo

    }
    @SuppressLint("LongLogTag")
    public String ReadResult(long timeout) {
        // Reads sentences "$...;" until OK / end is received or the timeout expires.
        // Every wait is bounded: no blocking read, no infinite loop on a closed stream.
        this.exit = false;
        lastData = "";
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {
            while (!this.exit) {
                // A reader thread being stopped is interrupted (see TelemetryStreamReader):
                // the exit flag alone is not enough because it is reset when this method starts
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if ((System.currentTimeMillis() - lastReceived) > timeout) {
                    this.exit = true;
                    break;
                }
                InputStream in = getInputStream();
                if (in == null) {
                    throw new IOException("Input stream is null (link closed)");
                }
                if (in.available() <= 0) {
                    sleepQuietly(5);
                    continue;
                }
                int first = in.read();
                if (first < 0) {
                    throw new IOException("End of stream (link closed)");
                }
                char ch = (char) first;
                lastData = lastData + ch;
                if (ch != '$') {
                    continue; // noise, newline after ';', partial frame...
                }

                // Read the sentence until ';' with a bounded wait
                long sentenceDeadline = System.currentTimeMillis() + SENTENCE_TIMEOUT_MS;
                StringBuilder sb = new StringBuilder();
                boolean complete = false;
                while (true) {
                    int c = readByte(sentenceDeadline);
                    if (c < 0) break;               // timeout: truncated frame
                    ch = (char) c;
                    if (ch == ';') { complete = true; break; }
                    if (ch == '$') { sb.setLength(0); continue; } // new frame: resync
                    if (ch != '\r' && ch != '\n') sb.append(ch);
                }
                if (!complete) {
                    Log.w(TAG, "Incomplete sentence dropped: " + sb);
                    continue;
                }

                String tempBuff = sb.toString();
                if (tempBuff.isEmpty()) continue;
                String currentSentence[] = tempBuff.split(",");
                if (currentSentence.length == 0 || currentSentence[0] == null) continue;
                fullBuff = fullBuff + tempBuff;

                try {
                    long chk=0;
                    switch (currentSentence[0]) {
                        case "telemetry":
                            if (NumberParser.isNumber(currentSentence[currentSentence.length-1]))
                                chk = NumberParser.toLong(currentSentence[currentSentence.length-1]);
                            if (calculateSentenceCHK(currentSentence) == chk) {
                                Handler handler = mHandler;
                                if (handler != null) {
                                    dispatchTelemetry(handler, currentSentence, tempBuff);
                                }
                            }
                            break;

                        case "data":

                            if (NumberParser.isNumber(currentSentence[currentSentence.length-1]))
                                chk = NumberParser.toLong(currentSentence[currentSentence.length-1]);
                            Log.d("checksum", "check from sentence: " + chk);
                            Log.d("calculated checksum", "calculated check: " + calculateSentenceCHK(currentSentence));

                            String flightName = "FlightXX";
                            long time = 0;
                            if (calculateSentenceCHK(currentSentence) == chk) {

                                // Value 1 contain the flight number
                                if (currentSentence.length > 1)
                                    if (NumberParser.isNumber(currentSentence[1])) {
                                        currentFlightNbr = NumberParser.toInt(currentSentence[1]) + 1;
                                        if (currentFlightNbr < 10)
                                            flightName = getResources().getString(R.string.flight_name) + " "+ "0" + currentFlightNbr;
                                        else
                                            //flight
                                            flightName = getResources().getString(R.string.flight_name) + " " + currentFlightNbr;
                                    }
                                // value
                                // Value 2 contain the time
                                if (currentSentence.length > 2)
                                    if (NumberParser.isNumber(currentSentence[2]))
                                        time = NumberParser.toLong(currentSentence[2]);
                                // Value 3 contain the altitude
                                double altitude = 0;
                                if (currentSentence.length > 3) {
                                    if (NumberParser.isNumber(currentSentence[3]))
                                        altitude = NumberParser.toDouble(currentSentence[3]);
                                    // To do
                                    // Rounded rather than truncated: truncation biases toward zero
                                    MyFlight.AddToFlight(time,
                                            Math.round(altitude * FEET_IN_METER), flightName, 0);

                                }
                                //Value 4 contains the temperature
                                double temperature = 0;
                                if (currentSentence.length > 4) {
                                    if (NumberParser.isNumber(currentSentence[4]))
                                        temperature = NumberParser.toLong(currentSentence[4]);
                                    // To do

                                    MyFlight.AddToFlight(time,
                                            (long) (temperature), flightName, 1);
                                }
                                //Value 5 contains the pressure
                                double pressure = 0;
                                if (currentSentence.length > 5) {
                                    if (NumberParser.isNumber(currentSentence[5]))
                                        pressure = NumberParser.toLong(currentSentence[5]);
                                    // To do

                                    MyFlight.AddToFlight(time,
                                            (long) (pressure), flightName, 2);

                                }
                                // Then get the quaternion
                                QuaternionUtils quatUtils = new QuaternionUtils();
                                float quat[] = new float[4];
                                //w
                                if (currentSentence.length > 6) {
                                    quat[0] = quatUtils.decodeFloat(currentSentence[6]);
                                }
                                //x
                                if (currentSentence.length > 7) {
                                    quat[1] = quatUtils.decodeFloat(currentSentence[7]);
                                }
                                //y
                                if (currentSentence.length > 8) {
                                    quat[2] = quatUtils.decodeFloat(currentSentence[8]);
                                }
                                //z
                                if (currentSentence.length > 9) {
                                    quat[3] = quatUtils.decodeFloat(currentSentence[9]);

                                    float[] gravity = null;
                                    gravity = quatUtils.quaternionToGravity(quat);
                                    MyFlight.AddToFlight(time,
                                            gravity[0], flightName, 3);
                                    MyFlight.AddToFlight(time,
                                            gravity[1], flightName, 4);
                                    MyFlight.AddToFlight(time,
                                            gravity[2], flightName, 5);

                                    float[] euler = null;
                                    euler = quatUtils.quaternionToEuler(quat);
                                    MyFlight.AddToFlight(time,
                                            euler[0] * 180 / 3.14, flightName, 6);
                                    MyFlight.AddToFlight(time,
                                            euler[1] * 180 / 3.14, flightName, 7);
                                    MyFlight.AddToFlight(time,
                                            euler[2] * 180 / 3.14, flightName, 8);

                                    float[] ypr = null;
                                    ypr = quatUtils.quaternionToYawPitchRoll(quat, gravity);
                                    MyFlight.AddToFlight(time,
                                            ypr[0] * 180 / 3.14, flightName, 9);
                                    MyFlight.AddToFlight(time,
                                            ypr[1] * 180 / 3.14, flightName, 10);
                                    MyFlight.AddToFlight(time,
                                            ypr[2] * 180 / 3.14, flightName, 11);
                                }
                                //outputX
                                long outputX = 0;
                                if (currentSentence.length > 10) {
                                    if (NumberParser.isNumber(currentSentence[10])) {
                                        outputX = NumberParser.toLong(currentSentence[10]);
                                        MyFlight.AddToFlight(time, (long) (outputX), flightName, 12);
                                    }
                                }
                                //outputY
                                long outputY = 0;
                                if (currentSentence.length > 11) {
                                    if (NumberParser.isNumber(currentSentence[11])) {
                                        outputY = NumberParser.toLong(currentSentence[11]);
                                        MyFlight.AddToFlight(time, (long) (outputY), flightName, 13);
                                    }
                                }
                                long accelX = 0;
                                if (currentSentence.length > 12) {
                                    if (NumberParser.isNumber(currentSentence[12])) {
                                        accelX = NumberParser.toLong(currentSentence[12]);
                                        MyFlight.AddToFlight(time, (long) (accelX), flightName, 14);
                                    }
                                }
                                long accelY = 0;
                                if (currentSentence.length > 13) {
                                    if (NumberParser.isNumber(currentSentence[13])) {
                                        accelY = NumberParser.toLong(currentSentence[13]);
                                        MyFlight.AddToFlight(time, (long) (accelY), flightName, 15);
                                    }
                                }
                                long accelZ = 0;
                                if (currentSentence.length > 14) {
                                    if (NumberParser.isNumber(currentSentence[14])) {
                                        accelZ = NumberParser.toLong(currentSentence[14]);
                                        MyFlight.AddToFlight(time, (long) (accelZ), flightName, 16);
                                    }
                                }
                                // Derived curve: magnitude of the acceleration vector in G, computed
                                // only when the three axes are present and valid in this sentence
                                if (currentSentence.length > 14
                                        && NumberParser.isNumber(currentSentence[12])
                                        && NumberParser.isNumber(currentSentence[13])
                                        && NumberParser.isNumber(currentSentence[14])) {
                                    double accelG = AccelerationUtils.magnitudeInG(accelX, accelY, accelZ,
                                            GimbalCfg.getAltimeterName(), GimbalCfg.getAcceleroRange());
                                    MyFlight.AddToFlight(time, accelG, flightName, FlightData.SERIE_ACCEL);
                                }
                            }
                            break;
                        case "alticonfig":

                            if (NumberParser.isNumber(currentSentence[currentSentence.length-1]))
                                chk = NumberParser.toLong(currentSentence[currentSentence.length-1]);
                            Log.d("checksum", "check from sentence: " + chk);
                            Log.d("calculated checksum", "calculated check: " + calculateSentenceCHK(currentSentence));
                            if (calculateSentenceCHK(currentSentence) == chk) {

                                // Value 0 contains the AltimeterName
                                if (currentSentence.length > 1)
                                    GimbalCfg.setAltimeterName(currentSentence[1]);
                                //Value 1 contains ax_offset
                                if (currentSentence.length > 2)
                                    if (NumberParser.isNumber(currentSentence[2]))
                                        GimbalCfg.setAxOffset(NumberParser.toInt(currentSentence[2]));
                                    else
                                        GimbalCfg.setAxOffset(0);
                                // Value 2 contains ay_offset
                                if (currentSentence.length > 3)
                                    if (NumberParser.isNumber(currentSentence[3]))
                                        GimbalCfg.setAyOffset(NumberParser.toInt(currentSentence[3]));
                                    else
                                        GimbalCfg.setAyOffset(0);
                                // Value 3 contains az_offset
                                if (currentSentence.length > 4)
                                    if (NumberParser.isNumber(currentSentence[4]))
                                        GimbalCfg.setAzOffset(NumberParser.toInt(currentSentence[4]));
                                    else
                                        GimbalCfg.setAzOffset(0);
                                // Value 4 contains gx_offset
                                if (currentSentence.length > 5)
                                    if (NumberParser.isNumber(currentSentence[5]))
                                        GimbalCfg.setGxOffset(NumberParser.toInt(currentSentence[5]));
                                    else
                                        GimbalCfg.setGxOffset(0);
                                // Value 5 contains gy_offset
                                if (currentSentence.length > 6)
                                    if (NumberParser.isNumber(currentSentence[6]))
                                        GimbalCfg.setGyOffset(NumberParser.toInt(currentSentence[6]));
                                    else
                                        GimbalCfg.setGyOffset(0);
                                // Value 6 contains gz_offset
                                if (currentSentence.length > 7)
                                    if (NumberParser.isNumber(currentSentence[7]))
                                        GimbalCfg.setGzOffset(NumberParser.toInt(currentSentence[7]));
                                    else
                                        GimbalCfg.setGzOffset(0);
                                // Value 7 contains KpX
                                if (currentSentence.length > 8)
                                    if (NumberParser.isNumber(currentSentence[8]))
                                        GimbalCfg.setKpX(NumberParser.toDouble(currentSentence[8]) / 100);
                                    else
                                        GimbalCfg.setKpX(0.0);
                                // Value 8 contains KiX
                                if (currentSentence.length > 9)
                                    if (NumberParser.isNumber(currentSentence[9]))
                                        GimbalCfg.setKiX(NumberParser.toDouble(currentSentence[9]) / 100);
                                    else
                                        GimbalCfg.setKiX(0.0);
                                // Value 9 contains KdX
                                if (currentSentence.length > 10)
                                    if (NumberParser.isNumber(currentSentence[10]))
                                        GimbalCfg.setKdX(NumberParser.toDouble(currentSentence[10]) / 100);
                                    else
                                        GimbalCfg.setKdX(0.0);
                                // Value 10 contains KpY
                                if (currentSentence.length > 11)
                                    if (NumberParser.isNumber(currentSentence[11]))
                                        GimbalCfg.setKpY(NumberParser.toDouble(currentSentence[11]) / 100);
                                    else
                                        GimbalCfg.setKpY(0.0);
                                // Value 11 contains KiY
                                if (currentSentence.length > 12)
                                    if (NumberParser.isNumber(currentSentence[12]))
                                        GimbalCfg.setKiY(NumberParser.toDouble(currentSentence[12]) / 100);
                                    else
                                        GimbalCfg.setKiY(0.0);
                                // Value 12 contains KdY
                                if (currentSentence.length > 13)
                                    if (NumberParser.isNumber(currentSentence[13]))
                                        GimbalCfg.setKdY(NumberParser.toDouble(currentSentence[13]) / 100);
                                    else
                                        GimbalCfg.setKdY(0.0);
                                // Value 13 contains servoXMin
                                if (currentSentence.length > 14)
                                    if (NumberParser.isNumber(currentSentence[14]))
                                        GimbalCfg.setServoXMin(NumberParser.toInt(currentSentence[14]));
                                    else
                                        GimbalCfg.setServoXMin(0);
                                // Value 14 contains servoXMax
                                if (currentSentence.length > 15)
                                    if (NumberParser.isNumber(currentSentence[15]))
                                        GimbalCfg.setServoXMax(NumberParser.toInt(currentSentence[15]));
                                    else
                                        GimbalCfg.setServoXMax(0);
                                // Value 15 contains servoYMin
                                if (currentSentence.length > 16)
                                    if (NumberParser.isNumber(currentSentence[16]))
                                        GimbalCfg.setServoYMin(NumberParser.toInt(currentSentence[16]));
                                    else
                                        GimbalCfg.setServoYMin(0);
                                // Value 16 contains servoYMax
                                if (currentSentence.length > 17)
                                    if (NumberParser.isNumber(currentSentence[17]))
                                        GimbalCfg.setServoYMax(NumberParser.toInt(currentSentence[17]));
                                    else
                                        GimbalCfg.setServoYMax(0);
                                // Value 17 contains the connection speed
                                if (currentSentence.length > 18)
                                    if (NumberParser.isNumber(currentSentence[18]))
                                        GimbalCfg.setConnectionSpeed(NumberParser.toInt(currentSentence[18]));
                                    else
                                        GimbalCfg.setConnectionSpeed(38400);
                                // Value 18 contains the altimeter resolution
                                if (currentSentence.length > 19)
                                    if (NumberParser.isNumber(currentSentence[19]))
                                        GimbalCfg.setAltimeterResolution(NumberParser.toInt(currentSentence[19]));
                                    else
                                        GimbalCfg.setAltimeterResolution(0);
                                // Value 19 contains the eeprom size
                                if (currentSentence.length > 20)
                                    if (NumberParser.isNumber(currentSentence[20]))
                                        GimbalCfg.setEepromSize(NumberParser.toInt(currentSentence[20]));
                                    else
                                        GimbalCfg.setEepromSize(512);
                                // Value 20
                                if (currentSentence.length > 21)
                                    if (NumberParser.isNumber(currentSentence[21]))
                                        GimbalCfg.setAltiMajorVersion(NumberParser.toInt(currentSentence[21]));
                                // Value 21
                                if (currentSentence.length > 22)
                                    if (NumberParser.isNumber(currentSentence[22]))
                                        GimbalCfg.setAltiMinorVersion(NumberParser.toInt(currentSentence[22]));
                                // Value 22 units
                                if (currentSentence.length > 23)
                                    if (NumberParser.isNumber(currentSentence[23]))
                                        GimbalCfg.setUnits(NumberParser.toInt(currentSentence[23]));
                                    else
                                        GimbalCfg.setUnits(0);
                                // Value 23 endRecordAltitude
                                if (currentSentence.length > 24)
                                    if (NumberParser.isNumber(currentSentence[24]))
                                        GimbalCfg.setEndRecordAltitude(NumberParser.toInt(currentSentence[24]));
                                    else
                                        GimbalCfg.setEndRecordAltitude(5);
                                // Value 24 beepingFrequency
                                if (currentSentence.length > 25)
                                    if (NumberParser.isNumber(currentSentence[25]))
                                        GimbalCfg.setBeepingFrequency(NumberParser.toInt(currentSentence[25]));
                                    else
                                        GimbalCfg.setBeepingFrequency(440);
                                // value 25 LaunchDetect
                                if (currentSentence.length > 26)
                                    if (NumberParser.isNumber(currentSentence[26]))
                                        GimbalCfg.setLiftOffDetect(NumberParser.toInt(currentSentence[26]));
                                    else
                                        GimbalCfg.setLiftOffDetect(0);
                                // value 26 Gyro Range
                                if (currentSentence.length > 27)
                                    if (NumberParser.isNumber(currentSentence[27]))
                                        GimbalCfg.setGyroRange(NumberParser.toInt(currentSentence[27]));
                                    else
                                        GimbalCfg.setGyroRange(0);

                                // value 27 Accelero Range
                                if (currentSentence.length > 28)
                                    if (NumberParser.isNumber(currentSentence[28]))
                                        GimbalCfg.setAcceleroRange(NumberParser.toInt(currentSentence[28]));
                                    else
                                        GimbalCfg.setAcceleroRange(0);
                                // value 28 Recording timout
                                if (currentSentence.length > 29)
                                    if (NumberParser.isNumber(currentSentence[29]))
                                        GimbalCfg.setRecordingTimeout(NumberParser.toInt(currentSentence[29]));
                                    else
                                        GimbalCfg.setRecordingTimeout(0);
                                // value 29 the battery type
                                if (currentSentence.length > 30)
                                    if (NumberParser.isNumber(currentSentence[30]))
                                        GimbalCfg.setBatteryType(NumberParser.toInt(currentSentence[30]));
                                    else
                                        GimbalCfg.setBatteryType(0);

                                // value 30 the telemetry type
                                if (currentSentence.length > 31)
                                    if (NumberParser.isNumber(currentSentence[31]))
                                        GimbalCfg.setTelemetryType(NumberParser.toInt(currentSentence[31]));
                                    else
                                        GimbalCfg.setTelemetryType(0);
                                //DataReady = true;
                            }
                            myMessage = myMessage + " " + "alticonfig";
                            break;
                        case "testTrame":
                            if (NumberParser.isNumber(currentSentence[currentSentence.length - 1]))
                                chk = NumberParser.toLong(currentSentence[currentSentence.length - 1]);
                            if (calculateSentenceCHK(currentSentence) == chk) {
                                testTrame.setTrameStatus(true);
                            } else
                            {
                                testTrame.setTrameStatus(false);
                            }
                            if (currentSentence.length > 1)
                                testTrame.setCurrentTrame(currentSentence[1]);
                            else
                                testTrame.setCurrentTrame("Error reading packet");
                            myMessage = myMessage + " " + "testTrame";
                            break;
                        case "nbrOfFlight":
                            // Value 1 contains the number of flight
                            if (currentSentence.length > 1)
                                if (NumberParser.isNumber(currentSentence[1]))
                                    NbrOfFlight = (NumberParser.toInt(currentSentence[1]));
                            myMessage = myMessage + " " + "nbrOfFlight";
                            break;
                        case "start":
                            // We are starting reading data
                            setDataReady(false);
                            myMessage = "start";
                            break;
                        case "end":
                            // We have finished reading data
                            setDataReady(true);
                            myMessage = myMessage + " " + "end";
                            exit = true;
                            break;
                        case "OK":
                            setDataReady(true);
                            if (currentSentence.length > 0)
                                commandRet = currentSentence[0];
                            myMessage = "OK";
                            exit = true;
                            break;
                        case "KO":
                            setDataReady(true);
                            if (currentSentence.length > 0)
                                commandRet = currentSentence[0];

                            break;
                        case "UNKNOWN":
                            setDataReady(true);
                            if (currentSentence.length > 0)
                                commandRet = currentSentence[0];

                            break;
                        default:

                            break;
                    }
                } catch (RuntimeException e) {
                    // A malformed sentence must never kill the reading thread
                    Log.w(TAG, "Malformed sentence ignored: " + tempBuff, e);
                }
            }
        } catch (IOException e) {
            // Link closed or broken while reading
            myMessage = myMessage + " " + "error";
        }
        return myMessage;
    }

    /**
     * Forwards the fields of a valid "$telemetry" sentence to the screen handler.
     * The message "what" codes are unchanged (see GimbalTelemetryTabActivity and
     * GimbalTabStatusActivity); only the ORDER in which they are sent matters here:
     *  1. sample time (27) first, so that the altitude of this frame is stamped with the time
     *     of this frame (it used to receive the time of the previous frame);
     *  2. lift-off / apogee events (23, 24, 25) before the altitude, so that the first
     *     altitude sample after lift-off is already plotted;
     *  3. the sensor values;
     *  4. landed (26) after the altitude, so that the landing altitude is the one of this frame.
     *
     * @param handler  UI handler of the current screen
     * @param s        sentence fields (s[0] = "telemetry", last field = checksum)
     * @param rawFrame complete sentence, for the logs
     */
    private void dispatchTelemetry(Handler handler, String[] s, String rawFrame) {
        // 1. Sample time
        sendTelemetryField(handler, 27, s, 27, "0", rawFrame);
        // 2. Flight events
        sendTelemetryField(handler, 23, s, 23, "0", rawFrame);      // lift off
        sendTelemetryField(handler, 24, s, 24, "0", rawFrame);      // apogee reached
        sendTelemetryField(handler, 25, s, 25, "0", rawFrame);      // apogee altitude
        // 3. Values
        if (s.length > 1) {
            handler.obtainMessage(0, String.valueOf(s[1])).sendToTarget(); // board name
        }
        for (int what = 1; what <= 9; what++) {
            // gyro X/Y/Z, accel X/Y/Z, orientation X/Y/Z
            sendTelemetryField(handler, what, s, what + 1, String.valueOf(-0.0), rawFrame);
        }
        sendTelemetryField(handler, 10, s, 11, String.valueOf(0), rawFrame);       // altitude
        sendTelemetryField(handler, 11, s, 12, String.valueOf(-0.0), rawFrame);    // temperature
        sendTelemetryField(handler, 12, s, 13, String.valueOf(0.0), rawFrame);     // pressure
        sendTelemetryField(handler, 13, s, 14, String.valueOf(-0.0), rawFrame);    // battery voltage
        if (s.length > 18) {
            // quaternion (4 hexadecimal encoded floats) for the 3D rocket view
            handler.obtainMessage(14, s[15] + "," + s[16] + "," + s[17] + "," + s[18]).sendToTarget();
        }
        sendTelemetryField(handler, 19, s, 19, null, rawFrame);                    // eeprom usage
        sendTelemetryField(handler, 20, s, 20, null, rawFrame);                    // correction
        sendTelemetryField(handler, 21, s, 21, null, rawFrame);                    // servo X
        sendTelemetryField(handler, 22, s, 22, null, rawFrame);                    // servo Y
        // 4. Landing, then number of flights
        sendTelemetryField(handler, 26, s, 26, "0", rawFrame);
        sendTelemetryField(handler, 28, s, 28, "0", rawFrame);
    }

    /**
     * Sends one numeric telemetry field to the screen handler.
     *
     * @param handler      UI handler of the current screen
     * @param what         message code expected by the screens
     * @param s            sentence fields
     * @param index        index of the field in the sentence
     * @param defaultValue value sent when the field is not a number, or null to send nothing
     * @param rawFrame     complete sentence, logged when a field is invalid
     */
    private void sendTelemetryField(Handler handler, int what, String[] s, int index,
                                    String defaultValue, String rawFrame) {
        if (s.length <= index) return; // older firmware: field not present
        if (NumberParser.isNumber(s[index])) {
            handler.obtainMessage(what, s[index]).sendToTarget();
        } else {
            if (what >= 20) {
                Log.d(TAG, "Invalid telemetry field " + index + ": " + rawFrame);
            }
            if (defaultValue != null) {
                handler.obtainMessage(what, defaultValue).sendToTarget();
            }
        }
    }

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }


    /**
     * Builds a configuration holding the locale selected in the application settings.
     * Uses the same mapping as updateBaseContextLocale (0 = phone language, 1 = English);
     * it previously mapped 1 to French and 2 to English, which did not match the settings menu.
     *
     * @return a configuration carrying the application locale
     */
    public Configuration getAppLocal() {
        Configuration config = new Configuration();
        config.setLocale(resolveAppLocale(AppConf.getApplicationLanguage()));
        return config;
    }


    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf = value;
    }

    public class TestTrame {

        private String currentTrame = "";
        private boolean trameStatus = false;

        public void setCurrentTrame (String trame) {
            currentTrame = trame;
        }

        public String getCurrentTrame() {
            return currentTrame;
        }

        public void setTrameStatus (boolean val) {
            trameStatus = val;
        }

        public boolean getTrameStatus () {
            return trameStatus;
        }
    }

}