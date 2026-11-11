package com.motorgimbalconsole.connection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.motorgimbalconsole.ConsoleApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 *   @description: This class has all the methods to deal with your bluetooth connection
 *   (classic Bluetooth SPP / RFCOMM link to the HC-05 / HC-06 module of the board).
 *   All methods are null-safe: calling them before connect() or after Disconnect() does
 *   nothing instead of throwing a NullPointerException (only IOException was caught before,
 *   so a write after a lost connection crashed the application).
 *   @author: boris.dureau@neuf.fr
 **/
public class BluetoothConnection {
    BluetoothDevice device;
    BluetoothAdapter myBluetooth = null;
    private static final String TAG = "BluetoothConnection";
    // Opened on a background thread, used by the reader and UI threads: volatile
    private volatile BluetoothSocket btSocket = null;
    private volatile boolean isBtConnected = false;
    //private String address = null;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Returns the Bluetooth adapter of the phone through the BluetoothManager system service
     * (BluetoothAdapter.getDefaultAdapter() is deprecated since API 31).
     *
     * @param context any context
     * @return the adapter, or null if the device has no Bluetooth
     */
    @Nullable
    public static BluetoothAdapter getAdapter(Context context) {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return (manager != null) ? manager.getAdapter() : null;
    }

    /**
     * Runtime permissions to request before using Bluetooth. Before Android 12 the install-time
     * BLUETOOTH / BLUETOOTH_ADMIN permissions are enough, so nothing is requested.
     *
     * @return the permissions of the "Nearby devices" group (Android 12+), or an empty array
     */
    public static String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        }
        return new String[0];
    }

    /**
     * @param context any context
     * @return true if the application may connect to Bluetooth devices
     */
    public static boolean hasConnectPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /** @return true if a discovery may be cancelled (BLUETOOTH_SCAN on Android 12+) */
    private static boolean hasScanPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Opens the RFCOMM (SPP) link to the board module. Blocking: called from a background thread.
     *
     * @param address MAC address of the paired module
     * @return true if the socket is connected
     */
    @SuppressLint("MissingPermission") // permissions checked by SearchBluetooth; SecurityException caught
    public boolean connect(String address) {
        boolean state;
        try {
            Context context = ConsoleApplication.getInstance();
            myBluetooth = getAdapter(context);//get the mobile bluetooth device
            BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
            setBtSocket(btDevice.createInsecureRfcommSocketToServiceRecord(myUUID));//create a RFCOMM (SPP) connection
            // A running discovery slows the connection down. The application never starts one,
            // and cancelling requires BLUETOOTH_SCAN on Android 12+: do it only when allowed
            // (otherwise the SecurityException made the whole connection fail).
            if (hasScanPermission(context)) {
                myBluetooth.cancelDiscovery();
            }
            getBtSocket().connect();//start connection
            state = true;
            setBTConnected(true);

        } catch (IOException e) {
            state = false;//if the try failed, you can check the exception here
            setBTConnected(false);
        } catch (IllegalArgumentException | SecurityException | NullPointerException e) {
            // Invalid / missing address, BLUETOOTH_CONNECT permission revoked, or no Bluetooth
            // adapter on the device: report a failed connection instead of crashing
            Log.w(TAG, "Unable to connect", e);
            state = false;
            setBTConnected(false);
        }
        return state;
    }
    public void setBTConnected(boolean BtConnected) {
        isBtConnected = BtConnected;
    }

    public boolean getBTConnected() {
        return isBtConnected;
    }

    public void setBtSocket(BluetoothSocket mybtSocket) {
        btSocket = mybtSocket;
    }

    public BluetoothSocket getBtSocket() {
        return btSocket;
    }


    /** @return the socket input stream, or null if the link is not open */
    public InputStream getInputStream () {
        BluetoothSocket socket = btSocket;
        if (socket == null) return null;
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    /** Closes the link. Safe to call several times. */
    public void Disconnect() {
        BluetoothSocket socket = btSocket;
        if (socket != null) //If the btSocket is busy
        {
            try {
                socket.close(); //close connection
            } catch (IOException e) {
                Log.w(TAG, "Error closing the socket", e);
            }
        }
        setBTConnected(false);
    }

    /** Sends a command to the board; a write error marks the link as disconnected. */
    public void write (String data) {
        BluetoothSocket socket = btSocket;
        if (socket == null) return;
        try {
            socket.getOutputStream().write(data.getBytes());
        } catch (IOException e) {
            Log.w(TAG, "Write failed, link lost", e);
            setBTConnected(false);
        }
    }

    public void flush() {
        BluetoothSocket socket = btSocket;
        if (socket == null) return;
        try {
            socket.getOutputStream().flush();
        } catch (IOException e) {
            Log.w(TAG, "Flush failed", e);
        }
    }

    /** Discards the bytes received but not read yet. */
    public void clearInput() {
        InputStream in = getInputStream();
        if (in == null) return;
        try {
            in.skip(in.available());
        } catch (IOException e) {
            Log.w(TAG, "clearInput failed", e);
        }
    }

    /*public void setAddress(String bTAddress) {

        address = bTAddress;

    }
    public String getAddress () {return address;}
*/
}
