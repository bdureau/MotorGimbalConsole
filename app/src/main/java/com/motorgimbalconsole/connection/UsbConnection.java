package com.motorgimbalconsole.connection;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

//import com.felhr.usbserial.SerialInputStream;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
/**
 *   @description: USB serial link to the gimbal board (CDC / FTDI / CH340... adapters through
 *   the felhr usbserial library). Exposes the received bytes as an InputStream so that the
 *   rest of the application reads Bluetooth and USB the same way.
 *   All methods are null-safe: calling them on a closed or never opened port does nothing
 *   instead of crashing (the screens call write() / clearInput() during their lifecycle even
 *   when the board has been unplugged).
 *   @author: boris.dureau@neuf.fr
 **/

public class UsbConnection {

    private static final String TAG = "UsbConnection";
    /** Receive buffer size: about 1 s of data at 38400 bauds. */
    private static final int RX_BUFFER_SIZE = 4096;

    // Opened by connect() on a background thread, used by the reader and UI threads
    private volatile UsbSerialDevice serialPort;
    private volatile UsbDeviceConnection connection;
    private volatile SerialInputStream usbInputStream;
    private volatile boolean isUSBConnected = false;

    public boolean connect( UsbManager usbManager,UsbDevice device, int baudRate) {
        boolean connected = false;
        Disconnect(); // never leak a previous port
        connection = usbManager.openDevice(device);
        if (connection == null) {
            // No USB permission, or the device has been unplugged
            Log.d("SERIAL", "UNABLE TO OPEN DEVICE");
            setUSBConnected(false);
            return false;
        }
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) { //Set Serial Connection Parameters.

                serialPort.setBaudRate(baudRate);
                //serialPort.setBaudRate(38400);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);//ok
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_DSR_DTR);//non
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_RTS_CTS);//ok
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_XON_XOFF);//ok

                usbInputStream = new SerialInputStream(serialPort);

                connected = true;
            } else {
                Log.d("SERIAL", "PORT NOT OPEN");
            }
        } else {
            Log.d("SERIAL", "PORT IS NULL");
        }
        if (!connected) {
            Disconnect(); // release the device connection opened above
        }
        setUSBConnected(connected);
        return connected;

    }
    /**
     * Closes the port and the USB device connection. Safe to call several times or when the
     * port was never opened (it used to throw a NullPointerException, and the device
     * connection was never closed).
     */
    public void Disconnect() {
        setUSBConnected(false);
        SerialInputStream in = usbInputStream;
        usbInputStream = null;
        if (in != null) {
            in.close(); // unblocks a reader waiting in read()
        }
        UsbSerialDevice port = serialPort;
        serialPort = null;
        if (port != null) {
            try {
                port.close();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error closing the serial port", e);
            }
        }
        UsbDeviceConnection conn = connection;
        connection = null;
        if (conn != null) {
            conn.close();
        }
    }

    /** Sends a command to the board; ignored if the port is closed. */
    public void write (String data) {
        UsbSerialDevice port = serialPort;
        if (port != null) {
            port.write(data.getBytes());
        }
    }

    public void flush () {
     //not implemented
    }

    /** Discards the bytes received but not read yet. */
    public void clearInput() {
        SerialInputStream in = usbInputStream;
        if (in == null) return;
        try {
            in.skip(in.available());
        } catch (IOException e) {
            Log.w(TAG, "clearInput failed", e);
        }
    }

    public InputStream getInputStream () {
        return usbInputStream;
    }
    public void setUSBConnected(boolean UsbConnected) {
        isUSBConnected = UsbConnected;
    }

    public boolean getUSBConnected() {
        return isUSBConnected;
    }
    /**
     * Created by BDUREAU on 29/07/2017.
     * Do not use the class from USB serial because the available method always return 0
     */
   public class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback
    {
        protected final UsbSerialInterface device;
        protected final ArrayBlockingQueue<Integer> data = new ArrayBlockingQueue<>(RX_BUFFER_SIZE);
        /** Bytes dropped because the application did not read fast enough (for the logs). */
        private long droppedBytes = 0;

        protected volatile boolean is_open;

        public SerialInputStream(UsbSerialInterface mydevice)
        {
            this.device = mydevice;
            is_open = true;
            device.read(this);
        }

        @Override
        public int available() throws IOException {

            return data.size();
        }

        @Override
        public int read()
        {
            while (is_open)
            {
                try
                {
                    int value = data.take();
                    if (!is_open && value < 0) return -1; // close() marker
                    return value;
                } catch (InterruptedException e)
                {
                    // ignore, will be retried by while loop
                }
            }
            return -1;
        }

        public void close()
        {
            is_open = false;
            data.clear();
            // Wakes up a reader blocked in read(); never blocks (the queue was just cleared)
            data.offer(-1);
        }

        /**
         * Called by the usbserial library on its own thread for every received chunk.
         * Uses offer() instead of put(): put() blocked the library thread when the buffer was
         * full, which stalled the whole USB reception. When the buffer is full the oldest
         * unread data is no longer useful anyway, so the new bytes are dropped and counted.
         */
        public void onReceivedData(byte[] new_data)
        {
            if (!is_open || new_data == null) return;
            for (byte b : new_data)
            {
                if (!data.offer(((int)b) & 0xff)) {
                    droppedBytes++;
                    if (droppedBytes % 1024 == 1) {
                        Log.w(TAG, "USB receive buffer full, bytes dropped: " + droppedBytes);
                    }
                }
            }
        }
    }

}
