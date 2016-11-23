package com.le2e.le2etruckstop.data.manager;


import android.os.Handler;

import com.le2e.le2etruckstop.ui.home_screen.impl.StationRequestImpl;

import timber.log.Timber;

public class StationRequestManager {
    private StationRequestImpl presnterImpl;
    private Handler delayedRequestHandler;
    private String runRad;
    private double runLat;
    private double runLng;
    private boolean saveMarkers;
    private final int RUNNABLE_DELAY = 1000;

    public StationRequestManager(StationRequestImpl presnterImpl) {
        this.presnterImpl = presnterImpl;
        delayedRequestHandler = new Handler();
    }

    private Runnable stationRequestRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("api request runnable go!");
            presnterImpl.getStationsByLoc(runRad, runLat, runLng);
        }
    };

    public void manageStationRequestRunnable(String radius, double lat, double lng, boolean save) {
        Timber.d("api request runnable started!");
        delayedRequestHandler.removeCallbacks(stationRequestRunnable);
        saveMarkers = save;
        runRad = radius;
        runLat = lat;
        runLng = lng;
        delayedRequestHandler.postDelayed(stationRequestRunnable, RUNNABLE_DELAY);
    }

    public boolean isSaveMarkers() {
        return saveMarkers;
    }
}
