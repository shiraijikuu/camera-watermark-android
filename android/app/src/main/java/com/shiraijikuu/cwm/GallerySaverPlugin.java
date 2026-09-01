package com.shiraijikuu.cwm;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Save a base64 image directly into the system gallery (Pictures/Camera-WaterMark).
 * Android 10+ (API 29) uses MediaStore and needs NO storage permission.
 * Android 9 and below writes to the public Pictures dir (WRITE_EXTERNAL_STORAGE, maxSdk 28).
 */
@CapacitorPlugin(name = "GallerySaver")
public class GallerySaverPlugin extends Plugin {

    private static final String ALBUM = "Camera-WaterMark";

    @PluginMethod
    public void saveImage(final PluginCall call) {
        final String b64 = call.getString("base64", "");
        final String name = call.getString("name", "image.jpg");
        final String mime = call.getString("mime", "image/jpeg");
        if (b64 == null || b64.isEmpty()) { call.reject("EMPTY_IMAGE"); return; }

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    final Context ctx = getContext();
                    String saved;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name);
                        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, mime);
                        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES + File.separator + ALBUM);
                        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
                        ContentResolver cr = ctx.getContentResolver();
                        Uri uri = cr.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new Exception("MediaStore insert failed");
                        OutputStream os = cr.openOutputStream(uri);
                        if (os == null) throw new Exception("openOutputStream failed");
                        try { os.write(bytes); } finally { os.close(); }
                        values.clear();
                        values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                        cr.update(uri, values, null, null);
                        saved = uri.toString();
                    } else {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            call.reject("NEED_PERMISSION"); return;
                        }
                        File dir = new File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM);
                        if (!dir.exists()) dir.mkdirs();
                        File file = new File(dir, name);
                        FileOutputStream fos = new FileOutputStream(file);
                        try { fos.write(bytes); } finally { fos.close(); }
                        MediaScannerConnection.scanFile(ctx, new String[]{file.getAbsolutePath()},
                                new String[]{mime}, null);
                        saved = file.getAbsolutePath();
                    }

                    JSObject ret = new JSObject();
                    ret.put("ok", true);
                    ret.put("uri", saved == null ? "" : saved);
                    call.resolve(ret);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    call.reject(msg == null ? "SAVE_FAILED" : msg);
                }
            }
        }).start();
    }
}
