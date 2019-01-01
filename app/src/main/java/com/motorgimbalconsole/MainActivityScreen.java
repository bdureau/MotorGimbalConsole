package com.motorgimbalconsole;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
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

import java.util.HashMap;
import java.util.Map;

import processing.android.PFragment;
import processing.core.PApplet;

public class MainActivityScreen extends AppCompatActivity {
    private PApplet sketch;
    Button btnConnectDisconnect, btnOrientation, btnCalibrate;
    //PFragment fragment;
    //private BluetoothConnection BTCon = null;
    private String address;
    ConsoleApplication myBT;
    private ProgressDialog progress;
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
                        btnConnectDisconnect.setText("disconnect");
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
                        btnConnectDisconnect.setText("connect");
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

        btnCalibrate = (Button)findViewById(R.id.butCalibrate);
        btnOrientation = (Button)findViewById(R.id.butOrientation);
        DisableUI();
        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        sketch = new Sketch();
        ((Sketch) sketch).setMyApp(myBT);
        //((Sketch) sketch).setApp(myBT);
        PFragment fragment = new PFragment(sketch);
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();



        //BTCon = new BluetoothConnection();
        //fragment =(PFragment)findViewById(R.id.fragment);
        /*FrameLayout frame = new FrameLayout(this);
        frame.setId(CompatUtils.getUniqueViewId());
        setContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        sketch = new Sketch();
        PFragment fragment = new PFragment(sketch);
        fragment.setView(frame, this);*/

        btnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Sketch) sketch).setOrientation('h');
            }
        });
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.write("c;\n".toString());
            }
        });

         /*btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (myBT.getConnected()) {
                    // msg("disconnecting");
                    Disconnect(); //close connection
                    DisableUI();
                    btnConnectDisconnect.setText("connect");
                }
                else {

                        address = myBT.getAddress();

                        if (address != null ) {
                            new ConnectBT().execute(); //Call the class to connect
                            if (myBT.getConnected()) {
                                EnableUI();
                                btnConnectDisconnect.setText("disconnect");
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }


                }
            }
        });*/
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType().equals( "0"))
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {
                    // msg("disconnecting");
                    Disconnect(); //close connection
                    DisableUI();
                    btnConnectDisconnect.setText("connect");
                }
                else {
                    // msg(myBT.getConnectionType());
                    if (myBT.getConnectionType().equals( "bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null ) {
                            new ConnectBT().execute(); //Call the class to connect
                            if (myBT.getConnected()) {
                                EnableUI();
                                btnConnectDisconnect.setText("disconnect");
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
        btnCalibrate.setEnabled(false);
        btnOrientation.setEnabled(false);

    }
    private void EnableUI () {
        btnCalibrate.setEnabled(true);
        btnOrientation.setEnabled(true);

    }
    @Override
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
    }
    private void Disconnect() {
        myBT.Disconnect();
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
           /* Intent i = new Intent(MainActivityScreen.this, AboutActivity.class);
            startActivity(i);*/
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
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
            progress = ProgressDialog.show(MainActivityScreen.this,
                    "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            //if (myBT.getBtSocket() == null || !myBT.getConnected()) {
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
                //finish();
            } else {
                //Connected.
                msg("Connected");
                //isBtConnected = true;
                myBT.setConnected(true);
                EnableUI();
                btnConnectDisconnect.setText("disconnect");
            }
            progress.dismiss();
        }
    }
}
