/*
 * Model Rocket Gimbal Console - Flight Computer & Ground Control System
 *
 * Class: MainActivityScreen
 * Author: Model Rocketry Android Developer
 * Description: Main dashboard providing primary navigation to telemetry displays,
 *              gimbal configuration, firmware updates, and serial/Bluetooth communication
 *              interfaces for the rocket motor gimbal stabilization system.
 */

package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.felhr.usbserial.UsbSerialDevice;
import com.motorgimbalconsole.config.AppTabConfigActivity;
import com.motorgimbalconsole.config.GimbalConfigData;
import com.motorgimbalconsole.config.GimbalTabConfigActivity;
import com.motorgimbalconsole.config.ModulesConfig.Config3DR;
import com.motorgimbalconsole.config.ModulesConfig.ConfigBT;
import com.motorgimbalconsole.config.ModulesConfig.ConfigLoraE220;
import com.motorgimbalconsole.config.ModulesConfig.ConfigLoraE32;
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
import java.util.Map;

public class MainActivityScreen extends AppCompatActivity {

    // UI card actions for primary navigation
    private CardView btnConnectDisconnect, btnConfig, btnStatus, btnAbout;
    private CardView btnReset, btnFlight, btnFlashFirmware, btnTelemetry;

    // Visual indicators and iconography
    private ImageView image_settings, image_curve, image_telemetry, image_reset, image_status,
            image_firmware, image_info, image_connect;
    private TextView text_settings, text_curve, text_telemetry, text_reset, text_status,
            text_firmware, text_info, text_connect;

    private String address;
    private ConsoleApplication myBT;

    private UsbManager usbManager;
    private UsbDevice device;
    private GimbalConfigData GimbalCfg = null;
    private FirmwareCompatibility firmCompat = null;
    public final String ACTION_USB_PERMISSION = "com.motorgimbalconsole.USB_PERMISSION";
    private boolean isReceiverRegistered = false;

