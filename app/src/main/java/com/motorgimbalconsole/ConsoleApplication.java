package com.motorgimbalconsole;
/**
 *   @description:
 *   @author: boris.dureau@neuf.fr
 **/

import android.app.Application;
import android.os.Handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;


/**
 *   @description: This is quite a major class used everywhere because it can point to your connection, appconfig
 *   @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private boolean isConnected = false;
    private static boolean DataReady = false;

    private String address;
    private BluetoothConnection BTCon = null;

    private Handler mHandler;
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BTCon = new BluetoothConnection();
    }


    public void setAddress(String bTAddress) {
        address = bTAddress;
    }
    public String getAddress () {return address;}

    public InputStream getInputStream(){
        InputStream tmpIn=null;
        tmpIn= BTCon.getInputStream();
        return tmpIn;
    }

    public void setConnected(boolean Connected) {
        BTCon.setBTConnected(Connected);
    }

    public boolean getConnected() {
        boolean ret = false;
        ret= BTCon.getBTConnected();
        return ret;
    }



    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state=false;

        state = BTCon.connect(address);
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
            if (myMessage.equals( "OK") )
            {
                valid = true;
            }
            else
            {
                valid = false;
            }
        //}
        return valid;
    }
    public void Disconnect() {
            BTCon.Disconnect();
    }

    public void flush() {
            BTCon.flush();
    }

    public void write(String data) {
            BTCon.write(data);
    }
    public void clearInput() {
            BTCon.clearInput();
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



}
