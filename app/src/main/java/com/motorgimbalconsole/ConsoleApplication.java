package com.motorgimbalconsole;
/**
 *   @description:
 *   @author: boris.dureau@neuf.fr
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

    private GlobalConfig AppConf=null;
    private String address;
    private String myTypeOfConnection ="bluetooth";// "USB";//"bluetooth";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon =null;

    private Handler mHandler;
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        AppConf = new GlobalConfig();
        AppConf.ReadConfig();
        myTypeOfConnection = AppConf.getConnectionTypeValue();
       // myTypeOfConnection = "bluetooth";
        //myTypeOfConnection ="usb";
    }
    public void setConnectionType (String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }
    public String getConnectionType () {
        return myTypeOfConnection;
    }

    public void setAddress(String bTAddress) {
        address = bTAddress;
    }
    public String getAddress () {return address;}

    public InputStream getInputStream(){
        InputStream tmpIn=null;
        if(myTypeOfConnection.equals("bluetooth")) {
            tmpIn= BTCon.getInputStream();
        }
        else {
            tmpIn= UsbCon.getInputStream();
        }
        return tmpIn;
    }

    public void setConnected(boolean Connected) {
        if(myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        }
        else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public boolean getConnected() {
        boolean ret = false;
        if(myTypeOfConnection.equals("bluetooth")) {
            ret= BTCon.getBTConnected();
        }
        else {
            ret=UsbCon.getUSBConnected();
        }
        return ret;
    }



    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state=false;
        if(myTypeOfConnection.equals( "bluetooth")) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            if(!isConnectionValid()){
                Disconnect();
                state = false;
            }
        }
        clearInput();
        flush();
        write("1\n".toString());
        try {
            while (getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
           /* if(!isConnectionValid()){
                Disconnect();
                state = false;
            }*/

        return state;
    }

    // connect to the USB
    public boolean connect(UsbManager usbManager,UsbDevice device, int baudRate) {
        boolean state=false;
        if(myTypeOfConnection.equals( "usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            if(!isConnectionValid()){
                Disconnect();
                state = false;
            }
        }
        return state;
    }


    public boolean isConnectionValid() {
        boolean valid=false;
        //if(getConnected()) {

            setDataReady(false);

            flush();
            clearInput();

            write("h;\n".toString());

            //flush();

        //appendLog("sent command\n");
            //get the results
            //wait for the result to come back
            try {
                while (getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage =ReadResult();
        //appendLog(myMessage);
          /*  if (myMessage.equals( "OK") )
            {
                valid = true;
            }
            else
            {
                valid = false;
            }*/
        //}
        valid = true;
        return valid;
    }
    public void Disconnect() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.Disconnect();
        }
        else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.write(data);
        }
        else {
            UsbCon.write(data);
        }
    }
    public void clearInput() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.clearInput();
        }
        else {
            UsbCon.clearInput();
        }
    }


    public void appendLog(String text)
    {
        File logFile = new File("sdcard/debugfile.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    //public void setExit(boolean b) {this.exit=b;};

    public String ReadResult() {

        // Reads in data while data is available


        return "myMessage";
    }
    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }

    public Configuration getAppLocal() {

        Locale locale=null;
        if (AppConf.getApplicationLanguage().equals("1")) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        }
        else if(AppConf.getApplicationLanguage().equals("2")) {
            locale = Locale.ENGLISH;//new Locale("en_US");
        }
        else   {
            locale =Locale.getDefault();
        }


        Configuration config = new Configuration();
        config.locale= locale;
        return config;

    }


    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf=value;
    }

    public class GlobalConfig {

        SharedPreferences appConfig =null;
        SharedPreferences.Editor edit = null;
        AppConfigData appCfgData = null;
        //application language
        private String applicationLanguage ="0";
        //Graph units
        private String units="0";

        //flight retrieval timeout
        private long flightRetrievalTimeout;
        //data retrieval timeout
        private long configRetrievalTimeout;

        //graph background color
        private String graphBackColor="1";
        //graph color
        private String graphColor="0";
        //graph font size
        private String fontSize="10";
        // connection type is bluetooth
        private String connectionType = "0";
        // default baud rate for USB is 57600
        private String baudRate = "9";

        public GlobalConfig()
        {
            appConfig  = getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
            appCfgData = new AppConfigData();

        }

        public void ResetDefaultConfig() {

            applicationLanguage ="0";
            graphBackColor="1";
            graphColor="0";
            fontSize="10";
            units="0";
            baudRate="9";
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
                fontSize =appConfig.getString("FontSize","10");
                if (!fontSize.equals(""))
                    setFontSize(fontSize);

                //Baud rate
                String baudRate;
                baudRate = appConfig.getString("BaudRate","");
                if(!baudRate.equals(""))
                    setBaudRate(baudRate);

                //Connection type
                String connectionType;
                connectionType = appConfig.getString("ConnectionType","");
                if(!connectionType.equals(""))
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

        public void setFontSize(String value){
            fontSize=value;
        }

        public String getApplicationLanguage() {
            return applicationLanguage;
        }

        public void setApplicationLanguage(String value) {
            applicationLanguage=value;
        }

        //return the unit id
        public String getUnits() {
            return units;
        }
        public String getUnitsValue ( ) {
            return appCfgData.getUnitsByNbr(Integer.parseInt(units));
        }
        //set the unit by id
        public void setUnits(String value) {
            units=value;
        }
        public String getGraphColor() {
            return graphColor;
        }

        public void setGraphColor(String value) {
            graphColor=value;
        }

        public String getGraphBackColor() {
            return graphBackColor;
        }

        public void setGraphBackColor(String value) {
            graphBackColor=value;
        }

        //get the id of the current connection type
        public String getConnectionType() {
            return connectionType;
        }
        //get the name of the current connection type
        public String getConnectionTypeValue (){
            return appCfgData.getConnectionTypeByNbr(Integer.parseInt(connectionType));
        }
        public void setConnectionType(String value) {
            connectionType=value;
        }

        public String getBaudRate() {
            return baudRate;
        }
        public String getBaudRateValue() {
            return appCfgData.getBaudRateByNbr(Integer.parseInt(baudRate));
        }
        public void setBaudRate(String value) {

            baudRate=value;
        }

        public int ConvertFont(int font) {
            return font+8;
        }
        public int ConvertColor(int col) {

            int myColor=0;

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
                    myColor =  Color.DKGRAY;
                    break;
                case 9:
                    myColor =  Color.LTGRAY;
                    break;
                case 10:
                    myColor =  Color.RED;
                    break;
            }
            return myColor;
        }
    }

}