    /**
     * BroadcastReceiver managing hot-plug USB OTG events and USB permissions.
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ACTION_USB_PERMISSION)) {
                boolean granted = true;
                if (Build.VERSION.SDK_INT < 31 && intent.getExtras() != null) {
                    granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                }

                if (granted) {
                    // Initialize serial stream at 38400 baud standard for rocket gimbal telemetry
                    if (myBT.connect(usbManager, device, 38400)) {
                        myBT.setConnected(true);
                        EnableUI();
                        myBT.setConnectionType("usb");
                    }
                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg(getResources().getString(R.string.usb_device_attached));
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                // Safeguard against detached USB cable during active communications
                if ("usb".equals(myBT.getConnectionType()) && myBT.getConnected()) {
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
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        // Register receiver with explicit export flags compatible across all Android versions
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        ContextCompat.registerReceiver(this, broadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        isReceiverRegistered = true;

        // Initialize Card views
        btnConnectDisconnect = findViewById(R.id.connect_card);
        btnReset = findViewById(R.id.reset_card);
        btnFlight = findViewById(R.id.flights_card);
        btnTelemetry = findViewById(R.id.telemetry_card);
        btnConfig = findViewById(R.id.settings_card);
        btnStatus = findViewById(R.id.status_card);
        btnFlashFirmware = findViewById(R.id.flash_card);
        btnAbout = findViewById(R.id.info_card);

        // Initialize Icons
        image_settings = findViewById(R.id.image_settings);
        image_curve = findViewById(R.id.image_curve);
        image_telemetry = findViewById(R.id.image_telemetry);
        image_reset = findViewById(R.id.image_reset);
        image_status = findViewById(R.id.image_status);
        image_firmware = findViewById(R.id.image_firmware);
        image_info = findViewById(R.id.image_info);
        image_connect = findViewById(R.id.image_connect);

        // Initialize Text labels
        text_settings = findViewById(R.id.text_settings);
        text_curve = findViewById(R.id.text_curve);
        text_telemetry = findViewById(R.id.text_telemetry);
        text_reset = findViewById(R.id.text_reset);
        text_status = findViewById(R.id.text_status);
        text_firmware = findViewById(R.id.text_firmware);
        text_info = findViewById(R.id.text_info);
        text_connect = findViewById(R.id.text_connect);

        myBT = (ConsoleApplication) getApplication();

        if (myBT.getConnected()) {
            EnableUI();
        } else {
            DisableUI();
            text_connect.setText(getResources().getString(R.string.connect_disconnect));
            setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
        }

        // Telemetry real-time monitor
        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    // Command 'y1;' triggers high-rate IMU & gimbal telemetry streaming
                    myBT.write("y1;");
                }
                Intent i = new Intent(MainActivityScreen.this, GimbalTelemetryTabActivity.class);
                startActivity(i);
            }
        });

        // PID and stabilization config
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readConfig();
                Intent i = new Intent(MainActivityScreen.this, GimbalTabConfigActivity.class);
                startActivity(i);
            }
        });

        // Historical flight curves and log downloads
        btnFlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivityScreen.this, FlightListActivity.class);
                startActivity(i);
            }
        });

        // Altimeter and gimbal diagnostic status
        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
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

        // Bootloader flash utility
        btnFlashFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                Intent i = new Intent(MainActivityScreen.this, FlashFirmware.class);
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

        // Physical link connection orchestrator
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT) {
                    myBT.setConnectionType("bluetooth");
                } else {
                    myBT.setConnectionType("usb");
                }

                if (myBT.getConnected()) {
                    Disconnect();
                    DisableUI();
                    text_connect.setText(getResources().getString(R.string.connect_disconnect));
                    setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
                } else {
                    if ("bluetooth".equals(myBT.getConnectionType())) {
                        address = myBT.getAddress();
                        if (address != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityScreen.this);
                            builder.setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + "?")
                                    .setTitle("")
                                    .setCancelable(false)
                                    .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            // Execute asynchronous connection worker
                                            new ConnectBT().execute();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                                            startActivity(i);
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }
                    } else {
                        // USB OTG host handshake
                        myBT.setModuleName("USB");
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (usbDevices != null && !usbDevices.isEmpty()) {
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                if (UsbSerialDevice.isSupported(device)) {
                                    int flags = (Build.VERSION.SDK_INT >= 31) ? PendingIntent.FLAG_IMMUTABLE : 0;
                                    PendingIntent pi = PendingIntent.getBroadcast(
                                            MainActivityScreen.this, 0, new Intent(ACTION_USB_PERMISSION), flags);
                                    usbManager.requestPermission(device, pi);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent IntentReceiver leak when Activity is torn down
        if (isReceiverRegistered) {
            unregisterReceiver(broadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    private void DisableUI() {
        setEnabledCard(false, btnConfig, image_settings, text_settings);
        setEnabledCard(false, btnStatus, image_status, text_status);
        setEnabledCard(false, btnFlight, image_curve, text_curve);
        setEnabledCard(false, btnReset, image_reset, text_reset);
        setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
        setEnabledCard(true, btnFlashFirmware, image_firmware, text_firmware);
        invalidateOptionsMenu();
    }

    private void setEnabledCard(boolean enable, CardView card, ImageView image, TextView text) {
        card.setEnabled(enable);
        image.setImageAlpha(enable ? 0xFF : 0x3F);
    }

    private void EnableUI() {
        boolean success = false;
        for (int attempt = 0; attempt < 3; attempt++) {
            success = readConfig();
            if (success) break;
        }

        GimbalConfigData config = myBT.getGimbalConfigData();
        if (config != null && ("RocketMotorGimbal".equals(config.getAltimeterName()) ||
                "RocketMotorGimbal_bno055".equals(config.getAltimeterName()))) {

            boolean allowConfig = (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT) ||
                    (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB &&
                            myBT.getAppConf().getFullUSBSupport());

            setEnabledCard(allowConfig, btnConfig, image_settings, text_settings);
            setEnabledCard(allowConfig, btnStatus, image_status, text_status);
            setEnabledCard(allowConfig, btnFlight, image_curve, text_curve);
            setEnabledCard(allowConfig, btnReset, image_reset, text_reset);

            setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);
            text_connect.setText(getResources().getString(R.string.disconnect));
            setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);

            String firmwareVersion = config.getAltiMajorVersion() + "." + config.getAltiMinorVersion();
            if (!firmCompat.IsCompatible(config.getAltimeterName(), firmwareVersion)) {
                msg(getString(R.string.flash_advice_msg));
            } else {
                msg(getResources().getString(R.string.MS_msg4));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg));
            myBT.Disconnect();
        }
        invalidateOptionsMenu();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }

    /**
     * Queries configuration parameters ('b;') from the rocket flight computer without blocking UI thread indefinitely.
     */
    private boolean readConfig() {
        if (!myBT.getConnected()) return false;

        myBT.setDataReady(false);
        myBT.flush();
        myBT.clearInput();
        myBT.write("b;");
        myBT.flush();

        // Read result with internal 3000ms timeout to avoid UI starvation
        String myMessage = myBT.ReadResult(3000);
        if ("OK".equals(myMessage)) {
            myBT.setDataReady(false);
            myMessage = myBT.ReadResult(3000);
        }

        if ("start alticonfig end".equals(myMessage)) {
            try {
                GimbalCfg = myBT.getGimbalConfigData();
                return GimbalCfg != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        myBT.getAppConf().ReadConfig();
        boolean isUsb = (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB);
        menu.findItem(R.id.action_bluetooth).setEnabled(!isUsb && !myBT.getConnected());

        boolean isConnected = myBT.getConnected();
        menu.findItem(R.id.action_mod3dr_settings).setEnabled(!isConnected);
        menu.findItem(R.id.action_modbt_settings).setEnabled(!isConnected);
        menu.findItem(R.id.action_modlora_settings).setEnabled(!isConnected);
        menu.findItem(R.id.action_modlorae32_settings).setEnabled(!isConnected);
        menu.findItem(R.id.action_test_connection).setEnabled(isConnected);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivityScreen.this, AppTabConfigActivity.class));
            return true;
        } else if (id == R.id.action_help) {
            Intent i = new Intent(MainActivityScreen.this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        } else if (id == R.id.action_bluetooth) {
            startActivity(new Intent(MainActivityScreen.this, SearchBluetooth.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(MainActivityScreen.this, AboutActivity.class));
            return true;
        } else if (id == R.id.action_mod3dr_settings) {
            startActivity(new Intent(MainActivityScreen.this, Config3DR.class));
            return true;
        } else if (id == R.id.action_modbt_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigBT.class));
            return true;
        } else if (id == R.id.action_modlora_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigLoraE220.class));
            return true;
        } else if (id == R.id.action_modlorae32_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigLoraE32.class));
            return true;
        } else if (id == R.id.action_test_connection) {
            startActivity(new Intent(MainActivityScreen.this, TestConnection.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /**
     * Background worker handling Bluetooth RFCOMM socket connection handshakes.
     */
    private class ConnectBT extends AsyncTask<Void, Void, Boolean> {
        private AlertDialog alert;

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityScreen.this);
            builder.setMessage(getResources().getString(R.string.MS_msg1) + "\n" + myBT.getModuleName())
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
        protected Boolean doInBackground(Void... devices) {
            if (!myBT.getConnected()) {
                return myBT.connect();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }

            if (Boolean.TRUE.equals(success) && myBT.getConnected()) {
                myBT.setConnected(true);
                EnableUI();
                setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);
                text_connect.setText(getResources().getString(R.string.disconnect));
            } else {
                msg(getResources().getString(R.string.MS_msg5));
            }
        }
    }

    /**
     * Manages firmware compatibility tables for thrust vectoring and altimeter boards.
     */
    public static class FirmwareCompatibility {
        private final Map<String, String> compatibleVersions = new HashMap<>();

        public FirmwareCompatibility() {
            Add("RocketMotorGimbal", "1.3");
            Add("RocketMotorGimbal_bno055", "1.3");
        }

        public void Add(String altiName, String verList) {
            compatibleVersions.put(altiName, verList);
        }

        public boolean IsCompatible(String altiName, String ver) {
            if (altiName == null || ver == null) return false;
            String compatFirmwareList = compatibleVersions.get(altiName);
            if (compatFirmwareList == null) return false;

            String[] versions = compatFirmwareList.split(",");
            for (String target : versions) {
                if (target.trim().equals(ver.trim())) {
                    return true;
                }
            }
            return false;
        }
    }
}