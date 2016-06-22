package com.example.kav.bluetoothexample.Interface;

import android.annotation.TargetApi;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ConnectThread;
import com.example.kav.bluetoothexample.UnlockService.AccelerometerUnlock;
import com.example.kav.bluetoothexample.Activity.MainActivity;
import com.example.kav.bluetoothexample.R;
import com.example.kav.bluetoothexample.Service.ScanThread;
import com.example.kav.bluetoothexample.UnlockService.IUnlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by kav on 16/06/07.
 */
public class BluetoothScannerThread implements IBluetoothScanner {
    private static volatile BluetoothScannerThread instance;

    public static BluetoothScannerThread getInstance() {
        BluetoothScannerThread localInstance = instance;
        if (localInstance == null) {
            synchronized (BluetoothScannerThread.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new BluetoothScannerThread();
                }
            }
        }
        return localInstance;
    }

    @Override
    public void startScan(Context context, IUnlock unlockClient) {
        new ScanThread(context, unlockClient);
    }
}