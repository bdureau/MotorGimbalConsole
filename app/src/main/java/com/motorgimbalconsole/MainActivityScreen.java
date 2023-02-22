package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Bundle;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.motorgimbalconsole.config.AppTabConfigActivity;
import com.motorgimbalconsole.config.ModulesConfig.Config3DR;
import com.motorgimbalconsole.config.ModulesConfig.ConfigBT;
import com.motorgimbalconsole.config.ModulesConfig.ConfigLora;
import com.motorgimbalconsole.config.GimbalTabConfigActivity;
import com.motorgimbalconsole.config.GimbalConfigData;
import com.motorgimbalconsole.connection.SearchBluetooth;
import com.motorgimbalconsole.connection.TestConnection;
import com.motorgimbalconsole.flash.FlashFirmware;
import com.motorgimbalconsole.flights.FlightListActivity;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;
import com.motorgimbalconsole.telemetry.GimbalTabStatusActivity;
import com.motorgimbalconsole.telemetry.GimbalTelemetryTabActivity;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class MainActivityScreen extends AppCompatActivity {

    CardView btnConnectDisconnect, btnConfig, btnStatus, btnAbout;
    CardView btnReset, btnFlight, btnFlashFirmware, btnTelemetry;

    ImageView image_settings, image_curve, image_telemetry, image_reset, image_status,
    image_firmware, image_info, image_connect;
    TextView text_settings, text_curve, text_telemetry, text_reset, text_status,
            text_firmware, text_info, text_connect;

    private String address;
    ConsoleApplication myBT;

    UsbManager usbManager;
    UsbDevice device;
    private GimbalConfigData GimbalCfg = null;
    private FirmwareCompatibility firmCompat = null;
    public final String ACTION_USB_PERMISSION = "com.motorgimbalconsole.USB_PERMISSION";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = true;
                if(android.os.Build.VERSION.SDK_INT < 31)
                    granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);

                if (granted) {
                    if (myBT.connect(usbManager, device, 38400)) {
                        myBT.setConnected(true);
                        EnableUI();
                        myBT.setConnectionType("usb");
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
                        DisableUI();
                        setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                        text_connect.setText(getResources().getString(R.string.connect_disconnect));
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        //This will check if the firmware is compatible with the app and advice on flashing the firmware
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        //card
        btnConnectDisconnect = (CardView) findViewById(R.id.connect_card);
        btnReset = (CardView) findViewById(R.id.reset_card);
        btnFlight = (CardView) findViewById(R.id.flights_card);
        btnTelemetry = (CardView) findViewById(R.id.telemetry_card);
        btnConfig = (CardView) findViewById(R.id.settings_card);
        btnStatus = (CardView) findViewById(R.id.status_card);
        btnFlashFirmware = (CardView) findViewById(R.id.flash_card);
        btnAbout = (CardView) findViewById(R.id.info_card);

        //images
        image_settings = (ImageView) findViewById(R.id.image_settings);
        image_curve = (ImageView) findViewById(R.id.image_curve);
        image_telemetry = (ImageView) findViewById(R.id.image_telemetry);
        image_reset = (ImageView) findViewById(R.id.image_reset);
        image_status = (ImageView) findViewById(R.id.image_status);
        image_firmware = (ImageView) findViewById(R.id.image_firmware);
        image_info = (ImageView) findViewById(R.id.image_info);
        image_connect = (ImageView) findViewById(R.id.image_connect);

        //text
        text_settings = (TextView) findViewById(R.id.text_settings);
        text_curve = (TextView) findViewById(R.id.text_curve);
        text_telemetry  = (TextView) findViewById(R.id.text_telemetry);
        text_reset = (TextView) findViewById(R.id.text_reset);
        text_status = (TextView) findViewById(R.id.text_status);
        text_firmware = (TextView) findViewById(R.id.text_firmware);
        text_info = (TextView) findViewById(R.id.text_info);
        text_connect = (TextView) findViewById(R.id.text_connect);

        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        if (myBT.getConnected()) {
            EnableUI();
        } else {
            DisableUI();
            text_connect.setText(getResources().getString(R.string.connect_disconnect));
            setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
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
                i = new Intent(MainActivityScreen.this, GimbalTelemetryTabActivity.class);
                startActivity(i);
            }
        });

        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readConfig();
                Intent i = new Intent(MainActivityScreen.this, GimbalTabConfigActivity.class);
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
                Intent i = new Intent(MainActivityScreen.this, GimbalTabStatusActivity.class);
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
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivityScreen.this, AboutActivity.class);
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
                    text_connect.setText(getResources().getString(R.string.connect_disconnect));
                    setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                } else {
                    if (myBT.getConnectionType().equals("bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null) {
                            new ConnectBT().execute(); //Call the class to connect

                            if (myBT.getConnected()) {
                                EnableUI();
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }
                    } else {
                        myBT.setModuleName("USB");
                        //this is a USB connection
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {
                            boolean keep = true;
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                int deviceVID = device.getVendorId();

                                //PendingIntent pi = PendingIntent.getBroadcast(MainActivityScreen.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                PendingIntent pi;
                                if(android.os.Build.VERSION.SDK_INT >= 31) {
                                    pi = PendingIntent.getBroadcast(MainActivityScreen.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                                } else {
                                    pi = PendingIntent.getBroadcast(MainActivityScreen.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                }
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
        setEnabledCard(false, btnConfig, image_settings, text_settings);
        setEnabledCard(false, btnStatus, image_status, text_status);
        setEnabledCard(false, btnFlight, image_curve, text_curve);
        setEnabledCard(false, btnReset, image_reset, text_reset);
        setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
        setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();

    }
    private void setEnabledCard(boolean enable, CardView card, ImageView image, TextView text) {
        card.setEnabled(enable);
        image.setImageAlpha(enable? 0xFF : 0x3F);
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
                setEnabledCard(true, btnConfig, image_settings, text_settings);
                setEnabledCard(true, btnStatus, image_status, text_status);
                setEnabledCard(true, btnFlight, image_curve, text_curve);
                setEnabledCard(true, btnReset, image_reset, text_reset);
            } else {
                setEnabledCard(false, btnConfig, image_settings, text_settings);
                setEnabledCard(false, btnStatus, image_status, text_status);
                setEnabledCard(false, btnFlight, image_curve, text_curve);
                setEnabledCard(false, btnReset, image_reset, text_reset);
            }

            setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);
            text_connect.setText(getResources().getString(R.string.disconnect));
            setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);
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
            try { Thread.sleep(1000); } catch (InterruptedException e) {}//move it elsewhere
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
            Intent i = new Intent(MainActivityScreen.this, AppTabConfigActivity.class);
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
        //Open the lora module config
        if (id == R.id.action_modlora_settings) {
            Intent i = new Intent(MainActivityScreen.this, ConfigLora.class);
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
            builder.setMessage(getResources().getString(R.string.MS_msg1)+ "\n"+ myBT.getModuleName())
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
            Add("RocketMotorGimbal", "1.2");
            Add("RocketMotorGimbal_bno055", "1.2");
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
