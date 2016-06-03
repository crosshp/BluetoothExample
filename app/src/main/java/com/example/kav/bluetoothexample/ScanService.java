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
import android.widget.Toast;

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
    final int distanceHigh = -55;
    final int distanceMedium = -65;
    final int distanceLow = -80;
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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanInHighLevel(bluetoothLeScanner);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void scanInLowMode(final BluetoothLeScanner bluetoothLeScanner) {
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                intent.putExtra(MainActivity.RSSI_INTENT, result.getRssi());
                intent.putExtra(MainActivity.POWER_COUNT, result.getScanRecord().getTxPowerLevel());
                sendBroadcast(intent);
                Log.e("Scan", "Low");
                Log.e("Scan result", result.getDevice().getAddress() + "\nrssi = " + result.getRssi());
                if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() > distanceLow)) {
                    bluetoothLeScanner.stopScan(this);
                    scanInMediumLevel(bluetoothLeScanner);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("Scan", "Batch");
                for (ScanResult scanResult : results) {
                    Log.e("Batch", scanResult.getDevice().getAddress());
                }
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan", "Failed Low");
                Toast.makeText(getBaseContext(),"Error Scanning in Low Level",Toast.LENGTH_LONG).show();
            }
        };
        this.bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scanInMediumLevel(final BluetoothLeScanner bluetoothLeScanner) {
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                intent.putExtra(MainActivity.RSSI_INTENT, result.getRssi());
                intent.putExtra(MainActivity.POWER_COUNT, result.getScanRecord().getTxPowerLevel());
                sendBroadcast(intent);
                Log.e("Scan", "Medium");
                Log.e("Scan result", result.getDevice().getAddress() + "\nrssi = " + result.getRssi());
                if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() > distanceMedium)) {
                    bluetoothLeScanner.stopScan(this);
                    scanInHighLevel(bluetoothLeScanner);
                } else if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() < distanceMedium)) {
                    bluetoothLeScanner.stopScan(this);
                    scanInLowMode(bluetoothLeScanner);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("Scan", "Batch");
                for (ScanResult scanResult : results) {
                    Log.e("Batch", scanResult.getDevice().getAddress());
                }
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan", "Failed Medium");
                Toast.makeText(getBaseContext(),"Error Scanning in Medium Level",Toast.LENGTH_LONG).show();

            }
        };
        bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scanInHighLevel(final BluetoothLeScanner bluetoothLeScanner) {
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanCallBack = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
                intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
                intent.putExtra(MainActivity.RSSI_INTENT, result.getRssi());
                intent.putExtra(MainActivity.POWER_COUNT, result.getScanRecord().getTxPowerLevel());
                sendBroadcast(intent);
                Log.e("Scan", "High");
                Log.e("Scan result", result.getDevice().getAddress() + "\nrssi = " + result.getRssi());
                if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() > distanceHigh)) {
                    sendNotification(result);
                    bluetoothLeScanner.stopScan(this);
                } else {
                    if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() < distanceLow)) {
                        bluetoothLeScanner.stopScan(this);
                        scanInLowMode(bluetoothLeScanner);
                    } else if (result.getDevice().getAddress().equals(beaconAddress) && (result.getRssi() < distanceMedium)) {
                        bluetoothLeScanner.stopScan(this);
                        scanInMediumLevel(bluetoothLeScanner);
                    }
                }
            }
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.e("Scan", "Batch");
                for (ScanResult scanResult : results) {
                    Log.e("Batch", scanResult.getDevice().getAddress());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan", "Failed High");
                Toast.makeText(getBaseContext(),"Error Scanning in High Level",Toast.LENGTH_LONG).show();
            }
        };
        bluetoothLeScanner.startScan(new ArrayList<ScanFilter>(), scanSettings, scanCallBack);
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
        intent.putExtra(MainActivity.ADDRESS_INTENT, "0");
        intent.putExtra(MainActivity.RSSI_INTENT, 0);
        intent.putExtra(MainActivity.PROGRESS_BAR_STATUS, false);
        sendBroadcast(intent);
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
