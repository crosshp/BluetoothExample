package com.example.kav.bluetoothexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.kav.bluetoothexample.Service.ScanThread;

/**
 * Created by kav on 16/06/17.
 */
public class ScreenOnOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            new ScanThread(context).start();
        }
    }
}
