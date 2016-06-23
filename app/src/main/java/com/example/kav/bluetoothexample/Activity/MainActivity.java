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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kav.bluetoothexample.R;
import com.example.kav.bluetoothexample.UnlockService.AccelerometerUnlock;
import com.example.kav.bluetoothexample.UnlockService.IUnlock;
import com.example.kav.bluetoothexample.UnlockService.ScreenUnlock;


public class MainActivity extends AppCompatActivity {
    public static final String ADDRESS_INTENT = "ADDRESS";
    public static final String NAME_INTENT = "NAME_INTENT";
    public static final String RSSI_INTENT = "RSSI";
    public static final String INTENT_FILTER_RSSI = "INTENT_FILTER_RSSI";
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
    private RadioGroup radioGroup = null;
    private IUnlock iUnlock = null;
    private ProgressBar progressBar = null;
    private CardView cardView = null;

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
        cardView = (CardView) findViewById(R.id.card1);
        buttonBigGraph = (FloatingActionButton) findViewById(R.id.buttonBigGraph);
        buttonBigGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(getBaseContext(), GraphActivity.class);
                startActivity(intent);*/
                Animation animRotateIn_icon = AnimationUtils.loadAnimation(getBaseContext(),
                        R.anim.anim_activity);

                buttonBigGraph.startAnimation(animRotateIn_icon);
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, final int checkedId) {
                progressBar.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(getBaseContext(), ScreenUnlock.class));
                        stopService(new Intent(getBaseContext(), AccelerometerUnlock.class));
                        if (checkedId == R.id.radioButtonScreen) {
                            iUnlock = new ScreenUnlock();
                            Log.e("Lock", "SCREEN");
                        } else {
                            iUnlock = new AccelerometerUnlock();
                            Log.e("Lock", "Accelerometer");
                        }
                        iUnlock.startChecking(getBaseContext());
                        progressBar.setVisibility(View.GONE);
                    }

                }, 7000);
            }
        });
    }


    private void startScanService() {
        iUnlock = new AccelerometerUnlock();
        iUnlock.startChecking(getBaseContext());
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
        iUnlock.stopChecking(getBaseContext());
        super.onDestroy();
    }


    public void test() {
    }
}
