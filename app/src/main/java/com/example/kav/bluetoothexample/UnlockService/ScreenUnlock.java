package com.example.kav.bluetoothexample.UnlockService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ScanThread;

/**
 * Created by kav on 16/06/22.
 */
public class ScreenUnlock extends Service implements IUnlock {
    private String action = Intent.ACTION_USER_PRESENT;
    private BroadcastReceiver unlockReceiver = null;
    private ScreenUnlock screenUnlock = this;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (unlockReceiver == null) {
            unlockReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    new ScanThread(getBaseContext(), screenUnlock).start();
                }
            };
            registerReceiver(unlockReceiver, new IntentFilter(action));
            Log.e("SCREEN SERVICE", "START!!!");
        }
    }

    @Override
    public void onDestroy() {
        if (unlockReceiver != null) {
            unregisterReceiver(unlockReceiver);
            unlockReceiver = null;
            Log.e("SCREEN SERVICE", "STOP!!!");
        }
        super.onDestroy();
    }

    @Override
    public void startChecking(Context context) {
        context.startService(new Intent(context, ScreenUnlock.class));
    }

    @Override
    public void stopChecking(Context context) {
        context.stopService(new Intent(context, ScreenUnlock.class));
    }
}
