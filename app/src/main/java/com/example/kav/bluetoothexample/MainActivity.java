package com.example.kav.bluetoothexample;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static final String ADDRESS_INTENT = "ADDRESS";
    static final String RSSI_INTENT = "RSSI";
    static final String INTENT_FILTER_RSSI = "INTENT_FILTER_RSSI";
    static final String PROGRESS_BAR_STATUS = "PROGRESS_BAR_STATUS";
    static final String POWER_COUNT = "POWER_COUNT";
    static int notificationID = 0;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 0;
    BluetoothAdapter bluetoothAdapter = null;
    final String UUID = "00001800-0000-1000-8000-00805f9b34fb";
    Button startScanButton = null;
    Button stopScanButton = null;
    TextView addressText = null;
    TextView rssiText = null;
    TextView distanceText = null;
    ProgressBar progressBar = null;
    private BroadcastReceiver broadcastReceiverForGetRSSI = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initializeViewComponents();
        checkBluetooth();
        if (checkPermissions()) return;
    }

    private void initializeViewComponents() {
        startScanButton = (Button) findViewById(R.id.buttonStartScan);
        stopScanButton = (Button) findViewById(R.id.buttonStopScan);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        rssiText = (TextView) findViewById(R.id.textRssi);
        addressText = (TextView) findViewById(R.id.textAddress);
        distanceText = (TextView) findViewById(R.id.textDistance);
    }

    private boolean checkPermissions() {
        int hasPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            initializeButtonClick(startScanButton, stopScanButton);
            return true;
        }
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_COARSE_LOCATION_PERMISSIONS);
        return false;
    }

    private void initializeButtonClick(Button startScanButton, Button stopScanButton) {
        if (startScanButton != null) {
            startScanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startScanService();
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
        }
        if (stopScanButton != null) {
            stopScanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopScanService();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION_PERMISSIONS: {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeButtonClick(startScanButton, stopScanButton);
                } else {
                    Toast.makeText(this, "Not acsess!",
                            Toast.LENGTH_LONG).show();
                    initializeButtonClick(startScanButton, stopScanButton);
                }
                return;
            }
        }
    }

    double getDistance(int rssi, int txPower) {
    /*
     * RSSI = TxPower - 10 * n * lg(d)
     * n = 2 (in free space)
     *
     * d = 10 ^ ((TxPower - RSSI) / (10 * n))
     */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    private void startScanService() {
        Intent intent = new Intent(this, ScanService.class);
        startService(intent);
        if (broadcastReceiverForGetRSSI == null) {
            registerBroadcastReceiver();
        }
    }

    private void stopScanService() {
        Intent intent = new Intent(this, ScanService.class);
        stopService(intent);
        if (broadcastReceiverForGetRSSI != null) {
            unregisterReceiver(broadcastReceiverForGetRSSI);
            broadcastReceiverForGetRSSI = null;
        }
    }

    protected static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine distance, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }

    private void registerBroadcastReceiver() {
        broadcastReceiverForGetRSSI = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("recieve", "here");
                int rssi = intent.getIntExtra(RSSI_INTENT, 0);
                String address = intent.getStringExtra(ADDRESS_INTENT);
                boolean progressBarStatus = intent.getBooleanExtra(PROGRESS_BAR_STATUS, true);
                int power = intent.getIntExtra(POWER_COUNT, 0);
                rssiText.setText(String.valueOf(rssi));
                addressText.setText(address);
                distanceText.setText(String.valueOf(getDistance(rssi, power)));
                if (!progressBarStatus) {
                    progressBar.setVisibility(View.GONE);
                    stopScanService();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FILTER_RSSI);
        registerReceiver(broadcastReceiverForGetRSSI, filter);

    }

    private void checkBluetooth() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Doesn't support BLE", Toast.LENGTH_SHORT).show();
        }
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


      /*  if (bluetoothAdapter != null) {
            Log.e("Address", bluetoothAdapter.getAddress());
            Log.e("Name", bluetoothAdapter.getName());
            bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.e("Device name", device.getName());
                    Log.e("Device adress", device.getAddress());
                    if (device.getAddress().equals("20:C3:8F:FF:54:BC")) {
                        iBeaconDevice = device;
                        bluetoothAdapter.stopLeScan(this);
                        bluetoothAdapter.cancelDiscovery();
                        bluetoothGatt = iBeaconDevice.connectGatt(getBaseContext(), false, new BluetoothGattCallback() {
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                Log.e("GATT", "On Connection");
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    Log.e("GATT", "Connected to GATT server.");
                                    gatt.discoverServices();

                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    Log.e("GATT", "Disconnected from GATT server.");
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                Log.e("GATT", "On Service Discovered");
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    Log.e("GATT", "Succes Service Discovered");
                                    for (int i = 0; i < gatt.getServices().size(); i++) {
                                        Log.e("UUID Type" + i, String.valueOf(gatt.getServices().get(i).getType()));
                                        Log.e("UUID" + i + " = ", gatt.getServices().get(i).getUuid().toString());
                                    }
                                    gatt.close();

                                }
                                //gatt.readCharacteristic();
                            }

                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                Log.e("GATT", "On Charachre Read7");
                                Log.e("Charach", characteristic.getStringValue(0));
                            }

                            @Override
                            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                Log.e("GATT", "On Charachre Read6");
                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                Log.e("GATT", "On Charachre Read5");
                            }

                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                Log.e("GATT", "On Charachre Read11111");
                            }

                            @Override
                            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                Log.e("GATT", "On Charachre Read1111");
                            }

                            @Override
                            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                                Log.e("GATT", "On Charachre Read111");
                            }

                            @Override
                            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    Log.e("GATT", "Succes Read Rssi");
                                    Log.e("GATT", "Rssi = " + rssi);
                                    //
                                }
                            }

                            @Override
                            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                                Log.e("GATT", "On Charachre Read1");
                            }
                        });
                        Log.e("Ibeacon Device name", iBeaconDevice.getName());
                        Log.e("Ibeacon Device adress", iBeaconDevice.getAddress());
                        arrayAdapter.add(device.getName());
                        arrayAdapter.notifyDataSetChanged();
                        bluetoothAdapter.stopLeScan(this);

                    }
                }


            });
        }
*/


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}
