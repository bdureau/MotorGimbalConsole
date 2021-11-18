package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.motorgimbalconsole.config.AppConfigActivity;
import com.motorgimbalconsole.config.Config3DR;
import com.motorgimbalconsole.config.ConfigBT;
import com.motorgimbalconsole.config.ConsoleTabConfigActivity;
import com.motorgimbalconsole.config.GimbalConfigData;
import com.motorgimbalconsole.connection.SearchBluetooth;
import com.motorgimbalconsole.connection.TestConnection;
import com.motorgimbalconsole.flash.FlashFirmware;
import com.motorgimbalconsole.flights.FlightListActivity;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;
import com.motorgimbalconsole.telemetry.ConsoleTabStatusActivity;
import com.motorgimbalconsole.telemetry.TelemetryMp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class MainActivityScreen extends AppCompatActivity {

    Button btnConnectDisconnect, btnConfig, btnStatus;
    Button btnReset, btnFlight, btnFlashFirmware, btnTelemetry;

    private String address;
    ConsoleApplication myBT;
    //private ProgressDialog progress;
    UsbManager usbManager;
    UsbDevice device;
    private GimbalConfigData GimbalCfg = null;
    private FirmwareCompatibility firmCompat = null;
    public final String ACTION_USB_PERMISSION = "com.motorgimbalconsole.USB_PERMISSION";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    if (myBT.connect(usbManager, device, 38600)) {
                        myBT.setConnected(true);
                        EnableUI();
                        myBT.setConnectionType("usb");
                        //btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                    }
                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg(getResources().getString(R.string.usb_device_attached));
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (myBT.getConnectionType().equals("usb"))
                    if (myBT.getConnected()) {
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
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        btnConnectDisconnect = (Button) findViewById(R.id.button);

        btnReset = (Button) findViewById(R.id.butGimbalReset);
        btnFlight = (Button) findViewById(R.id.butGimbalFlight);
        btnTelemetry = (Button) findViewById(R.id.butGimbalTelemetry);
        btnConfig = (Button) findViewById(R.id.butGimbalConfig);
        btnStatus = (Button) findViewById(R.id.butGimbalStatus);
        btnFlashFirmware = (Button) findViewById(R.id.butFlash);
        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        if (myBT.getConnected()) {
            EnableUI();
            //btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
        } else {
            DisableUI();
            btnConnectDisconnect.setText(getResources().getString(R.string.connect_disconnect));
        }

        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    //send telemetry command
                    myBT.write("y1;".toString());
                }
                Intent i;
                i = new Intent(MainActivityScreen.this, TelemetryMp.class);
                startActivity(i);
            }
        });

        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();

                    myBT.write("y1;".toString());
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
        //commands to be sent to flash the firmware
        btnFlashFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myBT.getAppConf().ReadConfig();

                Intent i = new Intent(MainActivityScreen.this, FlashFirmware.class);
                //Change the activity.
                startActivity(i);
            }
        });
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType().equals("0"))
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {

                    Disconnect(); //close connection
                    DisableUI();
                    btnConnectDisconnect.setText(getResources().getString(R.string.connect_disconnect));
                    btnFlashFirmware.setEnabled(true);
                } else {
                    if (myBT.getConnectionType().equals("bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null) {
                            new ConnectBT().execute(); //Call the class to connect

                            if (myBT.getConnected()) {
                                EnableUI();
                                //btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                                //btnFlashFirmware.setEnabled(false);
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }
                    } else {
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

    private void DisableUI() {
        btnConfig.setEnabled(false);
        btnStatus.setEnabled(false);
        btnFlight.setEnabled(false);
        btnReset.setEnabled(false);
        btnTelemetry.setEnabled(false);
        btnFlashFirmware.setEnabled(true);
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();

    }

    private void EnableUI() {
        boolean success;
        success = readConfig();
        //second attempt
        if (!success)
            success = readConfig();
        //third attempt
        if (!success)
            success = readConfig();
        //fourth and last
        if (!success)
            success = readConfig();
        if (myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal") ||
                myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal_bno055")) {
            if (myBT.getAppConf().getConnectionType().equals("0") || (myBT.getAppConf().getConnectionType().equals("1") && myBT.getAppConf().getFullUSBSupport().equals("true"))) {
                btnConfig.setEnabled(true);
                btnStatus.setEnabled(true);
                btnFlight.setEnabled(true);
                btnReset.setEnabled(true);
            } else {
                btnConfig.setEnabled(false);
                btnStatus.setEnabled(false);
                btnFlight.setEnabled(false);
                btnReset.setEnabled(false);
            }

            btnTelemetry.setEnabled(true);
            btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
            btnFlashFirmware.setEnabled(false);
            if (!firmCompat.IsCompatible(myBT.getGimbalConfigData().getAltimeterName(),
                    myBT.getGimbalConfigData().getAltiMajorVersion() + "." + myBT.getGimbalConfigData().getAltiMinorVersion())) {
                msg(getString(R.string.flash_advice_msg));
            } else {
                msg(getResources().getString(R.string.MS_msg4));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg));
            myBT.Disconnect();
        }
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }

    private boolean readConfig() {
        // ask for config
        boolean success = false;
        if (myBT.getConnected()) {

            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();

            myBT.write("b;".toString());

            myBT.flush();


            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if (myBT.getConnected()) {
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage = myBT.ReadResult(10000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myMessage = myBT.ReadResult(10000);
            }
            if (myMessage.equals("start alticonfig end")) {
                try {
                    GimbalCfg = myBT.getGimbalConfigData();
                    success = true;
                } catch (Exception e) {
                    //  msg("pb ready data");
                    success = false;
                }
            } else {
                // msg("data not ready");
                success = false;
            }
        }
        return success;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        myBT.getAppConf().ReadConfig();
        //Log.d("MainScreen", "myBT.getConnectionType():" +myBT.getConnectionType());
        if (myBT.getAppConf().getConnectionType().equals("1")) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        } else {
            menu.findItem(R.id.action_bluetooth).setEnabled(true);
        }

        //if we are connected then enable some menu options and if not disable them
        if (myBT.getConnected()) {
            // We are connected so no need to choose the bluetooth
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
            // We are connected so we do not want to configure the 3DR module
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(false);
            // same goes for the BT module
            menu.findItem(R.id.action_modbt_settings).setEnabled(false);
            // Allow connection testing
            menu.findItem(R.id.action_test_connection).setEnabled(true);
        } else {
            // not connected so allow those
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(true);
            menu.findItem(R.id.action_modbt_settings).setEnabled(true);
            //cannot do connection testing until we are connected
            menu.findItem(R.id.action_test_connection).setEnabled(false);
        }
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
            Intent i = new Intent(MainActivityScreen.this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            // choose the bluetooth device
            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(MainActivityScreen.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_mod3dr_settings) {
            Intent i = new Intent(MainActivityScreen.this, Config3DR.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_modbt_settings) {
            Intent i = new Intent(MainActivityScreen.this, ConfigBT.class);
            startActivity(i);
            return true;
        }
        //Test current connection
        if (id == R.id.action_test_connection) {
            Intent i = new Intent(MainActivityScreen.this, TestConnection.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /* This is the Bluetooth connection sub class */
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
            //show a progress dialog
            builder = new AlertDialog.Builder(MainActivityScreen.this);

            //Connecting...
            builder.setMessage(getResources().getString(R.string.MS_msg1))
                    .setTitle(getResources().getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.MS_cancel), new DialogInterface.OnClickListener() {
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

            if (!myBT.getConnected()) {

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
                msg(getResources().getString(R.string.MS_msg5));

            } else {
                //Connected.
                myBT.setConnected(true);
                EnableUI();
            }
            alert.dismiss();
        }
    }

    public class FirmwareCompatibility {
        // Create a hash map
        public HashMap<String, String> hm;

        FirmwareCompatibility() {
            hm = null;
            hm = new HashMap();
            //init compatible versions
            Add("RocketMotorGimbal", "1.1");
            Add("RocketMotorGimbal_bno055", "1.1");
        }

        public void Add(String altiName, String verList) {
            hm.put(altiName, verList);
        }

        public boolean IsCompatible(String altiName, String ver) {
            boolean compatible = false;
            String compatFirmwareList = "";
            Set set = hm.entrySet();

            // Get an iterator
            Iterator i = set.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();

                if (me.getKey().equals(altiName)) {
                    compatFirmwareList = me.getValue().toString();
                    break;
                }
            }
            String firmwareVersion[] = compatFirmwareList.split(",");
            for (int j = 0; j < firmwareVersion.length; j++) {
                if (firmwareVersion[j].equals(ver))
                    compatible = true;
            }
            return compatible;
        }
    }
}
