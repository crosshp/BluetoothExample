package com.example.kav.bluetoothexample.UnlockService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.OrientationEventListener;

import com.example.kav.bluetoothexample.Service.ScanThreadJellyBean;
import com.example.kav.bluetoothexample.Service.ScanThreadM;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kav on 16/06/09.
 */
public class AccelerometerUnlock extends Service implements IUnlock {
    private boolean isFirstWiggle = false;
    private long startTime = 0;
    private int startPoint = 0;
    private int deltaRotate = 90;
    private int delayTimeRotate = 750;
    private long firstTime = 0;
    long firstVibrateMilliseconds = 20;
    private final int dispersionOrientation = 15;
    private List<Integer> upPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> downPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> leftPosition = new ArrayList<>(dispersionOrientation);
    private List<Integer> rightPosition = new ArrayList<>(dispersionOrientation);
    private int[] positionValue = {0, 90, 180, 270};
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;
    private boolean isWakeLockAcquire = false;
    private int countOfWiggle = 0;
    private boolean isScanStart = false;
    private OrientationEventListener orientationEventListener = null;
    private String TAG = "GYRO SERVICE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "GYRO START!!!!   GYRO START!!!!   GYRO START!!!!");
        initDispersionArrays();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        orientationEventListener = new OrientationEventListener(getBaseContext(), SensorManager.SENSOR_DELAY_GAME) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (!isWakeLockAcquire) {
                    wakeLock.acquire();
                    isWakeLockAcquire = true;
                }
                orientation = getValueOfArea(orientation);
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
                                    unlockPhone();
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
            upPosition.add((352 + i) % 361);
            downPosition.add(172 + i);
            leftPosition.add(262 + i);
            rightPosition.add(82 + i);
        }
    }

    private int getValueOfArea(int orientation) {
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

    private void unlockPhone() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
        synchronized (this) {
            if (!isScanStart) {
                startScanService();
                isScanStart = true;
            }
        }
        wakeLock.release();
    }

    @Override
    public void onDestroy() {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
        Log.e(TAG, "GYRO DESTROY!!!!   GYRO DESTROY!!!!   GYRO DESTROY!!!!   GYRO DESTROY!!!!   GYRO DESTROY!!!!");
        upPosition = null;
        downPosition = null;
        leftPosition = null;
        rightPosition = null;
        super.onDestroy();
    }

    private void startScanService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            new ScanThreadM(getBaseContext(), this).start();
        else new ScanThreadJellyBean(getBaseContext(), this).start();

    }

    @Override
    public void startChecking(Context context) {
        context.startService(new Intent(context, AccelerometerUnlock.class));
    }

    @Override
    public void stopChecking(Context context) {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
        context.stopService(new Intent(context, AccelerometerUnlock.class));
    }
}