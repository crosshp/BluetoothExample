package com.example.kav.bluetoothexample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.OrientationEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kav on 16/06/09.
 */
public class ListenGyroService extends Service {
    private boolean isFirstWiggle = false;
    private long startTime = 0;
    private int startPoint = 0;
    private int deltaRotate = 90;
    private int delayTimeRotate = 870;
    private long firstTime = 0;
    long firstVibrateMilliseconds = 20;
    private final int dispersionOrientation = 16;

    public static String GYRO_WAKE_UP_ACTION = "GYRO_WAKE_UP_ACTION";
    private List<Integer> upPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> downPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> leftPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> rightPosition = new ArrayList<>(dispersionOrientation);
    private int[] positionValue = {0, 90, 180, 270};
    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;
    boolean isWakeLockAcquire = false;
    private int countOfWiggle = 0;
    private OrientationEventListener orientationEventListener = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDispersionArrays();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        orientationEventListener = new OrientationEventListener(getBaseContext(), SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (!isWakeLockAcquire) {
                    wakeLock.acquire();
                    isWakeLockAcquire = true;
                }
                orientation = getStandartValueOfPosition(orientation);
                checkWiggle(orientation);
            }
        };
        orientationEventListener.enable();
    }

    private void checkWiggle(int orientation) {
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!isFirstWiggle) {
            if (orientation != -100) {
                startTime = System.currentTimeMillis();
                startPoint = orientation;
                isFirstWiggle = true;
            }
        } else {
            if (orientation == startPoint) {
                startTime = System.currentTimeMillis();
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime < delayTimeRotate) {
                if (orientation != -100) {
                    if (Math.abs(Math.abs(orientation) - Math.abs(startPoint)) == deltaRotate || Math.abs(Math.abs(orientation) - Math.abs(startPoint)) == 270) {
                        vibrator.vibrate(firstVibrateMilliseconds);
                        isFirstWiggle = false;
                        countOfWiggle++;
                        if (countOfWiggle == 1)
                            firstTime = System.currentTimeMillis();
                        if (countOfWiggle == 2) {
                            if (System.currentTimeMillis() - firstTime > delayTimeRotate) {
                                countOfWiggle = 1;
                                firstTime = System.currentTimeMillis();
                            } else {
                                if (System.currentTimeMillis() - firstTime < delayTimeRotate) {
                                    unlockAndStartHighScan();
                                    countOfWiggle = 0;
                                    if (isWakeLockAcquire) {
                                        wakeLock.release();
                                        isWakeLockAcquire = false;
                                    }
                                } else
                                    countOfWiggle = 0;
                            }
                        }
                    }
                }
            } else
                isFirstWiggle = false;
        }

    }

    private void initDispersionArrays() {
        for (int i = 0; i < dispersionOrientation; i++) {
            upPosition.add((355 + i) % 361);
            downPosition.add(171 + i);
            leftPosition.add(261 + i);
            rightPosition.add(81 + i);
        }
    }

    private int getStandartValueOfPosition(int orientation) {
        if (upPosition.contains(orientation))
            return positionValue[0];
        if (rightPosition.contains(orientation))
            return positionValue[1];
        if (downPosition.contains(orientation))
            return positionValue[2];
        if (leftPosition.contains(orientation))
            return positionValue[3];
        return -100;
    }

    private void unlockAndStartHighScan() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        Intent intent = new Intent(GYRO_WAKE_UP_ACTION);
        IntentFilter filter = new IntentFilter(GYRO_WAKE_UP_ACTION);
        filter.setPriority(1000);
        sendBroadcast(intent);
        wakeLock.release();
    }

    @Override
    public void onDestroy() {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
        super.onDestroy();
    }
}