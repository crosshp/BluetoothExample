package com.example.kav.bluetoothexample;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
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
    private int delayTimeRotate = 770;
    private int secondDelayTimeRotate = delayTimeRotate * 2 + 100;
    private boolean isSecondWiggle = false;
    public static String GYRO_WAKE_UP_ACTION = "GYRO_WAKE_UP_ACTION";

    private final int dispersionOrientation = 21;
    private List<Integer> upPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> downPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> leftPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> rightPosition = new ArrayList<>(dispersionOrientation);
    private int[] positionValue = {0, 90, 180, 270};


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initDispersionArrays();

        new OrientationEventListener(getBaseContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientation = getStandartValueOfPosition(orientation);
                Log.e("Orienatation", String.valueOf(orientation));
                if (!isFirstWiggle) {
                    if (orientation != -100) {
                        startTime = System.currentTimeMillis();
                        startPoint = orientation;
                        isFirstWiggle = true;
                    }
                } else if (!isSecondWiggle) {
                    if (orientation == startPoint) {
                        startTime = System.currentTimeMillis();
                        return;
                    }
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime < delayTimeRotate) {
                        if (Math.abs(orientation - startPoint) == deltaRotate || Math.abs(orientation - startPoint) == 270) {
                            Log.e("Gyroscope", "FIRST FIRST FIRST!!!");
                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            long milliseconds = 20;
                            v.vibrate(milliseconds);
                            isSecondWiggle = true;
                        }
                    } else
                        isFirstWiggle = false;
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime < secondDelayTimeRotate) {
                        if (Math.abs(orientation - startPoint) == 0) {
                            Log.e("Gyroscope", "UNLOCK!!!");
                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            long milliseconds = 20;
                            v.vibrate(milliseconds);
                            isSecondWiggle = false;
                            isFirstWiggle = false;
                            unlockAndStartHighScan();
                        }
                    } else {
                        isFirstWiggle = false;
                        isSecondWiggle = false;
                    }
                }
            }
        }.enable();
    }

    private void initDispersionArrays() {
        for (int i = 0; i < dispersionOrientation; i++) {
            upPosition.add((351 + i) % 361);
            downPosition.add(170 + i);
            leftPosition.add(260 + i);
            rightPosition.add(80 + i);
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
        sendBroadcast(intent);
        wakeLock.release();
    }
}