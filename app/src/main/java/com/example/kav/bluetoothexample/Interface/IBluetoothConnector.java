package com.example.kav.bluetoothexample.Interface;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by kav on 16/06/23.
 */
public interface IBluetoothConnector {
    public void connect(BluetoothDevice device, Context context);

    public void disconnect();
}
