package com.example.kav.bluetoothexample.Interface;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.Context;

/**
 * Created by kav on 16/06/07.
 */
public interface IBluetoothScanner {
    ScanCallback startScan(Context context);
    void stopScan(Context context);
}
