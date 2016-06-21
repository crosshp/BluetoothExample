package com.example.kav.bluetoothexample.UnlockService;

import android.content.Context;

/**
 * Created by kav on 16/06/21.
 */
public interface IUnlock {
    void startChecking(Context context);
    void stopChecking(Context context);
}
