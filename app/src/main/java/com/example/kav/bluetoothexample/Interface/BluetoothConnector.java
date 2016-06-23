package com.example.kav.bluetoothexample.Interface;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Created by kav on 16/06/23.
 */
public class BluetoothConnector implements IBluetoothConnector {
    private static volatile BluetoothConnector instance;
    private final UUID SYSTEM_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final UUID MODULE_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device = null;
    private BluetoothGatt bluetoothGatt = null;
    private String TAG = "CONNECT SERVICE";
    private String message = "123451";
    private byte[] messageInByte = message.getBytes();
    private int delayConnect = 5000;
    private Context context = null;
    private Thread connectThread = null;
    private Observer observer = null;
    private boolean isObserverSuccess = false;


    public void setOnScanResultListener(Observer observer) {
        this.observer = observer;
    }


    public static BluetoothConnector getInstance() {
        BluetoothConnector localInstance = instance;
        if (localInstance == null) {
            synchronized (BluetoothConnector.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new BluetoothConnector();
                }
            }
        }
        return localInstance;
    }


    @Override
    public void connect(BluetoothDevice deviceIn, Context contextIn) {
        this.device = deviceIn;
        this.context = contextIn;
        if (connectThread == null) {
            connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(delayConnect);
                            } catch (InterruptedException e) {
                                onDestroy();
                            }
                            onDestroy();
                        }
                    }).start();

                    Log.e(TAG, device.getName());
                    bluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.e(TAG, "CONNECT!");
                                bluetoothGatt.discoverServices();

                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.e(TAG, "Disconnected from GATT server.");
                                onDestroy();
                            }
                            super.onConnectionStateChange(gatt, status, newState);
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(SYSTEM_UUID).getCharacteristic(MODULE_UUID);
                                if ((bluetoothGattCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) {
                                    bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                                    bluetoothGatt.connect();
                                    bluetoothGattCharacteristic.setValue(messageInByte);
                                    boolean isWrite = gatt.writeCharacteristic(bluetoothGattCharacteristic);
                                    if (!isWrite) {
                                        Log.e(TAG, "CANNOT WRITE!");
                                        onDestroy();
                                    } else {
                                        observer.onSuccess();
                                        isObserverSuccess = true;
                                    }

                                }
                            }
                            super.onServicesDiscovered(gatt, status);
                        }

                        @Override
                        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            Log.e(TAG, "CharacteristicRead");
                            super.onCharacteristicRead(gatt, characteristic, status);
                        }

                        @Override
                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            Log.e(TAG, "CharacteristicWrite");
                            super.onCharacteristicWrite(gatt, characteristic, status);
                        }
                    });
                }
            });
            connectThread.start();
        }
    }

    @Override
    public void disconnect() {
        onDestroy();
    }


    public void onDestroy() {
        if (observer != null && !isObserverSuccess)
            observer.onFail();
        observer = null;

        isObserverSuccess = false;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        Log.e(TAG, "DESTROYED!!!");

        if (connectThread != null && !connectThread.isInterrupted()) {
            connectThread.interrupt();
            connectThread = null;
        }
        device = null;
        context = null;
    }
}
