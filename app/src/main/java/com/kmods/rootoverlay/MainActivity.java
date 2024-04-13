package com.kmods.rootoverlay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "RootOverlay";

    private ESPView overlayView;
    private AIDLConnection aidlConn;
    public static IDrawService aidlIPC;
    private static WindowManager windowManager;

    class AIDLConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "AIDL onServiceConnected");
            aidlConn = this;

            aidlIPC = IDrawService.Stub.asInterface(service);
            try {
                Log.d(TAG, "App PID : " + Process.myPid());
                Log.d(TAG, "AIDL PID : " + aidlIPC.getPid());
                Log.d(TAG, "App UID : " + Process.myUid());
                Log.d(TAG, "AIDL UID : " + aidlIPC.getUid());

                Point screenSize = new Point();
                Display display = windowManager.getDefaultDisplay();
                display.getRealSize(screenSize);
                //if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                //    // flip w and h if launched in portrait mode
                //    int invP1 = screenSize.y;
                //    int invP2 = screenSize.x;
                //    screenSize.set(invP1, invP2);
                //}

                aidlIPC.updateSurface(screenSize.x, screenSize.y);
            } catch (RemoteException e) {
                Log.e(TAG, "Remote error", e);
            }

            addOverlay();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            removeOverlay();
            Log.d(TAG, "AIDL onServiceDisconnected");
            aidlConn = null;
        }
    }

    static class ESPView extends SurfaceView implements SurfaceHolder.Callback {
        public ESPView(Context context) {
            super(context);
            setZOrderOnTop(true);
            //setBackgroundColor(Color.TRANSPARENT);
            SurfaceHolder holder = getHolder();
            holder.setFormat(PixelFormat.TRANSLUCENT);
            holder.addCallback(this);
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus();
            requestFocusFromTouch();
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            Log.d(TAG,"surfaceCreated: " + holder.getClass().getName());
            //Paint textPaint = new Paint();
            //textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            //textPaint.setTextAlign(Paint.Align.CENTER);
            //textPaint.setTextSize(28);
            //textPaint.setAntiAlias(true);
            //textPaint.setARGB(255, 255, 160, 0);
            //textPaint.setShadowLayer(7f, 0, 0, Color.BLACK);
            //
            //Canvas canvas = holder.lockCanvas();
            //canvas.drawText("PID", 190, 60, textPaint);
            //holder.unlockCanvasAndPost(canvas);
            try {
                aidlIPC.initSurface(holder.getSurface());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG,"surfaceChanged");
            try {
                aidlIPC.updateSurface(width, height);
                aidlIPC.setOrientation(windowManager.getDefaultDisplay().getRotation());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            Log.d(TAG,"surfaceDestroyed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("APKP", getApplicationInfo().publicSourceDir);

        getPermission();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        TextView tv = findViewById(R.id.textView);
        tv.setText("App PID: " + Process.myPid());

        Button mButton = findViewById(R.id.button);
        mButton.setOnClickListener(v -> {
            if(Shell.getShell().isRoot()){
                Intent intent = new Intent(this, AIDLService.class);
                RootService.bind(intent, new AIDLConnection());
            } else {
                Toast.makeText(this, "Root not Granted", Toast.LENGTH_SHORT).show();
            }
        });

        Button mButton2 = findViewById(R.id.button2);
        mButton2.setOnClickListener(v -> {
            if(Shell.getShell().isRoot()) {
                RootService.unbind(aidlConn);
            }
        });
    }

    private void addOverlay() {
        WindowManager.LayoutParams overlayParams = getOverlayParams();

        overlayParams.gravity = Gravity.START | Gravity.TOP;

        overlayParams.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        overlayParams.x = 0;
        overlayParams.y = 0;

        overlayParams.width  = WindowManager.LayoutParams.MATCH_PARENT;
        overlayParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            overlayParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        overlayView = new ESPView(this);
        windowManager.addView(overlayView, overlayParams);
    }

    private void removeOverlay(){
        windowManager.removeView(overlayView);
    }

    @NonNull
    private static WindowManager.LayoutParams getOverlayParams() {
        //int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
        //            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        //            WindowManager.LayoutParams.FLAG_FULLSCREEN |
        //            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
        //            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
        //            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
        //            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
        //            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_FULLSCREEN;

        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags, PixelFormat.TRANSLUCENT);
    }

    private void getPermission() {
        if (!Settings.canDrawOverlays(this)) {  //Android M Or Over
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
