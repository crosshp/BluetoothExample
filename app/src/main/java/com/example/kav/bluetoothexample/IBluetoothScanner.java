package com.example.kav.bluetoothexample;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;

/**
 * Created by kav on 16/06/07.
 */
public interface IBluetoothScanner {
    ScanCallback startScan(BluetoothManager bluetoothManager);
    void stopScan();
}
