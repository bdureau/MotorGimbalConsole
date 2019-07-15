package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



public class MainActivityScreen extends AppCompatActivity {

    Button btnConnectDisconnect, btnConfig, btnStatus;
    Button btnReset, btnFlight;

    private String address;
    ConsoleApplication myBT;
    //private ProgressDialog progress;
    UsbManager usbManager;
    UsbDevice device;
    public final String ACTION_USB_PERMISSION = "com.altimeter.bdureau.bearconsole.USB_PERMISSION";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    if(myBT.connect(usbManager, device, 38600)){
                        myBT.setConnected(true);
                        EnableUI();
                        myBT.setConnectionType("usb");
                        btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                    }
                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg("I can connect via usb");
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if(myBT.getConnectionType().equals("usb"))
                    if(myBT.getConnected()) {
                        myBT.Disconnect();
                        btnConnectDisconnect.setText(getResources().getString(R.string.connect_disconnect));
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        setContentView(R.layout.activity_main_screen);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        btnConnectDisconnect = (Button)findViewById(R.id.button);
        btnConnectDisconnect.setText("connect");
        btnReset= (Button)findViewById(R.id.butGimbalReset);
        btnFlight= (Button)findViewById(R.id.butGimbalFlight);

        btnConfig = (Button)findViewById(R.id.butGimbalConfig);
        btnStatus = (Button)findViewById(R.id.butGimbalStatus);
        DisableUI();
        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();



        btnConfig.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 /*if(myBT.getConnected()) {
                     myBT.flush();
                     myBT.clearInput();

                     myBT.write("y0;\n".toString());
                 }*/
                 readConfig();
                 Intent i = new Intent(MainActivityScreen.this, ConsoleTabConfigActivity.class);
                 startActivity(i);
             }
         });
        btnFlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivityScreen.this, FlightListActivity.class);
                startActivity(i);
            }
        });
        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();

                    myBT.write("y1;\n".toString());
                }
                Intent i = new Intent(MainActivityScreen.this, ConsoleTabStatusActivity.class);
                startActivity(i);
            }
        });
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivityScreen.this, ResetSettingsActivity.class);
                startActivity(i);
            }
        });
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType().equals( "0"))
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {

                    Disconnect(); //close connection
                    DisableUI();
                    btnConnectDisconnect.setText(getResources().getString(R.string.connect_disconnect));
                }
                else {
                    if (myBT.getConnectionType().equals( "bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null ) {
                            new ConnectBT().execute(); //Call the class to connect

                            if (myBT.getConnected()) {
                                EnableUI();
                                btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }
                    }
                    else {
                        //this is a USB connection
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {
                            boolean keep = true;
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                int deviceVID = device.getVendorId();

                                PendingIntent pi = PendingIntent.getBroadcast(MainActivityScreen.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                usbManager.requestPermission(device, pi);
                                keep = false;


                                if (!keep)
                                    break;
                            }
                        }
                    }
                }
            }
        });

    }

    private void DisableUI () {
        btnConfig.setEnabled(false);
        btnStatus.setEnabled(false);
        btnFlight.setEnabled(false);
        btnReset.setEnabled(false);

    }
    private void EnableUI () {
        btnConfig.setEnabled(true);
        btnStatus.setEnabled(true);
        btnFlight.setEnabled(true);
        btnReset.setEnabled(true);
    }
    /*@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (sketch != null) {
            sketch.onRequestPermissionsResult(
                    requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (sketch != null) {
            sketch.onNewIntent(intent);
        }
    }*/
    private void Disconnect() {
        myBT.Disconnect();
    }
    private void readConfig()
    {
        // ask for config
        if(myBT.getConnected()) {

            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();

            myBT.write("b;\n".toString());

            myBT.flush();


            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if(myBT.getConnected()) {
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage =myBT.ReadResult(10000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myMessage =myBT.ReadResult(10000);
            }
            if (myMessage.equals( "start alticonfig end") )
            {
                try {
                    //GimbalCfg= myBT.getAltiConfigData();
                }
                catch (Exception e) {
                    //  msg("pb ready data");
                }
            }
            else
            {
                // msg("data not ready");
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivityScreen.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
           /* Intent i = new Intent(MainActivityScreen.this, HelpActivity.class);
            startActivity(i);*/
            return true;
        }
        if (id == R.id.action_bluetooth) {
            // choose the bluetooth device
            Intent i = new Intent(MainActivityScreen.this,SearchBluetooth.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(MainActivityScreen.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // connect to the bluetooth adapter
   /* public boolean connect() {
        boolean state=false;

        state = BTCon.connect(address);



        return state;
    }*/
    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    /* This is the Bluetooth connection sub class */
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private  AlertDialog.Builder builder=null;
        private AlertDialog alert;
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
           /* progress = ProgressDialog.show(MainActivityScreen.this,
                    "Connecting...", "Please wait!!!"); */ //show a progress dialog
            builder = new AlertDialog.Builder(MainActivityScreen.this);
            //Connecting...
            builder.setMessage(getResources().getString(R.string.MS_msg1))
                    .setTitle(getResources().getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            myBT.setExit(true);
                            myBT.Disconnect();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            if ( !myBT.getConnected()) {

                if (myBT.connect())
                    ConnectSuccess = true;
                else
                    ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                //Connection Failed. Is it a SPP Bluetooth? Try again.
                msg("Connection Failed");

            } else {
                //Connected.
                msg("Connected");
                myBT.setConnected(true);
                EnableUI();
                btnConnectDisconnect.setText("disconnect");
            }
            //progress.dismiss();
            alert.dismiss();
        }
    }
}
