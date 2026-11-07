package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @description: Main screen (dashboard) of the Motor Gimbal Console application.
 *
 * Responsibilities:
 *  - Displays the main menu cards (connect/disconnect, settings, status, flights,
 *    telemetry, reset, firmware flashing, about) and enables/disables them according
 *    to the connection state and the connection type (Bluetooth or USB).
 *  - Manages the connection life cycle with the board:
 *      * Bluetooth: asks to reuse the last paired module or opens the device search screen.
 *      * USB: finds a supported serial device, requests the USB permission if needed
 *        and connects through a BroadcastReceiver.
 *  - Once the link is open, reads the board configuration ("b;" command), checks the
 *    firmware name and version, and advises flashing if the firmware is not compatible.
 *  - Handles clean disconnection (telemetry is stopped with "y0;" before closing) so
 *    that the board can be reconnected repeatedly without problems.
 *
 * Threading: every operation on the link (connect, config read, disconnect) runs on a
 * single background thread (ioExecutor), so the UI thread is never blocked and link
 * operations never overlap. The UI is updated only once each operation has completed.
 *
 * @author: boris.dureau@neuf.fr
 **/
public class MainActivityScreen extends AppCompatActivity {

    private static final String TAG = "MainActivityScreen";
    private static final int USB_BAUD_RATE = 38400;
    private static final int CONFIG_READ_ATTEMPTS = 4;
    private static final long CONFIG_READ_TIMEOUT_MS = 5000;

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

    /** Every link operation (connect, config read, disconnect) runs on this single thread:
     *  never on the UI thread, and never concurrently. */
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    /** A connection / disconnection is in progress: repeated clicks are ignored. */
    private volatile boolean busy = false;
    /** The user cancelled the connection or asked for a disconnection. */
    private volatile boolean cancelRequested = false;
    private AlertDialog progressDialog;
    private boolean receiverRegistered = false;

