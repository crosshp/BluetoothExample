package com.example.kav.bluetoothexample.UnlockService;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ScanThread;

/**
 * Created by kav on 16/06/17.
 */
public class ScreenUnlock extends BroadcastReceiver implements IUnlock {
    SharedPreferences sharedPreferences = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            /*sharedPreferences = context.getSharedPreferences("Screen", Context.MODE_PRIVATE);
            if (!sharedPreferences.getBoolean("first", true)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("first", true);
                editor.commit();
                Log.e("Receicer", "PHONE IS UNLOCKED!");
                new ScanThread(context, this).start();
            }*/
        }
    }

    @Override
    public void startChecking(Context context) {
     /*   sharedPreferences = context.getSharedPreferences("Screen", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first", false);
        editor.commit();
        if (!sharedPreferences.getBoolean("isRegister", true)) {
            try {
                context.registerReceiver(this, new IntentFilter(Intent.ACTION_USER_PRESENT));
                editor.putBoolean("isRegister", true);
                editor.commit();
            } catch (java.lang.Exception e) {
                Log.e("EXCEPTION!", "CANNOT REGISTER RECEIVER");
            }
        }*/

    }

    @Override
    public void stopChecking(Context context) {
     /*   sharedPreferences = context.getSharedPreferences("Screen", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first", true);
        editor.commit();
        try {
            if (sharedPreferences.getBoolean("isRegister", false)) {
                context.unregisterReceiver(this);
                editor.putBoolean("isRegister", false);
                editor.commit();
            }
        } catch (java.lang.IllegalArgumentException e) {
            Log.e("EXCEPTION!", "CANNOT UNREGISTER RECEIVER");
        }
*/
    }
}
