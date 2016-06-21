package com.example.kav.bluetoothexample.UnlockService;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ScanThread;

/**
 * Created by kav on 16/06/17.
 */
public class ScreenUnlock extends BroadcastReceiver implements IUnlock {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            Log.e("Receicer", "PHONE IS UNLOCKED!");
            new ScanThread(context, this).start();
        }
    }

    @Override
    public void startChecking(Context context) {
        context.registerReceiver(this, new IntentFilter(Intent.ACTION_USER_PRESENT));
    }

    @Override
    public void stopChecking(Context context) {
        context.unregisterReceiver(this);
    }
}
