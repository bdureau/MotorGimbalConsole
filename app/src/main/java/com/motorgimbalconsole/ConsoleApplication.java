package com.motorgimbalconsole;
/**
 * @description:
 * @author: boris.dureau@neuf.fr
 **/

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

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
    private String address;
    private String myTypeOfConnection = "bluetooth";// "USB";//"bluetooth";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;
    private GimbalConfigData GimbalCfg = null;
    private Handler mHandler;

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private boolean exit = false;
    public long lastReceived = 0;
    public String lastData;
    public int currentFlightNbr = 0;
    public String commandRet = "";
    private double FEET_IN_METER = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        MyFlight = new FlightData();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        AppConf = new GlobalConfig();
        AppConf.ReadConfig();
        GimbalCfg = new GimbalConfigData();
        myTypeOfConnection = AppConf.getConnectionTypeValue();
        // myTypeOfConnection = "bluetooth";
        //myTypeOfConnection ="usb";
    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
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

    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        /*clearInput();
        flush();
        write("1\n".toString());
        try {
            while (getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }*/


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
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }


    public boolean isConnectionValid() {
        boolean valid = false;
        //if(getConnected()) {

        setDataReady(false);

        flush();
        clearInput();

        write("h;\n".toString());

        flush();
        clearInput();
        write("h;\n".toString());
        //get the results
        //wait for the result to come back
        try {
            while (getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        //long timeOut = 10000;
        //long startTime = System.currentTimeMillis();

        myMessage = ReadResult(3000);
        if (myMessage.equals("OK")) {
            //lastReadResult = myMessage;
            valid = true;
        } else {
            //lastReadResult = myMessage;
            valid = false;
        }

        //valid = true;
        return valid;
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
        MyFlight = new FlightData();

        //if(AppConf.getUnits().equals("0"))
        if (AppConf.getUnitsValue().equals("Meters")) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }

    public void appendLog(String text) {
        File logFile = new File("sdcard/debugfile.txt");
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

    ;

    public String ReadResult(long timeout) {

        // Reads in data while data is available

        //setDataReady(false);
        this.exit = false;
        lastData = "";
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {


            while (this.exit == false) {
                if ((System.currentTimeMillis() - lastReceived) > timeout)
                    this.exit = true;
                if (getInputStream().available() > 0) {
                    // Read in the available character
                    char ch = (char) getInputStream().read();
                    lastData = lastData + ch;
                    if (ch == '$') {

                        // read entire sentence until the end
                        String tempBuff = "";
                        while (ch != ';') {
                            // this is not the end of our command
                            ch = (char) getInputStream().read();
                            if (ch != '\r')
                                if (ch != '\n')
                                    if (ch != ';')
                                        tempBuff = tempBuff
                                                + Character.toString(ch);
                        }
                        if (ch == ';') {
                            ch = (char) getInputStream().read();
                        }

                        //Sentence currentSentence = null;
                        String currentSentence[] = new String[25];
                        if (!tempBuff.isEmpty()) {
                            //currentSentence = readSentence(tempBuff);
                            currentSentence = tempBuff.split(",");

                            fullBuff = fullBuff + tempBuff;
                        }

                        switch (currentSentence[0]) {
                            case "telemetry":
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
                                            mHandler.obtainMessage(10, String.valueOf(-0)).sendToTarget();
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
                                            mHandler.obtainMessage(12, String.valueOf(-0.0)).sendToTarget();
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
                                   /* // Value 15 contains graph2
                                    mHandler.obtainMessage(15, String.valueOf(currentSentence[16])).sendToTarget();
                                    // Value 16 contains graph3
                                    mHandler.obtainMessage(16, String.valueOf(currentSentence[17])).sendToTarget();
                                    // Value 17 contains graph4
                                    mHandler.obtainMessage(17, String.valueOf(currentSentence[18])).sendToTarget();
*/
                                }
                                break;
                            //case "alti_status":
                            //  if (mHandler != null) {

                            // }
                            //  break;
                            case "data":
                                String flightName = "FlightXX";
                                long time = 0;
                                // Value 1 contain the flight number
                                if (currentSentence.length > 1)
                                    if (currentSentence[1].matches("\\d+(?:\\.\\d+)?")) {
                                        currentFlightNbr = Integer.valueOf(currentSentence[1]) + 1;
                                        if (currentFlightNbr < 10)
                                            flightName = "Flight " + "0" + currentFlightNbr;
                                        else
                                            flightName = "Flight " + currentFlightNbr;
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
                                        temperature = Double.valueOf(currentSentence[4]);
                                    // To do

                                    MyFlight.AddToFlight(time,
                                            (long) (temperature), flightName, 1);
                                }
                                //Value 5 contains the pressure
                                double pressure = 0;
                                if (currentSentence.length > 5) {
                                    if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                        pressure = Double.valueOf(currentSentence[5]);
                                    // To do

                                    MyFlight.AddToFlight(time,
                                            (long) (pressure), flightName, 2);

                                }
                                // Then get the quaternion
                                //w
                                String w;
                                if (currentSentence.length > 6) {
                                    w = currentSentence[6];
                                }
                                //x
                                String x;
                                if (currentSentence.length > 7) {
                                    x = currentSentence[7];
                                }
                                //y
                                String y;
                                if (currentSentence.length > 8) {
                                    y = currentSentence[8];
                                }
                                //z
                                String z;
                                if (currentSentence.length > 9) {
                                    z = currentSentence[9];
                                }
                                //outputX
                                long outputX=0;
                                if (currentSentence.length > 10) {
                                    if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                        outputX = Long.valueOf(currentSentence[10]);
                                   // MyFlight.AddToFlight(time,(long) (outputX), flightName, 1);
                                }
                                //outputY
                                long outputY=0;
                                if (currentSentence.length > 11) {
                                    if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                        outputY = Long.valueOf(currentSentence[10]);
                                    // MyFlight.AddToFlight(time,(long) (outputY), flightName, 1);
                                }
                                break;
                            case "alticonfig":
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
                                //DataReady = true;
                                myMessage = myMessage + " " + "alticonfig";
                                break;
                            case "nbrOfFlight":
                                // Value 1 contains the number of flight
                                if (currentSentence.length > 1)
                                    NbrOfFlight = (Integer.valueOf(currentSentence[1]));
                                break;
                            case "start":
                                //appendLog("Start");
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
                                //appendLog("end");
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

                    }
                }
            }
        } catch (IOException e) {
            //lastTempBuf = fullBuff;
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

    /* public class Sentence {

         public String keyword;
         public String value0;
         public String value1;
         public String value2;
         public String value3;
         public String value4;
         public String value5;
         public String value6;
         public String value7;
         public String value8;
         public String value9;
         public String value10;
         public String value11;
         public String value12;
         public String value13;
         public String value14;
         public String value15;
         public String value16;
         public String value17;
         public String value18;
         public String value19;
         public String value20;
         public String value21;
         public String value22;
         public String value23;
         public String value24;
         public String value25;

     }*/
    public Configuration getAppLocal() {

        Locale locale = null;
        if (AppConf.getApplicationLanguage().equals("1")) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        } else if (AppConf.getApplicationLanguage().equals("2")) {
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

    public class GlobalConfig {

        SharedPreferences appConfig = null;
        SharedPreferences.Editor edit = null;
        AppConfigData appCfgData = null;
        //application language
        private String applicationLanguage = "0";
        //Graph units
        private String units = "0";

        //flight retrieval timeout
        private long flightRetrievalTimeout;
        //data retrieval timeout
        private long configRetrievalTimeout;

        //graph background color
        private String graphBackColor = "1";
        //graph color
        private String graphColor = "0";
        //graph font size
        private String fontSize = "10";
        // connection type is bluetooth
        private String connectionType = "0";
        // default baud rate for USB is 57600
        private String baudRate = "9";

        public GlobalConfig() {
            appConfig = getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
            appCfgData = new AppConfigData();

        }

        public void ResetDefaultConfig() {

            applicationLanguage = "0";
            graphBackColor = "1";
            graphColor = "0";
            fontSize = "10";
            units = "0";
            baudRate = "9";
            connectionType = "0";
            /*edit.clear();
            edit.putString("AppLanguage","0");
            edit.putString("Units", "0");
            edit.putString("GraphColor", "0");
            edit.putString("GraphBackColor", "1");
            edit.putString("FontSize", "10");*/

        }

        public void ReadConfig() {
            try {
                String appLang;
                appLang = appConfig.getString("AppLanguage", "");
                if (!appLang.equals(""))
                    setApplicationLanguage(appLang);

                //Application Units
                String appUnit;
                appUnit = appConfig.getString("Units", "");
                if (!appUnit.equals(""))
                    setUnits(appUnit);

                //Graph color
                String graphColor;
                graphColor = appConfig.getString("GraphColor", "");
                if (!graphColor.equals(""))
                    setGraphColor(graphColor);

                //Graph Background color
                String graphBackColor;
                graphBackColor = appConfig.getString("GraphBackColor", "");
                if (!graphBackColor.equals(""))
                    setGraphBackColor(graphBackColor);

                //Font size
                String fontSize;
                fontSize = appConfig.getString("FontSize", "10");
                if (!fontSize.equals(""))
                    setFontSize(fontSize);

                //Baud rate
                String baudRate;
                baudRate = appConfig.getString("BaudRate", "");
                if (!baudRate.equals(""))
                    setBaudRate(baudRate);

                //Connection type
                String connectionType;
                connectionType = appConfig.getString("ConnectionType", "");
                if (!connectionType.equals(""))
                    setConnectionType(connectionType);

            } catch (Exception e) {

            }
        }

        public void SaveConfig() {
            edit.putString("AppLanguage", getApplicationLanguage());
            edit.putString("Units", getUnits());
            edit.putString("GraphColor", getGraphColor());
            edit.putString("GraphBackColor", getGraphBackColor());
            edit.putString("FontSize", getFontSize());
            edit.putString("BaudRate", getBaudRate());
            edit.putString("ConnectionType", getConnectionType());
            edit.commit();

        }

        public String getFontSize() {
            return fontSize;
        }

        public void setFontSize(String value) {
            fontSize = value;
        }

        public String getApplicationLanguage() {
            return applicationLanguage;
        }

        public void setApplicationLanguage(String value) {
            applicationLanguage = value;
        }

        //return the unit id
        public String getUnits() {
            return units;
        }

        public String getUnitsValue() {
            return appCfgData.getUnitsByNbr(Integer.parseInt(units));
        }

        //set the unit by id
        public void setUnits(String value) {
            units = value;
        }

        public String getGraphColor() {
            return graphColor;
        }

        public void setGraphColor(String value) {
            graphColor = value;
        }

        public String getGraphBackColor() {
            return graphBackColor;
        }

        public void setGraphBackColor(String value) {
            graphBackColor = value;
        }

        //get the id of the current connection type
        public String getConnectionType() {
            return connectionType;
        }

        //get the name of the current connection type
        public String getConnectionTypeValue() {
            return appCfgData.getConnectionTypeByNbr(Integer.parseInt(connectionType));
        }

        public void setConnectionType(String value) {
            connectionType = value;
        }

        public String getBaudRate() {
            return baudRate;
        }

        public String getBaudRateValue() {
            return appCfgData.getBaudRateByNbr(Integer.parseInt(baudRate));
        }

        public void setBaudRate(String value) {

            baudRate = value;
        }

        public int ConvertFont(int font) {
            return font + 8;
        }

        public int ConvertColor(int col) {

            int myColor = 0;

            switch (col) {

                case 0:
                    myColor = Color.BLACK;
                    break;

                case 1:
                    myColor = Color.WHITE;
                    break;
                case 2:
                    myColor = Color.MAGENTA;
                    break;
                case 3:
                    myColor = Color.BLUE;
                    break;
                case 4:
                    myColor = Color.YELLOW;
                    break;
                case 5:
                    myColor = Color.GREEN;
                    break;
                case 6:
                    myColor = Color.GRAY;
                    break;
                case 7:
                    myColor = Color.CYAN;
                    break;
                case 8:
                    myColor = Color.DKGRAY;
                    break;
                case 9:
                    myColor = Color.LTGRAY;
                    break;
                case 10:
                    myColor = Color.RED;
                    break;
            }
            return myColor;
        }
    }
/*public Sentence readSentence(String tempBuff) {
        // we have a sentence let's find out what it is
        String tempArray[] = tempBuff.split(",");
        Sentence sentence = new Sentence();

        sentence.keyword = tempArray[0];
        if (tempArray.length > 1)
            sentence.value0 = tempArray[1];

        if (tempArray.length > 2)
            if(tempArray[2].matches("\\d+(?:\\.\\d+)?"))
                sentence.value1 = Long.parseLong(tempArray[2]);
            else
                sentence.value1 =-1;
        if (tempArray.length > 3)
            if(tempArray[3].matches("\\d+(?:\\.\\d+)?"))
                sentence.value2 = Long.parseLong(tempArray[3]);
            else
                sentence.value2 =-1;

        if (tempArray.length > 4)
            if(tempArray[4].matches("\\d+(?:\\.\\d+)?"))
                sentence.value3 = Long.parseLong(tempArray[4]);
            else
                sentence.value3 =-1;

        if (tempArray.length > 5)
            if(tempArray[5].matches("\\d+(?:\\.\\d+)?"))
                sentence.value4 = Long.parseLong(tempArray[5]);
            else
                sentence.value4 = -1;

        if (tempArray.length > 6)
            if(tempArray[6].matches("\\d+(?:\\.\\d+)?"))
                sentence.value5 = Long.parseLong(tempArray[6]);
            else
                sentence.value5 = -1;

        if (tempArray.length > 7)
            if(tempArray[7].matches("\\d+(?:\\.\\d+)?"))
                sentence.value6 = Long.parseLong(tempArray[7]);
            else
                sentence.value6 = -1;

        if (tempArray.length > 8)
            if(tempArray[8].matches("\\d+(?:\\.\\d+)?"))
                sentence.value7 = Double.parseDouble(tempArray[8]);
            else
                sentence.value7 = -1;


        if (tempArray.length > 9)
            if(tempArray[9].matches("\\d+(?:\\.\\d+)?"))
                sentence.value8 = Double.parseDouble(tempArray[9]);
            else
                sentence.value8 = -1;

        if (tempArray.length > 10)
            if(tempArray[10].matches("\\d+(?:\\.\\d+)?"))
                sentence.value9 = Double.parseDouble(tempArray[10]);
            else
                sentence.value9 = -1;

        if (tempArray.length > 11)
            if(tempArray[11].matches("\\d+(?:\\.\\d+)?"))
                sentence.value10 = Double.parseDouble(tempArray[11]);
            else
                sentence.value10 = -1;

        if (tempArray.length > 12)
            if(tempArray[12].matches("\\d+(?:\\.\\d+)?"))
                sentence.value11 = Double.parseDouble(tempArray[12]);
            else
                sentence.value11 = -1;

        if (tempArray.length > 13)
            if(tempArray[13].matches("\\d+(?:\\.\\d+)?"))
                sentence.value12 = Double.parseDouble(tempArray[13]);
            else
                sentence.value12 = -1;

        if (tempArray.length > 14)
            if(tempArray[14].matches("\\d+(?:\\.\\d+)?"))
                sentence.value13 = Integer.parseInt(tempArray[14]);
            else
                sentence.value13 = -1;
        if (tempArray.length > 15)
            if(tempArray[15].matches("\\d+(?:\\.\\d+)?"))
                sentence.value14 = Integer.parseInt(tempArray[15]);
            else
                sentence.value14 = -1;
        if (tempArray.length > 16)
            if(tempArray[16].matches("\\d+(?:\\.\\d+)?"))
                sentence.value15 = Integer.parseInt(tempArray[16]);
            else
                sentence.value15 = -1;
        if (tempArray.length > 17)
            if(tempArray[17].matches("\\d+(?:\\.\\d+)?"))
                sentence.value16 = Integer.parseInt(tempArray[17]);
            else
                sentence.value16 = -1;
        if (tempArray.length > 18)
            if(tempArray[18].matches("\\d+(?:\\.\\d+)?"))
                sentence.value17 = Integer.parseInt(tempArray[18]);
            else
                sentence.value17 = -1;
        if (tempArray.length > 19)
            if(tempArray[19].matches("\\d+(?:\\.\\d+)?"))
                sentence.value18 = Integer.parseInt(tempArray[19]);
            else
                sentence.value18 = -1;
        if (tempArray.length > 20)
            if(tempArray[20].matches("\\d+(?:\\.\\d+)?"))
                sentence.value19 = Integer.parseInt(tempArray[20]);
            else
                sentence.value19 = -1;
        if (tempArray.length > 21)
            if(tempArray[21].matches("\\d+(?:\\.\\d+)?"))
                sentence.value20 = Integer.parseInt(tempArray[21]);
            else
                sentence.value20 = -1;

        if (tempArray.length > 22)
            if(tempArray[22].matches("\\d+(?:\\.\\d+)?"))
                sentence.value21 = Integer.parseInt(tempArray[22]);
            else
                sentence.value21 = -1;

        if (tempArray.length > 23)
            if(tempArray[23].matches("\\d+(?:\\.\\d+)?"))
                sentence.value22 = Integer.parseInt(tempArray[23]);
            else
                sentence.value22 = -1;

        if (tempArray.length > 24)
            if(tempArray[24].matches("\\d+(?:\\.\\d+)?"))
                sentence.value23 = Integer.parseInt(tempArray[24]);
            else
                sentence.value23 = -1;

        if (tempArray.length > 25)
            if(tempArray[25].matches("\\d+(?:\\.\\d+)?"))
                sentence.value24 = Integer.parseInt(tempArray[25]);
            else
                sentence.value24 = -1;
        return sentence;
    }*/


   /*public class Sentence {

        public String keyword;
        public String value0;
        public long value1;
        public long value2;
        public long value3;
        public long value4;
        public long value5;
        public long value6;
        public double value7;
        public double value8;
        public double value9;
        public double value10;
        public double value11;
        public double value12;
        public int value13;
        public int value14;
        public int value15;
        public int value16;
        public long value17;
        public int value18;
        public int value19;
        public int value20;
        public int value21;
        public int value22;
        public int value23;
        public int value24;
        public int value25;

    }*/

}
