package com.example.kav.bluetoothexample;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ScanThread;

/**
 * Created by kav on 16/06/17.
 */
public class UnlockDetector extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.e("Receicer", "PHONE IS UNLOCKED!");
            new ScanThread(context).start();
        }
    }
}
