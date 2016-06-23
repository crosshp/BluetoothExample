package com.example.kav.bluetoothexample.Interface;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.Context;

import com.example.kav.bluetoothexample.UnlockService.IUnlock;

/**
 * Created by kav on 16/06/07.
 */
public interface IBluetoothScanner {
    void startScan(Context context, IUnlock unlockClient);

    void stopScan();
}
