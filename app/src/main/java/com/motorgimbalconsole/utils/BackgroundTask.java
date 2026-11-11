package com.motorgimbalconsole.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BackgroundTask.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: drop-in replacement for android.os.AsyncTask (deprecated since API 30) for the
 * board communication tasks of the application (module configuration, calibration, gimbal
 * configuration read / upload).
 *
 * It keeps the AsyncTask method names so that the existing tasks only change their parent
 * class:
 *  - onPreExecute() runs immediately on the calling (UI) thread, e.g. to show a dialog;
 *  - doInBackground() runs on a background thread;
 *  - onPostExecute() runs afterwards on the UI thread, e.g. to dismiss the dialog.
 *
 * All tasks share ONE serial background thread, like the default AsyncTask executor. This is
 * deliberate: they all talk to the same serial link (Bluetooth or USB), and two tasks writing
 * commands and reading answers at the same time would mix up the replies.
 *
 * Unlike AsyncTask, an exception thrown in doInBackground() is logged instead of crashing the
 * application, and onPostExecute() is still called so that progress dialogs are always closed.
 */
public abstract class BackgroundTask {

    private static final String TAG = "BackgroundTask";

    /** Single thread shared by every board communication task (keeps commands in order). */
    private static final ExecutorService BOARD_IO_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "BoardIO");
                thread.setDaemon(true);
                return thread;
            });

    /** Posts the completion callbacks on the UI thread. */
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Called on the UI thread before the background work starts. */
    protected void onPreExecute() {
    }

    /**
     * Background work (board commands, waiting for answers). Never touch views here.
     *
     * @param params unused, kept for compatibility with the former AsyncTask signature
     * @return unused, always null
     */
    protected abstract Void doInBackground(Void... params);

    /**
     * Called on the UI thread once the background work is finished (even if it failed).
     *
     * @param result unused, always null
     */
    protected void onPostExecute(Void result) {
    }

    /**
     * Starts the task. Must be called from the UI thread, like AsyncTask.execute().
     *
     * @return this task
     */
    public final BackgroundTask execute() {
        onPreExecute();
        BOARD_IO_EXECUTOR.execute(() -> {
            try {
                doInBackground();
            } catch (RuntimeException e) {
                Log.e(TAG, "Background task failed", e);
            }
            MAIN_HANDLER.post(() -> onPostExecute(null));
        });
        return this;
    }

    /**
     * Runs a piece of board communication code on the shared serial background thread,
     * after every task already queued.
     *
     * @param work code to run in the background
     */
    public static void runOnBoardThread(Runnable work) {
        BOARD_IO_EXECUTOR.execute(() -> {
            try {
                work.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "Background work failed", e);
            }
        });
    }

    /**
     * Runs a piece of code on the UI thread.
     *
     * @param work code to run on the UI thread
     */
    public static void runOnUiThread(Runnable work) {
        MAIN_HANDLER.post(work);
    }
}
