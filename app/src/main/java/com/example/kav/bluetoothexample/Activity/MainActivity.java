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
import android.support.design.widget.FloatingActionButton;
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
    private BluetoothAdapter bluetoothAdapter = null;
    private TextView addressText = null;
    private TextView rssiText = null;
    private TextView powerText = null;
    private FloatingActionButton buttonBigGraph = null;
    private BroadcastReceiver broadcastReceiverForGetRSSI = null;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private UnlockDetector unlockDetector = null;

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
        rssiText = (TextView) findViewById(R.id.textRssi);
        powerText = (TextView) findViewById(R.id.textPower);
        addressText = (TextView) findViewById(R.id.textAddress);
        buttonBigGraph = (FloatingActionButton) findViewById(R.id.buttonBigGraph);
        buttonBigGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), GraphActivity.class);
                startActivity(intent);
            }
        });
    }

    private double getDistance(int rssi, int txPower) {
        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    private void startScanService() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (unlockDetector == null) {
            unlockDetector = new UnlockDetector();
        }
        registerReceiver(unlockDetector, intentFilter);

        Intent intentStartGyro = new Intent(this, ListenGyroService.class);
        startService(intentStartGyro);
    }

    private void registerBroadcastReceiver() {
        broadcastReceiverForGetRSSI = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rssi = intent.getIntExtra(RSSI_INTENT, 0);
                String address = intent.getStringExtra(ADDRESS_INTENT);
                int power = intent.getIntExtra(POWER_COUNT, 0);
                rssiText.setText(String.valueOf(rssi));
                addressText.setText(address);
                powerText.setText(String.valueOf(power));
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

    @Override
    protected void onDestroy() {
        if (broadcastReceiverForGetRSSI != null) {
            unregisterReceiver(broadcastReceiverForGetRSSI);
            broadcastReceiverForGetRSSI = null;
        }
        Intent intentStartGyro = new Intent(this, ListenGyroService.class);
        stopService(intentStartGyro);

        if (unlockDetector != null) {
            unregisterReceiver(unlockDetector);
            unlockDetector = null;
        }
        super.onDestroy();
    }

}
