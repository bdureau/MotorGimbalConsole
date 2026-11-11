package com.motorgimbalconsole;

/**
 * ============================================================================
 * Project: MotorGimbalConsole / BearConsole
 * Class: ShareHandler
 * Author: Boris du Reau
 *
 * @description:
 *  Utility class for capturing high-resolution screenshots of active UI views
 *  and sharing them across third-party applications using Android's secure
 *  FileProvider mechanism.
 *
 *  Key architectural enhancements:
 *    - Replaces deprecated View drawing cache with Canvas software rendering.
 *    - Performs asynchronous compression and disk I/O on a dedicated background
 *      thread to guarantee zero UI thread stuttering or frame drops.
 *    - Sanitizes filename date patterns to ensure cross-filesystem compatibility.
 *    - Grants explicit read permissions on shared content URIs for Android 7.0+ (API 24+).
 *    - Writes the screenshots in the "shared" folder of the application cache, the
 *      only folder exposed by the FileProvider (xml/file_path.xml); Android purges it.
 *    - Properly recycles in-memory Bitmaps to prevent OutOfMemoryError exceptions.
 *
 * @author: boris.dureau@neuf.fr
 * ============================================================================
 */

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.motorgimbalconsole.flights.FlightFileExporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareHandler {

    private static final String TAG = "ShareHandler";

    // Dedicated single-thread executor to handle disk I/O operations off the UI thread
    private static final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    /**
     * Captures a screenshot of the specified View and shares it asynchronously.
     *
     * @param view The UI view to be rendered and captured.
     * @param ctx  The calling Context or Activity.
     */
    public static void takeScreenShot(View view, Context ctx) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
            Log.e(TAG, "Cannot take screenshot: View is null or has zero dimensions.");
            return;
        }

        // 1. Allocate an in-memory Bitmap and render the View hierarchy using Canvas
        // (Replaces the deprecated View#getDrawingCache() API)
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        // 2. Generate a sanitized timestamp string without colon (':') characters
        // Colons are illegal filename characters on FAT32/Linux file systems
        Date date = new Date();
        CharSequence format = DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);

        // 3. Offload compression and file writing to a background worker thread
        diskExecutor.execute(() -> {
            // Written in the "shared" cache folder, the only one exposed by the FileProvider
            // (xml/file_path.xml). The former folder (Pictures/FilShare of the app external
            // storage) kept every screenshot forever; the cache is purged by Android.
            File mainDir = FlightFileExporter.getSharedCacheDir(ctx);
            if (!mainDir.exists() && !mainDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory structure: " + mainDir.getAbsolutePath());
                bitmap.recycle();
                return;
            }

            File imageFile = new File(mainDir, "AltiMultiCurve-" + format + ".png");

            try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
                // Compress Bitmap to lossless PNG format
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();

                // Post the share intent launch back to the main UI thread
                new Handler(Looper.getMainLooper()).post(() -> shareScreenShot(imageFile, ctx));
            } catch (IOException e) {
                Log.e(TAG, "Failed to write screenshot image file to disk", e);
            } finally {
                // Explicitly free native pixel allocations to avoid memory leaks
                bitmap.recycle();
            }
        });
    }

    /**
     * Creates an ACTION_SEND intent and launches the Android Chooser dialog
     * to share the captured image file with other applications.
     *
     * @param imageFile The image file stored on disk to be shared.
     * @param ctx       The Context used to launch the sharing activity.
     */
    public static void shareScreenShot(File imageFile, Context ctx) {
        if (imageFile == null || !imageFile.exists()) {
            Toast.makeText(ctx, "Screenshot file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a secure content URI using FileProvider
        Uri uri = FileProvider.getUriForFile(
                ctx,
                ctx.getPackageName() + ".provider",
                imageFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TEXT, "Motor Gimbal has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        // Attached as ClipData too, so that the read permission goes through the chooser
        intent.setClipData(ClipData.newRawUri("", uri));

        // Crucial flag: grant temporary read access permission to the receiving application
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Share With");
        if (!(ctx instanceof Activity)) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            ctx.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, "No application available to share image", Toast.LENGTH_SHORT).show();
        }
    }
}