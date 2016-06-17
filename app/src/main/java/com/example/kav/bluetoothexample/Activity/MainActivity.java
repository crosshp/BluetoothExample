package com.example.kav.bluetoothexample.Activity;

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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kav.bluetoothexample.UnlockDetector;
import com.example.kav.bluetoothexample.Service.ListenGyroService;
import com.example.kav.bluetoothexample.R;


public class MainActivity extends AppCompatActivity {
    public static final String ADDRESS_INTENT = "ADDRESS";
    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String RSSI_INTENT = "RSSI";
    public static final String INTENT_FILTER_RSSI = "INTENT_FILTER_RSSI";
    public static final String PROGRESS_BAR_STATUS = "PROGRESS_BAR_STATUS";
    public static final String POWER_COUNT = "POWER_COUNT";
    public static int notificationID = 0;
    private static final int REQUEST_ENABLE_BT = 0;
    BluetoothAdapter bluetoothAdapter = null;
    TextView addressText = null;
    TextView rssiText = null;
    TextView powerText = null;
    TextView distanceText = null;
    ProgressBar progressBar = null;
    Button buttonBigGraph = null;
    private BroadcastReceiver broadcastReceiverForGetRSSI = null;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    UnlockDetector unlockDetector = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initializeViewComponents();
        checkBluetooth();
        goToBluetoothRequestPermission();
        registerBroadcastReceiver();
    }

    private void goToBluetoothRequestPermission() {
        int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            startScanService();
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_BLUETOOTH_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startScanService();
        } else {
            Toast.makeText(this, "Not acsess!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViewComponents() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        rssiText = (TextView) findViewById(R.id.textRssi);
        powerText = (TextView) findViewById(R.id.textPower);
        addressText = (TextView) findViewById(R.id.textAddress);
        distanceText = (TextView) findViewById(R.id.textDistance);
        buttonBigGraph = (Button) findViewById(R.id.buttonBigGraph);
        buttonBigGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), GraphActivity.class);
                startActivity(intent);
            }
        });
    }

    double getDistance(int rssi, int txPower) {
        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    private void startScanService() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (unlockDetector == null) {
            unlockDetector = new UnlockDetector();
        }
        registerReceiver(unlockDetector, intentFilter);

        /*Intent intentStartGyro = new Intent(this, ListenGyroService.class);
        startService(intentStartGyro);*/
    }

    private void registerBroadcastReceiver() {
        broadcastReceiverForGetRSSI = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rssi = intent.getIntExtra(RSSI_INTENT, 0);
                String address = intent.getStringExtra(ADDRESS_INTENT);
                boolean progressBarStatus = intent.getBooleanExtra(PROGRESS_BAR_STATUS, true);
                int power = intent.getIntExtra(POWER_COUNT, 0);
                rssiText.setText(String.valueOf(rssi));
                addressText.setText(address);
                powerText.setText(String.valueOf(power));
                distanceText.setText(String.valueOf(getDistance(rssi, power)));
                if (!progressBarStatus) {
                    progressBar.setVisibility(View.GONE);
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
        if (broadcastReceiverForGetRSSI != null) {
            unregisterReceiver(broadcastReceiverForGetRSSI);
            broadcastReceiverForGetRSSI = null;
        }
        Intent intentStartGyro = new Intent(this, ListenGyroService.class);
        stopService(intentStartGyro);

        if(unlockDetector !=null){
            unregisterReceiver(unlockDetector);
            unlockDetector = null;
        }
        super.onDestroy();
    }

}
