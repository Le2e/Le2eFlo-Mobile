package com.le2e.le2etruckstop.data.manager;


import android.os.Handler;

import com.le2e.le2etruckstop.ui.home.interfaces.TrackingImpl;

import timber.log.Timber;

public class TrackingModeManager {
    private TrackingImpl presenter;
    private Handler userInteractionHandler;

    public TrackingModeManager(TrackingImpl presenter) {
        userInteractionHandler = new Handler();
        this.presenter = presenter;
    }

    private Runnable userInteractionRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("user interaction runnable go!");
            presenter.turnTackingBackOn();
        }
    };

    public void manageTrackingRunnable(int delay) {
        userInteractionHandler.removeCallbacks(userInteractionRunnable);
        userInteractionHandler.postDelayed(userInteractionRunnable, delay);
    }

    public void stopTrackingMode() {
        userInteractionHandler.removeCallbacks(userInteractionRunnable);
    }
}
