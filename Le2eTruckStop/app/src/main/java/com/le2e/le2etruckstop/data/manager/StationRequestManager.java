package com.le2e.le2etruckstop.data.manager;


import android.os.Handler;

import com.le2e.le2etruckstop.ui.home.interfaces.StationRequestImpl;

import timber.log.Timber;

class StationRequestManager {
    private StationRequestImpl presenterImpl;
    private Handler delayedRequestHandler;
    private String runRad;
    private double runLat;
    private double runLng;
    private boolean saveMarkers;

    StationRequestManager(StationRequestImpl presenterImpl) {
        this.presenterImpl = presenterImpl;
        delayedRequestHandler = new Handler();
    }

    private Runnable stationRequestRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("api request runnable go!");
            presenterImpl.getStationsByLoc(runRad, runLat, runLng);
        }
    };

    void manageStationRequestRunnable(int delay, String radius, double lat, double lng, boolean save) {
        Timber.d("api request runnable started! w/ delay: %s", delay);
        delayedRequestHandler.removeCallbacks(stationRequestRunnable);
        saveMarkers = save;
        runRad = radius;
        runLat = lat;
        runLng = lng;
        delayedRequestHandler.postDelayed(stationRequestRunnable, delay);
    }

    void stopRequestRunnable() {
        delayedRequestHandler.removeCallbacks(stationRequestRunnable);
    }

    boolean isSaveMarkers() {
        return saveMarkers;
    }
}
