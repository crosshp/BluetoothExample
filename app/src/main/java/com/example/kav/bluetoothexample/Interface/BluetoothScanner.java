package com.example.kav.bluetoothexample.Interface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.kav.bluetoothexample.Activity.MainActivity;
import com.example.kav.bluetoothexample.UnlockService.IUnlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by kav on 17/06/07.
 */
public class BluetoothScanner implements IBluetoothScanner {
    private static volatile BluetoothScanner instance;
    private Thread scanThread = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private ScanCallback scanCallBack = null;
    private int distanceHigh = -52;
    private final UUID SYSTEM_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private String[] systemUUIDs = new String[]{SYSTEM_UUID.toString()};
    private Map<String, List<Integer>> resultsRssiMap = new HashMap<>();
    private int rssiSwimWindow = 0;
    private int delayScan = 5000;
    private Context context = null;
    private boolean isFirst = true;
    private String TAG = "SCAN THREAD";
    private IUnlock unlockClient = null;
    private Observer observer = null;
    private BluetoothDevice foundDevice = null;
    private boolean isObserverSuccess = false;


    public BluetoothDevice getFoundDevice() {
        return foundDevice;
    }

    public void setOnScanResultListener(Observer observer) {
        this.observer = observer;
    }

    public static BluetoothScanner getInstance() {
        BluetoothScanner localInstance = instance;
        if (localInstance == null) {
            synchronized (BluetoothScanner.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new BluetoothScanner();
                }
            }
        }
        return localInstance;
    }

    @Override
    public void startScan(Context contextIn, IUnlock unlockClientIn) {
        this.context = contextIn;
        this.unlockClient = unlockClientIn;
        if (scanThread == null) {
            scanThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "START SCAN");
                    unlockClient.stopChecking(context);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(delayScan);
                            } catch (InterruptedException e) {
                                onDestroy();
                            }
                            onDestroy();
                        }
                    }).start();
                    initCallBack();
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    if (bluetoothLeScanner != null)
                        bluetoothLeScanner.stopScan(scanCallBack);
                    scanInHighMode(bluetoothLeScanner);
                }
            });
            scanThread.start();
        }
    }

    @Override
    public void stopScan() {
        onDestroy();
    }

    private void initCallBack() {
        scanCallBack = new ScanCallback() {
            List<Integer> swimWindow = null;

            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                if (hasSystemUUID(uuids)) {
                    Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                    if (!resultsRssiMap.containsKey(result.getDevice().getAddress())) {
                        swimWindow = new ArrayList<>();
                        resultsRssiMap.put(result.getDevice().getAddress(), swimWindow);
                    } else {
                        swimWindow = resultsRssiMap.get(result.getDevice().getAddress());
                    }
                    swimWindow.add(result.getRssi());
                    if (swimWindow.size() == 5) {
                        swimWindow.remove(0);
                    }
                    rssiSwimWindow = 0;
                    for (Integer rssi : swimWindow) {
                        rssiSwimWindow += rssi;
                    }
                    rssiSwimWindow /= swimWindow.size();

                    if (rssiSwimWindow > distanceHigh) {
                        Log.e(TAG, "DEVICE FOUND!!!");
                        synchronized (this) {
                            if (isFirst) {
                                if (observer != null) {
                                    foundDevice = result.getDevice();
                                    observer.onSuccess();
                                    isObserverSuccess = true;
                                }
                                isFirst = false;
                            }
                        }
                        onDestroy();
                    }
                    intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                    intent.putExtra(MainActivity.NAME_INTENT, result.getDevice().getName());
                    intent.putExtra(MainActivity.RSSI_INTENT, (Integer) rssiSwimWindow);
                    intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
                    context.sendBroadcast(intent);
                }
            }
        };
    }

    private void scanInHighMode(BluetoothLeScanner bluetoothLeScanner) {
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
    }

    private boolean hasSystemUUID(List<ParcelUuid> uuids) {
        for (ParcelUuid uuid : uuids) {
            for (String systemUUID : systemUUIDs) {
                if (uuid.getUuid().toString().equals(systemUUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void onDestroy() {
        isFirst = true;
        if (observer != null && !isObserverSuccess)
            observer.onFail();

        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallBack);
            bluetoothLeScanner = null;
        }
        if (scanThread != null && !scanThread.isInterrupted()) {
            scanThread.interrupt();
            scanThread = null;
        }
        observer = null;
        isObserverSuccess = false;
        unlockClient.startChecking(context);
        Log.e(TAG, "DESTROYED!");
    }
}