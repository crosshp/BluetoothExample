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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kav on 16/06/02.
 */
public class ScanService extends Service {
    BluetoothGatt bluetoothGatt = null;
    BluetoothAdapter bluetoothAdapter = null;
    BluetoothLeScanner bluetoothLeScanner = null;
    ScanCallback scanCallBack = null;
    final int distance = -55;
    String beaconAddress = "20:C3:8F:FF:54:BC";



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startScan();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScan() {
        Log.e("Service", "Scan");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                intent.putExtra(MainActivity.RSSI_INTENT, result.getRssi());
                sendBroadcast(intent);
                Log.e("Scan", "Result");
                Log.e("Scan result", result.getDevice().getAddress() + "\nrssi = " + result.getRssi());
                if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() > distance)) {
                    sendNotification(result);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("Scan", "Batch");
                for(ScanResult scanResult : results){
                    Log.e("Batch",scanResult.getDevice().getAddress());
                    Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                    intent.putExtra(MainActivity.ADDRESS_INTENT, scanResult.getDevice().getAddress());
                    intent.putExtra(MainActivity.RSSI_INTENT, scanResult.getRssi());
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan", "Failed");
            }
        };
        bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void sendNotification(ScanResult result) {
        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(getBaseContext());
        notificationCompatBuilder.setSmallIcon(android.R.mipmap.sym_def_app_icon);
        notificationCompatBuilder.setContentTitle("Title");
        notificationCompatBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationCompatBuilder.setContentInfo(result.getDevice().getAddress());
        NotificationManagerCompat.from(getBaseContext()).notify("Tag", MainActivity.notificationID, notificationCompatBuilder.build());
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
}
