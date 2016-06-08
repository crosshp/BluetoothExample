package com.example.kav.bluetoothexample;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by kav on 16/06/02.
 */
public class ScanService extends Service {
    BluetoothGatt bluetoothGatt = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;
    ScanCallback scanCallBack = null;
    int distanceHigh = -69;
    final int delayToScanResult = 450;
    String beaconAddress = "20:C3:8F:FF:54:BC";
    String UUID_1 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String UUID_2 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String UUID_3 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    String[] systemUUIDs = new String[]{UUID_1, UUID_2, UUID_3};
    Intent intent = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        distanceHigh = intent.getIntExtra(MainActivity.RSSI_DISTANCE, 55);
        distanceHigh *= -1;
        startScan();


        return super.onStartCommand(intent, flags, startId);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScan() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanInLowMode(bluetoothLeScanner);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scanInLowMode(BluetoothLeScanner bluetoothLeScanner) {
        scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                int i = 0;
                List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
                for (ParcelUuid uuid : uuids) {
                    Log.e("UUID" + i, uuid.getUuid().toString());
                    i++;
                }
                if (hasSystemUUID(uuids)) {
                    Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                    if (result.getRssi() > distanceHigh) {
                        Log.e("ScanCallBack", "Device found!!!");
                        sendNotification(result);
                    }
                    intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                    intent.putExtra(MainActivity.RSSI_INTENT, (Integer) result.getRssi());
                    intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
                    sendBroadcast(intent);

                    Log.e("ScanCallBack", "Average RSSI = " + String.valueOf(result.getRssi())
                            + "\nDevice address = " + result.getDevice().getAddress()
                            + "\nDevice name = " + result.getDevice().getName());


                }
            }
        };
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
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


    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        Log.e("Destroy", "Here");
        if (bluetoothLeScanner != null) {
            Log.e("Scaner", bluetoothLeScanner.toString());
            bluetoothLeScanner.stopScan(scanCallBack);
            bluetoothLeScanner = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
