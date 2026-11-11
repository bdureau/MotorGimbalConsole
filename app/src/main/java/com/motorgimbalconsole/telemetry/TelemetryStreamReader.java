package com.motorgimbalconsole.telemetry;

import android.util.Log;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.utils.BackgroundTask;

/**
 * TelemetryStreamReader.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: starts and stops the telemetry stream of the gimbal board and reads it on a
 * dedicated background thread. Used by the telemetry and status screens, which used to
 * duplicate this logic with several problems:
 *  - the "stop" path waited for the board answer on the UI thread
 *    (while (available() <= 0); then ReadResult(100000)): the application froze (ANR) when
 *    the board did not answer;
 *  - the reading loop could not be stopped reliably: ReadResult() resets its exit flag when
 *    it starts, so a stop request arriving at that moment was lost and the old reader kept
 *    consuming the stream while a new one was started;
 *  - the running flags were not volatile, so the reader thread could miss the stop.
 *
 * How it works:
 *  - start() and stop() only change state and return immediately (safe on the UI thread);
 *  - the "y1;" / "y0;" commands are sent from the shared board I/O thread
 *    (BackgroundTask.runOnBoardThread), so they are executed in the order they were requested
 *    and never interleave with other board commands;
 *  - stop() interrupts the reader thread (ReadResult() checks the interrupt flag) and waits
 *    for it in the background, so there are never two readers on the link;
 *  - a generation counter discards a start that was cancelled before it ran.
 */
public final class TelemetryStreamReader {

    private static final String TAG = "TelemetryStreamReader";

    /** Board command that enables the telemetry stream. */
    private static final String CMD_TELEMETRY_ON = "y1;";
    /** Board command that disables the telemetry stream. */
    private static final String CMD_TELEMETRY_OFF = "y0;";
    /** Timeout of one ReadResult() call (silence before it returns). */
    private static final long READ_TIMEOUT_MS = 10000;
    /** Maximum time to wait for the reader thread to finish when stopping. */
    private static final long STOP_JOIN_TIMEOUT_MS = 2000;
    /** Pause before retrying when the link is closed, to avoid a busy loop. */
    private static final long LINK_DOWN_RETRY_MS = 250;

    private final ConsoleApplication app;

    // State shared between the UI thread, the board I/O thread and the reader thread,
    // always accessed while holding the lock of this object.
    private boolean running = false;
    private int generation = 0;
    private Thread readerThread = null;

    /**
     * @param app application object holding the Bluetooth / USB link
     */
    public TelemetryStreamReader(ConsoleApplication app) {
        this.app = app;
    }

    /** @return true between start() and stop() */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Enables the telemetry stream and starts reading it. Returns immediately.
     * Does nothing if the reader is already running.
     */
    public void start() {
        final int myGeneration;
        synchronized (this) {
            if (running) return;
            running = true;
            myGeneration = ++generation;
        }
        BackgroundTask.runOnBoardThread(() -> {
            if (!isCurrent(myGeneration)) return; // stopped before it could start
            if (app.getConnected()) {
                app.flush();
                app.clearInput();
                app.write(CMD_TELEMETRY_ON);
                app.flush();
            }
            Thread thread = new Thread(() -> readLoop(myGeneration), "TelemetryReader");
            thread.setDaemon(true);
            synchronized (this) {
                if (!running || generation != myGeneration) return;
                readerThread = thread;
            }
            thread.start();
        });
    }

    /**
     * Stops reading and disables the telemetry stream. Returns immediately: the reader thread
     * is joined and the "y0;" command is sent in the background.
     */
    public void stop() {
        final Thread reader;
        synchronized (this) {
            if (!running && readerThread == null) return;
            running = false;
            generation++;
            reader = readerThread;
            readerThread = null;
        }
        if (reader != null) {
            reader.interrupt();
        }
        app.setExit(true); // makes a ReadResult() in progress return
        BackgroundTask.runOnBoardThread(() -> {
            if (reader != null) {
                try {
                    reader.join(STOP_JOIN_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (reader.isAlive()) {
                    Log.w(TAG, "Telemetry reader did not stop in time");
                }
            }
            if (app.getConnected()) {
                app.flush();
                app.clearInput();
                app.write(CMD_TELEMETRY_OFF);
                app.flush();
            }
        });
    }

    /** @return true if the given start request is still the active one */
    private synchronized boolean isCurrent(int myGeneration) {
        return running && generation == myGeneration;
    }

    /**
     * Reader thread body: parses the telemetry sentences (ReadResult() forwards the values to
     * the screen through the application handler) until stop() is called.
     */
    private void readLoop(int myGeneration) {
        while (isCurrent(myGeneration) && !Thread.currentThread().isInterrupted()) {
            if (!app.getConnected() || app.getInputStream() == null) {
                if (!sleepQuietly(LINK_DOWN_RETRY_MS)) break;
                continue;
            }
            String result = app.ReadResult(READ_TIMEOUT_MS);
            if (result != null && result.contains("error")) {
                // Link broken: do not spin on a dead stream
                if (!sleepQuietly(LINK_DOWN_RETRY_MS)) break;
            }
        }
        Log.d(TAG, "Telemetry reader stopped");
    }

    /** @return false if the thread was interrupted while sleeping */
    private static boolean sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
