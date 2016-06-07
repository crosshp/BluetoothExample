package com.example.kav.bluetoothexample;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kav on 16/06/07.
 */
public class BluetoothScannerSingleton implements IBluetoothScanner {
    BluetoothLeScanner bluetoothLeScanner = null;
    final int distanceHigh = -55;
    final int delayToScanResult = 450;
    String beaconAddress = "20:C3:8F:FF:54:BC";
    BluetoothManager bluetoothManager = null;
    ScanCallback scanCallBack = null;
    private static volatile BluetoothScannerSingleton instance;

    public static BluetoothScannerSingleton getInstance() {
        BluetoothScannerSingleton localInstance = instance;
        if (localInstance == null) {
            synchronized (BluetoothScannerSingleton.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new BluetoothScannerSingleton();
                }
            }
        }
        return localInstance;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ScanCallback startScan(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        bluetoothLeScanner = bluetoothManager.getAdapter().getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        initializeCallBack();
        this.bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
        return scanCallBack;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopScan() {
        if (scanCallBack != null) {
            bluetoothLeScanner.stopScan(scanCallBack);
        }
        bluetoothManager = null;
        scanCallBack = null;
        bluetoothLeScanner = null;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initializeCallBack() {
        scanCallBack = new ScanCallback() {
            private boolean isFirstThread = true;
            Handler handler = new Handler();
            List<ScanResult> scanResults = new ArrayList<>();

            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                if (result.getDevice().getAddress().equals(beaconAddress))
                    scanResults.add(result);
                final Runnable delayScan = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            int sumRssi = 0;
                            for (ScanResult result : scanResults) {
                                sumRssi += result.getRssi();
                            }
                            int averageRssi = sumRssi / scanResults.size();
                            if (result.getDevice().getAddress().equals(beaconAddress) && (averageRssi > distanceHigh)) {
                                bluetoothLeScanner.stopScan(scanCallBack);
                                Log.e("ScanCallBack", "Device found!!!");
                            }
                            Log.e("ScanCallBack", "Average RSSI = " + String.valueOf(averageRssi)
                                    + "\nDevice address = " + result.getDevice().getAddress()
                                    + "\nDevice name = " + result.getDevice().getName());

                            isFirstThread = true;
                            handler.removeCallbacks(this);
                            scanResults.clear();
                        }
                    }
                };
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            if (isFirstThread) {
                                isFirstThread = false;
                                handler.postDelayed(delayScan, delayToScanResult);
                            }
                        }
                    }
                }).start();

                return;

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("ScanCallBack", "Batch");
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("ScanCallBack", "Failed");
            }
        };
    }
}