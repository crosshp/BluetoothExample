package com.example.kav.bluetoothexample.Service;

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
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.kav.bluetoothexample.Activity.MainActivity;
import com.example.kav.bluetoothexample.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by kav on 16/06/02.
 */
public class ScanThread extends Thread {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private ScanCallback scanCallBack = null;
    private int distanceHigh = -65;
    private final UUID SYSTEM_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private String[] systemUUIDs = new String[]{SYSTEM_UUID.toString()};
    private Map<String, List<Integer>> resultsRssiMap = new HashMap<>();
    private int rssiSwimWindow = 0;
    private int delayScan = 5000;
    private Context context = null;
    private boolean isFirst = true;
    private ScanThread currentThread = null;
    private String TAG = "SCAN THREAD";


    public ScanThread(Context context) {
        this.context = context;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void run() {
        Log.e(TAG, "START SCAN");
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void initCallBack() {
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
                        Log.e(TAG, "Device found!!!");
                        sendNotification(result);
                        synchronized (this) {
                            if (isFirst) {
                                new ConnectThread(context, result.getDevice()).start();
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
        String message = "Имя:" + result.getDevice().getName() +
                "\nАдресс:" + result.getDevice().getAddress() +
                "\nUUID:" + result.getScanRecord().getServiceUuids().get(0).getUuid().toString();
        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(context);
        notificationCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationCompatBuilder.setContentTitle("Найдено устройство");
        notificationCompatBuilder.setContentText(message);
        notificationCompatBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        NotificationManagerCompat.from(context).notify("Tag", MainActivity.notificationID, notificationCompatBuilder.build());
        Intent intent = new Intent(MainActivity.INTENT_FILTER_RSSI);
        intent.putExtra(MainActivity.ADDRESS_INTENT, result.getDevice().getAddress());
        intent.putExtra(MainActivity.RSSI_INTENT, (Integer) result.getRssi());
        intent.putExtra(MainActivity.POWER_COUNT, (Integer) result.getScanRecord().getTxPowerLevel());
        intent.putExtra(MainActivity.PROGRESS_BAR_STATUS, false);
        MainActivity.notificationID++;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onDestroy() {
        //
        //
        context.startService(new Intent(context, ListenGyroService.class));
        //
        //
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallBack);
            bluetoothLeScanner = null;
        }
        if (currentThread != null && !currentThread.isInterrupted()) {
            currentThread.interrupt();
            currentThread = null;
        }
        Log.e(TAG, "DESTROYED!");
    }
}