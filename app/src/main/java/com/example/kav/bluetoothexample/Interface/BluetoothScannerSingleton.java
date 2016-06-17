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
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.kav.bluetoothexample.Service.ListenGyroService;
import com.example.kav.bluetoothexample.Activity.MainActivity;
import com.example.kav.bluetoothexample.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kav on 16/06/07.
 */
public class BluetoothScannerSingleton implements IBluetoothScanner {
    BluetoothLeScanner bluetoothLeScanner = null;
    int distanceHigh = -65;
    String SYSTEM_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String[] systemUUIDs = new String[]{SYSTEM_UUID};
    Map<String, List<Integer>> resultsRssiMap = new HashMap<>();
    int rssiSwimWindow = 0;
    int delayScan = 5000;
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
    public ScanCallback startScan(final Context context) {
        if (scanCallBack == null)
            initializeCallBack(context);
        if (bluetoothLeScanner != null)
            bluetoothLeScanner.stopScan(scanCallBack);
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan(context);
            }
        }, delayScan);
        return scanCallBack;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopScan(Context context) {
        Log.e("Destroy", "Destroy Scan Service!");
        context.startService(new Intent(context, ListenGyroService.class));
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallBack);
            bluetoothLeScanner = null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initializeCallBack(final Context context) {
        scanCallBack = new ScanCallback() {
            List<Integer> swimWindow = null;

            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                int i = 0;
                List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                if (uuids != null) {
                    for (ParcelUuid uuid : uuids) {
                        Log.e("UUID" + i, uuid.getUuid().toString());
                        i++;
                    }
                    if (hasSystemUUID(uuids)) {
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
                            Log.e("ScanCallBack", "Device found!!!");
                            sendNotification(result, context);
                            stopScan(context);
                            //
                            ///
                            ///
                            //
                         //       connect2Beacon(result);
                        }

                        Log.e("ScanCallBack", "Average RSSI = " + String.valueOf(rssiSwimWindow)
                                + "\nDevice address = " + result.getDevice().getAddress()
                                + "\nDevice name = " + result.getDevice().getName());
                    }
                }
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendNotification(ScanResult result, Context context) {
        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(context);
        notificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationCompatBuilder.setContentTitle("Title");
        notificationCompatBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationCompatBuilder.setContentInfo(result.getDevice().getAddress());
        NotificationManagerCompat.from(context).notify("Tag", MainActivity.notificationID, notificationCompatBuilder.build());
        Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
        intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
        intent.putExtra(MainActivity.RSSI_INTENT, (Integer) result.getRssi());
        intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
        intent.putExtra(MainActivity.PROGRESS_BAR_STATUS, false);
        MainActivity.notificationID++;
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


}