package com.motorgimbalconsole.flights;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * FlightFileExporter.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: writes the files produced from a downloaded flight with the storage APIs of
 * current Android versions.
 *
 *  - CSV export: the files go to Download/MotorGimbalConsoleFlights, where the user finds them
 *    with any file manager. From Android 10 they are written through MediaStore, which needs no
 *    permission; on Android 9 and lower the public folder is written directly (permission
 *    WRITE_EXTERNAL_STORAGE, declared with maxSdkVersion="28").
 *    The former code wrote the public folder with java.io.File on every version: this relies
 *    on the legacy storage mode that Android 11+ no longer grants.
 *  - Sharing: the zip of curves is built in the application cache ("shared" folder), the only
 *    folder exposed by the FileProvider (xml/file_path.xml). It no longer leaves a copy of
 *    every CSV and of the zip in the Download folder each time the user shares a flight.
 */
public final class FlightFileExporter {

    private static final String TAG = "FlightFileExporter";

    /** Sub-folder of the public Download folder holding the exported curves. */
    public static final String EXPORT_FOLDER = "MotorGimbalConsoleFlights";

    /** Sub-folder of the application cache exposed by the FileProvider. */
    public static final String SHARED_CACHE_FOLDER = "shared";

    /** MIME type of the exported curves. */
    public static final String MIME_CSV = "text/csv";

    private FlightFileExporter() {
        // Static helper class: no instance
    }

    /**
     * Returns (and creates if needed) the cache folder whose files may be shared with other
     * applications through the FileProvider.
     *
     * @param context any context
     * @return the shared cache folder
     */
    public static File getSharedCacheDir(Context context) {
        File dir = new File(context.getCacheDir(), SHARED_CACHE_FOLDER);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Unable to create " + dir);
        }
        return dir;
    }

    /**
     * Saves a text file in Download/MotorGimbalConsoleFlights.
     *
     * @param context  any context
     * @param fileName name of the file to create
     * @param content  text content (written in UTF-8)
     * @param mimeType MIME type of the file, e.g. {@link #MIME_CSV}
     * @return true if the file was written
     */
    public static boolean saveToDownloads(Context context, String fileName, String content, String mimeType) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveWithMediaStore(context, fileName, bytes, mimeType);
        }
        return saveToLegacyPublicFolder(fileName, bytes);
    }

    /**
     * Android 10+: inserts the file in the MediaStore Downloads collection. The entry is marked
     * pending while it is written so that other applications never see a partial file.
     */
    private static boolean saveWithMediaStore(Context context, String fileName, byte[] bytes, String mimeType) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + EXPORT_FOLDER);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Log.e(TAG, "MediaStore refused to create " + fileName);
            return false;
        }
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) throw new IOException("No output stream for " + uri);
            out.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error writing " + fileName, e);
            resolver.delete(uri, null, null); // do not leave an empty or partial file
            return false;
        }
        values.clear();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0); // file complete: make it visible
        resolver.update(uri, values, null, null);
        return true;
    }

    /** Android 9 and lower: writes the public Download folder directly. */
    @SuppressWarnings("deprecation") // getExternalStoragePublicDirectory: only used below API 29
    private static boolean saveToLegacyPublicFolder(String fileName, byte[] bytes) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                EXPORT_FOLDER);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Unable to create " + dir);
            return false;
        }
        try (FileOutputStream out = new FileOutputStream(new File(dir, fileName))) {
            out.write(bytes);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing " + fileName, e);
            return false;
        }
    }

    /**
     * Builds a zip archive in the shared cache folder, ready to be shared.
     *
     * @param context any context
     * @param zipName name of the archive
     * @param entries file name to text content, in the order they must appear in the archive
     * @return the archive
     * @throws IOException if the archive cannot be written
     */
    public static File writeZipToSharedCache(Context context, String zipName, Map<String, String> entries)
            throws IOException {
        File zipFile = new File(getSharedCacheDir(context), zipName);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipFile;
    }
}
