package com.example.kav.bluetoothexample;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kav on 16/06/02.
 */
public class ScanService extends Service {
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;
    ScanCallback scanCallBack = null;
    int distanceHigh = -65;
    String UUID_1 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String UUID_2 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String UUID_3 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String[] systemUUIDs = new String[]{UUID_1, UUID_2, UUID_3};
    BroadcastReceiver gyroScopeReceiver = null;
    Map<String, List<Integer>> resultsRssiMap = new HashMap<>();
    int rssiSwimWindow = 0;
    int delayScan = 5000;

    public void registerScreenReceiver() {
        if (gyroScopeReceiver == null) {
            gyroScopeReceiver = new BroadcastReceiver() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (bluetoothLeScanner != null)
                        bluetoothLeScanner.stopScan(scanCallBack);
                    scanInHighMode(bluetoothLeScanner);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onDestroy();
                        }
                    }, delayScan);
                }
            };
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ListenGyroService.GYRO_WAKE_UP_ACTION);
        registerReceiver(gyroScopeReceiver, intentFilter);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            distanceHigh = intent.getIntExtra(MainActivity.RSSI_DISTANCE, 55);
            distanceHigh *= -1;
        }
        initCallBack();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        registerScreenReceiver();
        return super.onStartCommand(intent, flags, startId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void initCallBack() {
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
                            Log.e("ScanCallBack", "Device found!!!");
                            sendNotification(result);
                            onDestroy();
                        }
                        intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                        intent.putExtra(MainActivity.NAME_INTENT, result.getDevice().getName());
                        intent.putExtra(MainActivity.RSSI_INTENT, (Integer) rssiSwimWindow);
                        intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
                        sendBroadcast(intent);

                        Log.e("ScanCallBack", "Average RSSI = " + String.valueOf(rssiSwimWindow)
                                + "\nDevice address = " + result.getDevice().getAddress()
                                + "\nDevice name = " + result.getDevice().getName());
                    }
                }
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendNotification(ScanResult result) {
        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getBaseContext());
        notificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationCompatBuilder.setContentTitle("Title");
        notificationCompatBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationCompatBuilder.setContentInfo(result.getDevice().getAddress());
        NotificationManagerCompat.from(getBaseContext()).notify("Tag", MainActivity.notificationID, notificationCompatBuilder.build());
        Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
        intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
        intent.putExtra(MainActivity.RSSI_INTENT, (Integer) result.getRssi());
        intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
        intent.putExtra(MainActivity.PROGRESS_BAR_STATUS, false);
        MainActivity.notificationID++;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        Log.e("Destroy", "Here");
        Intent intentDestroy = new Intent(MainActivity.INTENT_FILTER_RSSI);
        intentDestroy.putExtra(MainActivity.PROGRESS_BAR_STATUS, false);
        sendBroadcast(intentDestroy);
        if (bluetoothLeScanner != null) {
            Log.e("Scanner", bluetoothLeScanner.toString());
            bluetoothLeScanner.stopScan(scanCallBack);
            bluetoothLeScanner = null;
        }
        if (gyroScopeReceiver != null) {
            unregisterReceiver(gyroScopeReceiver);
            gyroScopeReceiver = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
