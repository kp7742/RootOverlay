package com.kmods.rootoverlay;

import static com.kmods.rootoverlay.MainActivity.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

import java.lang.reflect.Method;

public class AIDLService extends RootService {
    static Paint circlePaint, textPaint;
    static int screenWidth, screenHeight;

    static class DrawIPC extends IDrawService.Stub {
        @Override
        public int getPid() {
            return Process.myPid();
        }

        @Override
        public int getUid() {
            return Process.myUid();
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void initSurface(Surface surface) throws RemoteException {
            Log.d(TAG, "AIDLService: initSurface");
            Canvas canvas = surface.lockHardwareCanvas();
            canvas.drawCircle(screenWidth / 2f, screenHeight / 2f, 220, circlePaint);
            canvas.drawText("PID: " + Process.myPid(), screenWidth / 2f, screenHeight / 2f, textPaint);
            surface.unlockCanvasAndPost(canvas);
        }

        @Override
        public void updateSurface(int width, int height) throws RemoteException {
            Log.d(TAG, "AIDLService: updateSurface");
            screenWidth = width;
            screenHeight = height;
        }

        @Override
        public void setOrientation(int orientation) throws RemoteException {
            Log.d(TAG, "AIDLService: setOrientation");
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "AIDLService: onCreate");

        // A12+ Fonts needs to preload for TypeFace generation
        try {
            Method preloadFont = Typeface.class.getMethod("loadPreinstalledSystemFontMap");
            preloadFont.invoke(null);
        } catch (Exception e) {
            Log.d(TAG, "AIDLService: onCreate | Err: " + e.getMessage());
        }

        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(3);
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.RED);

        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(7f, 0, 0, Color.BLACK);
    }

    @Override
    public void onRebind(@NonNull Intent intent) {
        // This callback will be called when we are reusing a previously started root process
        Log.d(TAG, "AIDLService: onRebind, daemon process reused");
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.d(TAG, "AIDLService: onBind");
        return new DrawIPC();
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        Log.d(TAG, "AIDLService: onUnbind, client process unbound");
        // Return true here so onRebind will be called
        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AIDLService: onDestroy");
    }
}