    /** Receives the USB permission result and the USB attach / detach events. */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(ACTION_USB_PERMISSION)) {
                @SuppressWarnings("deprecation")
                UsbDevice d = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (d != null) device = d;
                // With an immutable PendingIntent, EXTRA_PERMISSION_GRANTED is not filled in,
                // so ask the UsbManager directly.
                if (device != null && usbManager.hasPermission(device)) {
                    connectUsb();
                } else {
                    busy = false;
                    msg("PERM NOT GRANTED");
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg(getResources().getString(R.string.usb_device_attached));
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (myBT.getConnectionType().equals("usb") && myBT.getConnected()) {
                    // Cable unplugged: no point sending anything to the board
                    disconnectAsync(false);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        //This will check if the firmware is compatible with the app and advice on flashing the firmware
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // ContextCompat applies the flag on every Android version (and satisfies lint).
        // NOT_EXPORTED is fine: USB_PERMISSION is sent through our own PendingIntent
        // (same package), and ATTACHED/DETACHED are protected system broadcasts.
        ContextCompat.registerReceiver(this, broadcastReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;

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
        text_telemetry = (TextView) findViewById(R.id.text_telemetry);
        text_reset = (TextView) findViewById(R.id.text_reset);
        text_status = (TextView) findViewById(R.id.text_status);
        text_firmware = (TextView) findViewById(R.id.text_firmware);
        text_info = (TextView) findViewById(R.id.text_info);
        text_connect = (TextView) findViewById(R.id.text_connect);

        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();
        GimbalCfg = myBT.getGimbalConfigData();

        refreshUI();

        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    //send telemetry command
                    myBT.write("y1;");
                }
                Intent i = new Intent(MainActivityScreen.this, GimbalTelemetryTabActivity.class);
                startActivity(i);
            }
        });

        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Re-read the config in the background, then open the config screen
                if (busy) return;
                busy = true;
                cancelRequested = false;
                ioExecutor.execute(() -> {
                    readConfigWithRetry();
                    runOnUiThread(() -> {
                        busy = false;
                        if (isFinishing() || isDestroyed()) return;
                        startActivity(new Intent(MainActivityScreen.this, GimbalTabConfigActivity.class));
                    });
                });
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
        //commands to be sent to flash the firmware
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

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (busy) return; // connection / disconnection already in progress

                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT)
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {
                    disconnectAsync(true);
                    return;
                }

                if (myBT.getConnectionType().equals("bluetooth")) {
                    address = myBT.getAddress();
                    if (address != null) {
                        new AlertDialog.Builder(MainActivityScreen.this)
                                .setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + "?")
                                .setTitle("")
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.Yes), (dialog, id) -> {
                                    dialog.cancel();
                                    // The UI is updated once the connection has completed, not here
                                    connectBluetooth();
                                })
                                .setNegativeButton(R.string.no, (dialog, id) -> {
                                    dialog.cancel();
                                    // choose the bluetooth device
                                    startActivity(new Intent(MainActivityScreen.this, SearchBluetooth.class));
                                })
                                .create()
                                .show();
                    } else {
                        // choose the bluetooth device
                        startActivity(new Intent(MainActivityScreen.this, SearchBluetooth.class));
                    }
                } else {
                    myBT.setModuleName("USB");
                    startUsbConnection();
                }
            }
        });

        /*btnConnectDisconnect.setOnClickListener(v -> {
            myBT.getAppConf().ReadConfig();
            if (myBT.getAppConf().getConnectionType() == 0)
                myBT.setConnectionType("bluetooth");
            else
                myBT.setConnectionType("usb");

            // If already connected, initiate disconnection sequence
            if (myBT.getConnected()) {
                Disconnect();
                DisableUI();
                setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
                text_connect.setText(getResources().getString(R.string.connect));
            } else {
                // Handling Bluetooth connection path
                if (myBT.getConnectionType().equals("bluetooth")) {
                    address = myBT.getAddress();
                    if (address != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivityScreen.this);
                        builder.setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + " ?")
                                .setTitle("")
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.Yes), (dialog, id) -> {
                                    dialog.cancel();
                                    new ConnectBT().execute();
                                })
                                .setNegativeButton(R.string.no, (dialog, id) -> {
                                    dialog.cancel();
                                    Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                                    startActivity(i);
                                });
                        builder.create().show();
                    } else {
                        // No remembered address, open device discovery
                        Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                        startActivity(i);
                    }
                } else {
                    // Handling USB connection path
                    myBT.setModuleName("USB");
                    HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                    if (!usbDevices.isEmpty()) {
                        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                            device = entry.getValue();
                            Log.d(TAG, String.format("USBDevice detected (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                                    device.getVendorId(), device.getProductId(),
                                    UsbSerialDevice.isSupported(device),
                                    device.getDeviceClass(), device.getDeviceSubclass(),
                                    device.getDeviceName()));

                            // Request USB permission if device contains a recognized serial chip
                            if (UsbSerialDevice.isSupported(device)) {
                                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
                                Intent intent = new Intent(ACTION_USB_PERMISSION);
                                intent.setPackage(getPackageName()); // Explicit package for security

                                PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, flags);
                                Log.d(TAG, "Requesting permission for vendor ID: " + device.getVendorId());
                                usbManager.requestPermission(device, pi);
                                break;
                            }
                        }
                    }
                }
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When coming back from another screen (BT search, telemetry...), resync the display
        if (!busy) refreshUI();
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        if (receiverRegistered) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
        ioExecutor.shutdown();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------
    // Connection / disconnection
    // ---------------------------------------------------------------------------------------

    /** Connects to the last selected Bluetooth module. */
    private void connectBluetooth() {
        myBT.setConnectionType("bluetooth");
        performConnect(() -> myBT.connect());
    }

    /** Looks for a supported USB serial device and connects to it (asking for permission if needed). */
    private void startUsbConnection() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (usbDevices.isEmpty()) return;

        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            UsbDevice d = entry.getValue();
            if (!UsbSerialDevice.isSupported(d)) continue;

            device = d;
            if (usbManager.hasPermission(device)) {
                // Permission already granted (reconnection case): connect directly
                connectUsb();
            } else {
                busy = true;
                Intent permIntent = new Intent(ACTION_USB_PERMISSION);
                permIntent.setPackage(getPackageName()); // explicit intent (required by Android 14)
                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, permIntent, flags);
                usbManager.requestPermission(device, pi);
            }
            break;
        }
    }

    /** Connects to the current USB device (permission must already be granted). */
    private void connectUsb() {
        final UsbDevice target = device;
        myBT.setConnectionType("usb");
        busy = false; // performConnect sets it again
        performConnect(() -> myBT.connect(usbManager, target, USB_BAUD_RATE));
    }

    /**
     * Opens the link, then reads the configuration, entirely in the background.
     * The UI is updated only once the result is known.
     */
    private void performConnect(final Callable<Boolean> connector) {
        if (busy) return;
        busy = true;
        cancelRequested = false;
        showProgress(getResources().getString(R.string.MS_msg1) + "\n" + myBT.getModuleName());

        ioExecutor.execute(() -> {
            boolean linkOk = false;
            boolean cfgOk = false;
            try {
                // Start from a clean state in case a previous session is still lingering
                if (myBT.getConnected()) {
                    safeDisconnectLink();
                }
                Boolean r = connector.call();
                linkOk = r != null && r;
                if (linkOk && !cancelRequested) {
                    myBT.setConnected(true);
                    cfgOk = readConfigWithRetry();
                }
                if (linkOk && (cancelRequested || !cfgOk)) {
                    safeDisconnectLink();
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                safeDisconnectLink();
                linkOk = false;
            }
            final boolean fLink = linkOk;
            final boolean fCfg = cfgOk;
            runOnUiThread(() -> onConnectionFinished(fLink, fCfg));
        });
    }

    /** Called on the UI thread when a connection attempt has completed. */
    private void onConnectionFinished(boolean linkOk, boolean cfgOk) {
        dismissProgress();
        busy = false;
        if (isFinishing() || isDestroyed()) return;

        if (cancelRequested) {
            refreshUI();
            return;
        }
        if (!linkOk || !cfgOk) {
            //Connection Failed. Is it a SPP Bluetooth? Try again.
            msg(getResources().getString(R.string.MS_msg5));
            refreshUI();
            return;
        }

        GimbalCfg = myBT.getGimbalConfigData();
        if (!isSupportedFirmware()) {
            msg(getString(R.string.unsuported_firmware_msg));
            disconnectAsync(false);
            return;
        }

        refreshUI();
        if (!firmCompat.IsCompatible(GimbalCfg.getAltimeterName(),
                GimbalCfg.getAltiMajorVersion() + "." + GimbalCfg.getAltiMinorVersion())) {
            msg(getString(R.string.flash_advice_msg));
        } else {
            msg(getResources().getString(R.string.MS_msg4));
        }
    }

    /** Cancellation from the "Connecting..." dialog. */
    private void cancelConnection() {
        cancelRequested = true;
        myBT.setExit(true);
        // Closing the socket from another thread unblocks a pending Bluetooth connect()
        new Thread(this::safeDisconnectLink).start();
    }

    /**
     * Clean disconnection, in the background.
     * @param stopTelemetry sends "y0;" before closing so that the board stops streaming
     *                      telemetry (otherwise it pollutes the next reconnection).
     */
    private void disconnectAsync(final boolean stopTelemetry) {
        busy = true;
        cancelRequested = true;
        myBT.setExit(true); // interrupts a ReadResult in progress
        ioExecutor.execute(() -> {
            if (stopTelemetry && myBT.getConnected()) {
                try {
                    myBT.write("y0;");
                    myBT.flush();
                    Thread.sleep(150);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to stop telemetry", e);
                }
            }
            safeDisconnectLink();
            runOnUiThread(this::showDisconnected);
        });
    }

    /** Closes the link and ALWAYS resets the "connected" state to false. */
    private void safeDisconnectLink() {
        try {
            myBT.Disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Disconnect failed", e);
        }
        try {
            myBT.setConnected(false);
        } catch (Exception e) {
            Log.w(TAG, "setConnected(false) failed", e);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Configuration reading (always called off the UI thread)
    // ---------------------------------------------------------------------------------------

    /** Reads the board configuration, with several attempts. */
    private boolean readConfigWithRetry() {
        if (!myBT.getConnected()) return false;

        // If the board was still streaming telemetry (abrupt disconnection), stop it
        try {
            myBT.write("y0;");
            myBT.flush();
            Thread.sleep(300);
            myBT.clearInput();
        } catch (Exception e) {
            Log.w(TAG, "Unable to stop telemetry before reading config", e);
        }

        for (int attempt = 1; attempt <= CONFIG_READ_ATTEMPTS; attempt++) {
            if (cancelRequested || !myBT.getConnected()) return false;
            if (readConfigOnce()) return true;
            Log.d(TAG, "Config read attempt " + attempt + " failed");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** Sends "b;" and waits for "start alticonfig end". Returns true on success. */
    private boolean readConfigOnce() {
        if (myBT.getInputStream() == null) return false;

        myBT.setDataReady(false);
        myBT.flush();
        myBT.clearInput();
        myBT.write("b;");
        myBT.flush();

        // ReadResult waits for the data itself (with a timeout): no more busy-wait loop.
        // Any "OK" (reply to y0; or acknowledgement of b;) is skipped.
        for (int i = 0; i < 3; i++) {
            if (cancelRequested) return false;
            String myMessage = myBT.ReadResult(CONFIG_READ_TIMEOUT_MS);
            if (myMessage == null) return false;
            if (myMessage.contains("alticonfig") && myMessage.contains("end")) {
                GimbalCfg = myBT.getGimbalConfigData();
                return true;
            }
            if (!myMessage.trim().equals("OK")) return false;
            myBT.setDataReady(false);
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------------------------

    /** Returns true if the connected board runs a firmware supported by this application. */
    private boolean isSupportedFirmware() {
        GimbalConfigData cfg = myBT.getGimbalConfigData();
        if (cfg == null || cfg.getAltimeterName() == null) return false;
        String name = cfg.getAltimeterName();
        return name.equals("RocketMotorGimbal") || name.equals("RocketMotorGimbal_bno055");
    }

    /** Puts the cards in the state matching the current connection (without reading anything). */
    private void refreshUI() {
        if (myBT.getConnected() && isSupportedFirmware()) {
            showConnected();
        } else {
            showDisconnected();
        }
    }

    /** Enables the cards available while connected. */
    private void showConnected() {
        boolean fullAccess = myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT ||
                (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB &&
                        myBT.getAppConf().getFullUSBSupport());

        setEnabledCard(fullAccess, btnConfig, image_settings, text_settings);
        setEnabledCard(fullAccess, btnStatus, image_status, text_status);
        setEnabledCard(fullAccess, btnFlight, image_curve, text_curve);
        setEnabledCard(fullAccess, btnReset, image_reset, text_reset);
        setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);
        // cannot flash firmware if connected
        setEnabledCard(false, btnFlashFirmware, image_firmware, text_firmware);
        text_connect.setText(getResources().getString(R.string.disconnect));
        invalidateOptionsMenu();
    }

    /** Puts the screen in the disconnected state. */
    private void showDisconnected() {
        busy = false;
        DisableUI();
        text_connect.setText(getResources().getString(R.string.connect_disconnect));
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
        image.setImageAlpha(enable ? 0xFF : 0x3F);
    }

    /** Shows the "Connecting..." dialog with a Cancel button. */
    private void showProgress(String message) {
        dismissProgress();
        progressDialog = new AlertDialog.Builder(MainActivityScreen.this)
                .setMessage(message)
                .setTitle(getResources().getString(R.string.MS_msg2))
                .setCancelable(false)
                .setNegativeButton(getResources().getString(R.string.MS_cancel), (dialog, id) -> {
                    dialog.cancel();
                    cancelConnection();
                })
                .create();
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            try {
                if (progressDialog.isShowing()) progressDialog.dismiss();
            } catch (IllegalArgumentException ignored) {
                // activity already detached
            }
            progressDialog = null;
        }
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
        menu.findItem(R.id.action_bluetooth).setEnabled(
                myBT.getAppConf().getConnectionType() != GlobalConfig.ConnectionType.USB);

        //if we are connected then enable some menu options and if not disable them
        boolean connected = myBT.getConnected();
        if (connected) {
            // We are connected so no need to choose the bluetooth
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        }
        // Module configuration only when not connected
        menu.findItem(R.id.action_mod3dr_settings).setEnabled(!connected);
        menu.findItem(R.id.action_modbt_settings).setEnabled(!connected);
        menu.findItem(R.id.action_modlora_settings).setEnabled(!connected);
        menu.findItem(R.id.action_modlorae32_settings).setEnabled(!connected);
        // Connection testing only when connected
        menu.findItem(R.id.action_test_connection).setEnabled(connected);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivityScreen.this, AppTabConfigActivity.class));
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
            startActivity(new Intent(MainActivityScreen.this, SearchBluetooth.class));
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(MainActivityScreen.this, AboutActivity.class));
            return true;
        }
        if (id == R.id.action_mod3dr_settings) {
            startActivity(new Intent(MainActivityScreen.this, Config3DR.class));
            return true;
        }
        if (id == R.id.action_modbt_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigBT.class));
            return true;
        }
        //Open the lora module config
        if (id == R.id.action_modlora_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigLoraE220.class));
            return true;
        }
        //Open the lora module config
        if (id == R.id.action_modlorae32_settings) {
            startActivity(new Intent(MainActivityScreen.this, ConfigLoraE32.class));
            return true;
        }
        //Test current connection
        if (id == R.id.action_test_connection) {
            startActivity(new Intent(MainActivityScreen.this, TestConnection.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /** Lists the firmware versions compatible with this application, per board name. */
    public class FirmwareCompatibility {
        public final HashMap<String, String> hm = new HashMap<>();

        FirmwareCompatibility() {
            //init compatible versions
            Add("RocketMotorGimbal", "1.3");
            Add("RocketMotorGimbal_bno055", "1.3");
        }

        public void Add(String altiName, String verList) {
            hm.put(altiName, verList);
        }

        public boolean IsCompatible(String altiName, String ver) {
            String compatFirmwareList = hm.get(altiName);
            if (compatFirmwareList == null || ver == null) return false;
            for (String v : compatFirmwareList.split(",")) {
                if (v.trim().equals(ver)) return true;
            }
            return false;
        }
    }
}