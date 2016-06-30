package com.example.kav.bluetoothexample.UnlockService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ScanThreadJellyBean;
import com.example.kav.bluetoothexample.Service.ScanThreadM;

/**
 * Created by kav on 16/06/22.
 */
public class ScreenUnlock extends Service implements IUnlock {
    private String action = Intent.ACTION_USER_PRESENT;
    private BroadcastReceiver unlockReceiver = null;
    private ScreenUnlock screenUnlock = this;
    private String TAG = "SCREEN UNLOCK";
    private static String phoneNumber = "";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (unlockReceiver == null) {
            unlockReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        new ScanThreadM(getBaseContext(), screenUnlock, phoneNumber).start();
                    else
                        new ScanThreadJellyBean(getBaseContext(), screenUnlock, phoneNumber).start();
                }
            };
            registerReceiver(unlockReceiver, new IntentFilter(action));
            Log.e("SCREEN SERVICE", "START!!!");
        }
        return super.onStartCommand(intent, flags, startId);
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
    public void startChecking(Context context, String phoneNumber) {
        this.phoneNumber = phoneNumber;
        context.startService(new Intent(context, ScreenUnlock.class));
    }

    @Override
    public void stopChecking(Context context) {
        context.stopService(new Intent(context, ScreenUnlock.class));
    }
}
