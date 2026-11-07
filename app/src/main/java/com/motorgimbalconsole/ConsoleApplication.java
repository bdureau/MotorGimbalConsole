package com.motorgimbalconsole;
/**
 * @description: This manages all the data retrieval
 * @author: boris.dureau@neuf.fr
 **/

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.motorgimbalconsole.config.AppConfigData;
import com.motorgimbalconsole.config.GimbalConfigData;
import com.motorgimbalconsole.connection.BluetoothConnection;
import com.motorgimbalconsole.connection.UsbConnection;
import com.motorgimbalconsole.flights.FlightData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


/**
 *   @description: This is quite a major class used everywhere because it can point to your connection, appconfig
 *   @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private boolean isConnected = false;
    private static boolean DataReady = false;
    // Store number of flight
    public int NbrOfFlight = 0;
    private FlightData MyFlight = null;
    private GlobalConfig AppConf = null;
    private String address, moduleName;
    private String myTypeOfConnection = "bluetooth";// "USB";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;
    private GimbalConfigData GimbalCfg = null;
    private TestTrame testTrame = null;
    private Handler mHandler;

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * Met à jour la locale du contexte en fonction de la configuration de l'application.
     * 0 = Langue du téléphone, 1 = Anglais
     */
    public static Context updateBaseContextLocale(Context context, int appLanguage) {
        Locale locale;
        if (appLanguage == 1) {
            locale = Locale.ENGLISH;
        } else {
            locale = Locale.getDefault();
        }
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

    @Override
    public void onCreate() {
        super.onCreate();
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

    public void appendLog(String text) {
        //File logFile = new File("sdcard/debugfile.txt");
        //File logFile = new File(Environment.getDataDirectory() +"/debugfile.txt");
        //getExternalStorageDirectory()

        File logFile = new File(Environment.getExternalStorageDirectory() +"/test/debugfile.txt");
        //Environment.getDownloadCacheDirectory()
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        return new Long( checksum ).intValue();

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
                            if (currentSentence[currentSentence.length-1].matches("\\d+(?:\\.\\d+)?"))
                                chk = Long.valueOf(currentSentence[currentSentence.length-1]);
                            //Log.d("checksum", "check from sentence: " + chk);
                            if (calculateSentenceCHK(currentSentence) == chk) {
                                //Log.d("calculated checksum", "calculated check: " + calculateSentenceCHK(currentSentence));
                                if (mHandler != null) {
                                    // Value 1 contains the altimeter name
                                    if (currentSentence.length > 1)
                                        mHandler.obtainMessage(0, String.valueOf(currentSentence[1])).sendToTarget();
                                    // Value 1 contains the GyroX
                                    if (currentSentence.length > 2)
                                        if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(1, String.valueOf(currentSentence[2])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(1, String.valueOf(-0.0)).sendToTarget();
                                    // Value 2 contains the GyroY
                                    if (currentSentence.length > 3)
                                        if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(2, String.valueOf(currentSentence[3])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(2, String.valueOf(-0.0)).sendToTarget();
                                    // Value 3 contains the GyroZ
                                    if (currentSentence.length > 4)
                                        if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(3, String.valueOf(currentSentence[4])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(3, String.valueOf(-0.0)).sendToTarget();
                                    //Value 4 contains the AccelX
                                    if (currentSentence.length > 5)
                                        if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(4, String.valueOf(currentSentence[5])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(4, String.valueOf(-0.0)).sendToTarget();
                                    // Value 5 contains the AccelY
                                    if (currentSentence.length > 6)
                                        if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(5, String.valueOf(currentSentence[6])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(5, String.valueOf(-0.0)).sendToTarget();
                                    // Value 6 contains the AccelZ
                                    if (currentSentence.length > 7)
                                        if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(6, String.valueOf(currentSentence[7])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(6, String.valueOf(-0.0)).sendToTarget();
                                    // Value 7 contains the OrientX
                                    if (currentSentence.length > 8)
                                        if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(7, String.valueOf(currentSentence[8])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(7, String.valueOf(-0.0)).sendToTarget();
                                    // value 8 contains the OrientY
                                    if (currentSentence.length > 9)
                                        if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(8, String.valueOf(currentSentence[9])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(8, String.valueOf(-0.0)).sendToTarget();
                                    // Value 9 contains the OrientZ
                                    if (currentSentence.length > 10)
                                        if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(9, String.valueOf(currentSentence[10])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(9, String.valueOf(-0.0)).sendToTarget();
                                    // Value 10 contains the altitude
                                    if (currentSentence.length > 11)
                                        if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(10, String.valueOf(currentSentence[11])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(10, String.valueOf(0)).sendToTarget();
                                    // Value 11 contains the temperature
                                    if (currentSentence.length > 12)
                                        if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(11, String.valueOf(currentSentence[12])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(11, String.valueOf(-0.0)).sendToTarget();
                                    // Value 12 contains the pressure
                                    if (currentSentence.length > 13)
                                        if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(12, String.valueOf(currentSentence[13])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(12, String.valueOf(0.0)).sendToTarget();
                                    // Value 13 contains the battery voltage
                                    if (currentSentence.length > 14)
                                        if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(13, String.valueOf(currentSentence[14])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(13, String.valueOf(-0.0)).sendToTarget();
                                    // Value 14 contains graph
                                    if (currentSentence.length > 18)
                                        mHandler.obtainMessage(14, String.valueOf(currentSentence[15] + "," +
                                                currentSentence[16] + "," + currentSentence[17] + "," + currentSentence[18])).sendToTarget();
                                    // Value 19 contains the eeprom usage
                                    if (currentSentence.length > 19)
                                        if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(19, String.valueOf(currentSentence[19])).sendToTarget();
                                    // Value 20 contains the correction
                                    if (currentSentence.length > 20)
                                        if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(20, String.valueOf(currentSentence[20])).sendToTarget();
                                        else
                                            Log.d("Console - correction", tempBuff);
                                    // Value 21 contains ServoX
                                    if (currentSentence.length > 21)
                                        if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(21, String.valueOf(currentSentence[21])).sendToTarget();
                                        else
                                            Log.d("Console - ServoX", tempBuff);
                                    // Value 22 contains ServoY
                                    if (currentSentence.length > 22)
                                        if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(22, String.valueOf(currentSentence[22])).sendToTarget();
                                        else
                                            Log.d("Console - ServoY", tempBuff);

                                    // Value 23 contains liftoff
                                    if (currentSentence.length > 23)
                                        if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(23, String.valueOf(currentSentence[23])).sendToTarget();
                                        else {
                                            mHandler.obtainMessage(23, String.valueOf(0)).sendToTarget();

                                            Log.d("Console - liftoff", tempBuff);
                                        }
                                    // Value 24 contains apogee
                                    if (currentSentence.length > 24)
                                        if (currentSentence[24].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(24, String.valueOf(currentSentence[24])).sendToTarget();
                                        else {
                                            Log.d("Console - apogee", tempBuff);
                                            mHandler.obtainMessage(24, String.valueOf(0)).sendToTarget();
                                        }

                                    // Value 25 contains apogee altitude
                                    if (currentSentence.length > 25)
                                        if (currentSentence[25].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(25, String.valueOf(currentSentence[25])).sendToTarget();
                                        else {
                                            Log.d("Console - apogee altitude", tempBuff);
                                            mHandler.obtainMessage(25, String.valueOf(0)).sendToTarget();
                                        }
                                    // Value 26 contains landed
                                    if (currentSentence.length > 26)
                                        if (currentSentence[26].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(26, String.valueOf(currentSentence[26])).sendToTarget();
                                        else {
                                            Log.d("Console - landed", tempBuff);
                                            mHandler.obtainMessage(26, String.valueOf(0)).sendToTarget();
                                        }
                                    // Value 27 contains currentTime
                                    if (currentSentence.length > 27)
                                        if (currentSentence[27].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(27, String.valueOf(currentSentence[27])).sendToTarget();
                                        else {
                                            Log.d("Console - current time", tempBuff);
                                            mHandler.obtainMessage(27, String.valueOf(0)).sendToTarget();
                                        }
                                    // Value 28 contains nbr of flight
                                    if (currentSentence.length > 28)
                                        if (currentSentence[28].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(28, String.valueOf(currentSentence[28])).sendToTarget();
                                        else {
                                            //Log.d("Console - nbr of flight", tempBuff);
                                            mHandler.obtainMessage(28, String.valueOf(0)).sendToTarget();
                                        }
                                }
                            }
                            break;

                        case "data":

                            if (currentSentence[currentSentence.length-1].matches("\\d+(?:\\.\\d+)?"))
                                chk = Long.valueOf(currentSentence[currentSentence.length-1]);
                            Log.d("checksum", "check from sentence: " + chk);
                            Log.d("calculated checksum", "calculated check: " + calculateSentenceCHK(currentSentence));

                            String flightName = "FlightXX";
                            long time = 0;
                            if (calculateSentenceCHK(currentSentence) == chk) {

                                // Value 1 contain the flight number
                                if (currentSentence.length > 1)
                                    if (currentSentence[1].matches("\\d+(?:\\.\\d+)?")) {
                                        currentFlightNbr = Integer.valueOf(currentSentence[1]) + 1;
                                        if (currentFlightNbr < 10)
                                            flightName = getResources().getString(R.string.flight_name) + " "+ "0" + currentFlightNbr;
                                        else
                                            //flight
                                            flightName = getResources().getString(R.string.flight_name) + " " + currentFlightNbr;
                                    }
                                // value
                                // Value 2 contain the time
                                if (currentSentence.length > 2)
                                    if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                        time = Long.valueOf(currentSentence[2]);
                                // Value 3 contain the altitude
                                double altitude = 0;
                                if (currentSentence.length > 3) {
                                    if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                        altitude = Double.valueOf(currentSentence[3]);
                                    // To do
                                    MyFlight.AddToFlight(time,
                                            (long) (altitude * FEET_IN_METER), flightName, 0);

                                }
                                //Value 4 contains the temperature
                                double temperature = 0;
                                if (currentSentence.length > 4) {
                                    if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                        temperature = Long.valueOf(currentSentence[4]);
                                    // To do

                                    MyFlight.AddToFlight(time,
                                            (long) (temperature), flightName, 1);
                                }
                                //Value 5 contains the pressure
                                double pressure = 0;
                                if (currentSentence.length > 5) {
                                    if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                        pressure = Long.valueOf(currentSentence[5]);
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
                                    if (currentSentence[10].matches("\\d+(?:\\.\\d+)?")) {
                                        outputX = Long.valueOf(currentSentence[10]);
                                        MyFlight.AddToFlight(time, (long) (outputX), flightName, 12);
                                    }
                                }
                                //outputY
                                long outputY = 0;
                                if (currentSentence.length > 11) {
                                    if (currentSentence[11].matches("\\d+(?:\\.\\d+)?")) {
                                        outputY = Long.valueOf(currentSentence[11]);
                                        MyFlight.AddToFlight(time, (long) (outputY), flightName, 13);
                                    }
                                }
                                long accelX = 0;
                                if (currentSentence.length > 12) {
                                    if (currentSentence[12].matches("\\d+(?:\\.\\d+)?")) {
                                        accelX = Long.valueOf(currentSentence[12]);
                                        MyFlight.AddToFlight(time, (long) (accelX), flightName, 14);
                                    }
                                }
                                long accelY = 0;
                                if (currentSentence.length > 13) {
                                    if (currentSentence[13].matches("\\d+(?:\\.\\d+)?")) {
                                        accelY = Long.valueOf(currentSentence[13]);
                                        MyFlight.AddToFlight(time, (long) (accelY), flightName, 15);
                                    }
                                }
                                long accelZ = 0;
                                if (currentSentence.length > 14) {
                                    if (currentSentence[14].matches("\\d+(?:\\.\\d+)?")) {
                                        accelZ = Long.valueOf(currentSentence[14]);
                                        MyFlight.AddToFlight(time, (long) (accelZ), flightName, 16);
                                    }
                                }
                            }
                            break;
                        case "alticonfig":

                            if (currentSentence[currentSentence.length-1].matches("\\d+(?:\\.\\d+)?"))
                                chk = Long.valueOf(currentSentence[currentSentence.length-1]);
                            Log.d("checksum", "check from sentence: " + chk);
                            Log.d("calculated checksum", "calculated check: " + calculateSentenceCHK(currentSentence));
                            if (calculateSentenceCHK(currentSentence) == chk) {

                                // Value 0 contains the AltimeterName
                                if (currentSentence.length > 1)
                                    GimbalCfg.setAltimeterName(currentSentence[1]);
                                //Value 1 contains ax_offset
                                if (currentSentence.length > 2)
                                    if (currentSentence[2].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAxOffset(Integer.valueOf(currentSentence[2]));
                                    else
                                        GimbalCfg.setAxOffset(0);
                                // Value 2 contains ay_offset
                                if (currentSentence.length > 3)
                                    if (currentSentence[3].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAyOffset(Integer.valueOf(currentSentence[3]));
                                    else
                                        GimbalCfg.setAyOffset(0);
                                // Value 3 contains az_offset
                                if (currentSentence.length > 4)
                                    if (currentSentence[4].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAzOffset(Integer.valueOf(currentSentence[4]));
                                    else
                                        GimbalCfg.setAzOffset(0);
                                // Value 4 contains gx_offset
                                if (currentSentence.length > 5)
                                    if (currentSentence[5].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setGxOffset(Integer.valueOf(currentSentence[5]));
                                    else
                                        GimbalCfg.setGxOffset(0);
                                // Value 5 contains gy_offset
                                if (currentSentence.length > 6)
                                    if (currentSentence[6].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setGyOffset(Integer.valueOf(currentSentence[6]));
                                    else
                                        GimbalCfg.setGyOffset(0);
                                // Value 6 contains gz_offset
                                if (currentSentence.length > 7)
                                    if (currentSentence[7].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setGzOffset(Integer.valueOf(currentSentence[7]));
                                    else
                                        GimbalCfg.setGzOffset(0);
                                // Value 7 contains KpX
                                if (currentSentence.length > 8)
                                    if (currentSentence[8].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKpX(Double.valueOf(currentSentence[8]) / 100);
                                    else
                                        GimbalCfg.setKpX(0.0);
                                // Value 8 contains KiX
                                if (currentSentence.length > 9)
                                    if (currentSentence[9].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKiX(Double.valueOf(currentSentence[9]) / 100);
                                    else
                                        GimbalCfg.setKiX(0.0);
                                // Value 9 contains KdX
                                if (currentSentence.length > 10)
                                    if (currentSentence[10].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKdX(Double.valueOf(currentSentence[10]) / 100);
                                    else
                                        GimbalCfg.setKdX(0.0);
                                // Value 10 contains KpY
                                if (currentSentence.length > 11)
                                    if (currentSentence[11].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKpY(Double.valueOf(currentSentence[11]) / 100);
                                    else
                                        GimbalCfg.setKpY(0.0);
                                // Value 11 contains KiY
                                if (currentSentence.length > 12)
                                    if (currentSentence[12].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKiY(Double.valueOf(currentSentence[12]) / 100);
                                    else
                                        GimbalCfg.setKiY(0.0);
                                // Value 12 contains KdY
                                if (currentSentence.length > 13)
                                    if (currentSentence[13].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setKdY(Double.valueOf(currentSentence[13]) / 100);
                                    else
                                        GimbalCfg.setKdY(0.0);
                                // Value 13 contains servoXMin
                                if (currentSentence.length > 14)
                                    if (currentSentence[14].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setServoXMin(Integer.valueOf(currentSentence[14]));
                                    else
                                        GimbalCfg.setServoXMin(0);
                                // Value 14 contains servoXMax
                                if (currentSentence.length > 15)
                                    if (currentSentence[15].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setServoXMax(Integer.valueOf(currentSentence[15]));
                                    else
                                        GimbalCfg.setServoXMax(0);
                                // Value 15 contains servoYMin
                                if (currentSentence.length > 16)
                                    if (currentSentence[16].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setServoYMin(Integer.valueOf(currentSentence[16]));
                                    else
                                        GimbalCfg.setServoYMin(0);
                                // Value 16 contains servoYMax
                                if (currentSentence.length > 17)
                                    if (currentSentence[17].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setServoYMax(Integer.valueOf(currentSentence[17]));
                                    else
                                        GimbalCfg.setServoYMax(0);
                                // Value 17 contains the connection speed
                                if (currentSentence.length > 18)
                                    if (currentSentence[18].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setConnectionSpeed(Integer.valueOf(currentSentence[18]));
                                    else
                                        GimbalCfg.setConnectionSpeed(38400);
                                // Value 18 contains the altimeter resolution
                                if (currentSentence.length > 19)
                                    if (currentSentence[19].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAltimeterResolution(Integer.valueOf(currentSentence[19]));
                                    else
                                        GimbalCfg.setAltimeterResolution(0);
                                // Value 19 contains the eeprom size
                                if (currentSentence.length > 20)
                                    if (currentSentence[20].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setEepromSize(Integer.valueOf(currentSentence[20]));
                                    else
                                        GimbalCfg.setEepromSize(512);
                                // Value 20
                                if (currentSentence.length > 21)
                                    if (currentSentence[21].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAltiMajorVersion(Integer.valueOf(currentSentence[21]));
                                // Value 21
                                if (currentSentence.length > 22)
                                    if (currentSentence[22].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAltiMinorVersion(Integer.valueOf(currentSentence[22]));
                                // Value 22 units
                                if (currentSentence.length > 23)
                                    if (currentSentence[23].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setUnits(Integer.valueOf(currentSentence[23]));
                                    else
                                        GimbalCfg.setUnits(0);
                                // Value 23 endRecordAltitude
                                if (currentSentence.length > 24)
                                    if (currentSentence[24].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setEndRecordAltitude(Integer.valueOf(currentSentence[24]));
                                    else
                                        GimbalCfg.setEndRecordAltitude(5);
                                // Value 24 beepingFrequency
                                if (currentSentence.length > 25)
                                    if (currentSentence[25].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setBeepingFrequency(Integer.valueOf(currentSentence[25]));
                                    else
                                        GimbalCfg.setBeepingFrequency(440);
                                // value 25 LaunchDetect
                                if (currentSentence.length > 26)
                                    if (currentSentence[26].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setLiftOffDetect(Integer.valueOf(currentSentence[26]));
                                    else
                                        GimbalCfg.setLiftOffDetect(0);
                                // value 26 Gyro Range
                                if (currentSentence.length > 27)
                                    if (currentSentence[27].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setGyroRange(Integer.valueOf(currentSentence[27]));
                                    else
                                        GimbalCfg.setGyroRange(0);

                                // value 27 Accelero Range
                                if (currentSentence.length > 28)
                                    if (currentSentence[28].matches("^-?[0-9]\\d*(\\.\\d+)?$"))
                                        GimbalCfg.setAcceleroRange(Integer.valueOf(currentSentence[28]));
                                    else
                                        GimbalCfg.setAcceleroRange(0);
                                // value 28 Recording timout
                                if (currentSentence.length > 29)
                                    if (currentSentence[29].matches("\\d+(?:\\.\\d+)?"))
                                        GimbalCfg.setRecordingTimeout(Integer.valueOf(currentSentence[29]));
                                    else
                                        GimbalCfg.setRecordingTimeout(0);
                                // value 29 the battery type
                                if (currentSentence.length > 30)
                                    if (currentSentence[30].matches("\\d+(?:\\.\\d+)?"))
                                        GimbalCfg.setBatteryType(Integer.valueOf(currentSentence[30]));
                                    else
                                        GimbalCfg.setBatteryType(0);

                                // value 30 the telemetry type
                                if (currentSentence.length > 31)
                                    if (currentSentence[31].matches("\\d+(?:\\.\\d+)?"))
                                        GimbalCfg.setTelemetryType(Integer.valueOf(currentSentence[31]));
                                    else
                                        GimbalCfg.setTelemetryType(0);
                                //DataReady = true;
                            }
                            myMessage = myMessage + " " + "alticonfig";
                            break;
                        case "testTrame":
                            if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
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
                                if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                    NbrOfFlight = (Integer.valueOf(currentSentence[1]));
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

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }


    public Configuration getAppLocal() {

        Locale locale = null;
        if (AppConf.getApplicationLanguage()==1) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        } else if (AppConf.getApplicationLanguage()== 2) {
            locale = Locale.ENGLISH;//new Locale("en_US");
        } else {
            locale = Locale.getDefault();
        }


        Configuration config = new Configuration();
        config.locale = locale;
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