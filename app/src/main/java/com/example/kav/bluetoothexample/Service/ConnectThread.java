package com.example.kav.bluetoothexample.Service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by kav on 16/06/17.
 */
public class ConnectThread extends Thread {
    private final UUID SYSTEM_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final UUID MODULE_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device = null;
    private BluetoothGatt bluetoothGatt = null;
    private String TAG = "CONNECT SERVICE";
    private String message = "";
    private int delayConnect = 15000;
    private final ByteQueue queue = new ByteQueue(1024);
    private volatile boolean sending;
    private final byte[] buffer = new byte[20];
    private Context context = null;
    private ConnectThread currentThread = this;
    BluetoothGattCharacteristic bluetoothGattCharacteristic = null;

    public ConnectThread(Context context, BluetoothDevice device, String phoneNumber) {
        this.context = context;
        this.device = device;
       // message = "Demo" + phoneNumber + '\r';
        message = "123456";
    }

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
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        if (bluetoothGattCharacteristic == null)
                            gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.e(TAG, "Disconnected from GATT server.");
                        onDestroy();
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.e("SERVICE","SUCCESS DISCOVER");
                    bluetoothGattCharacteristic = bluetoothGatt.getService(SYSTEM_UUID).getCharacteristic(MODULE_UUID);
                    if ((bluetoothGattCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) {
                        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                        bluetoothGatt.connect();
                        boolean result = queue.write(message.getBytes());
                        if (result)
                            process(bluetoothGattCharacteristic, bluetoothGatt);
                    }
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.e(TAG, "CharacteristicRead");
            }

            private void process(BluetoothGattCharacteristic gattPort, BluetoothGatt gatt) {
                if (!sending) {
                    int size = queue.read(buffer);
                    if (size > 0) {
                        sending = true;
                        gattPort.setValue(size == buffer.length ? buffer : Arrays.copyOfRange(buffer, 0, size));
                        gatt.writeCharacteristic(gattPort);
                        Log.v("Channel", "sent, size: " + String.valueOf(size) + ": " + Hex.toString(Arrays.copyOfRange(buffer, 0, size)));
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                sending = false;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v("Channel", "written");
                    process(characteristic, gatt);
                } else {
                    queue.clear();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                if (data.length > 0) {
                    Log.v("Channel", "receive, size: " + String.valueOf(data.length) + ": " + Hex.toString(data));
                }
            }
        });
    }

    public void onDestroy() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        Log.e(TAG, "DESTROYED!!!");

        if (currentThread != null && !currentThread.isInterrupted()) {
            currentThread.interrupt();
            currentThread = null;
        }
        device = null;
        context = null;
    }
}
