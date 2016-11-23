package com.le2e.le2etruckstop.ui.home_screen;


import android.os.Handler;
import android.view.MotionEvent;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.model.TruckStopUtils;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;

import java.util.ArrayList;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> {
    private DataManager dataManager;
    private Subscription truckStationSub;
    private Handler delayedRequestHandler;
    private Handler userInteractionHandler;
    private final double EQUATOR_LENGTH = 24874;
    private final double EQUATOR_PIXEL_VALUE = 256;
    private final int RUNNABLE_DELAY = 1000;

    private String runRad;
    private double runLat;
    private double runLng;
    private boolean saveMarkers = false;

    public MapsHomePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
        delayedRequestHandler = new Handler();
        userInteractionHandler = new Handler();
    }

    private void stationsRetrieved(ArrayList<TruckStop> list) {
        if (isViewAttached()) {
            if (getView() != null) {
                if(!saveMarkers) {
                    Timber.d("clearing markers");
                    getView().clearMarkers();
                } else {
                    Timber.d("adding markers to existing map");
                }

                double lat;
                double lng;
                MarkerOptions options = new MarkerOptions();

                for (TruckStop truckStop : list) {
                    lat = Double.parseDouble(truckStop.getLat());
                    lng = Double.parseDouble(truckStop.getLng());
                    options.position(new LatLng(lat, lng));
                    options.title(truckStop.getName());
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_mall_directory_black_36dp));

                    getView().addTruckStopToMap(options, truckStop);
                }
            }
        }
    }

    private Runnable stationRequestRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("api request runnable go!");
            getTruckStationsByLocation(runRad, runLat, runLng, saveMarkers);
        }
    };

    void delayedStationRequest(String radius, double lat, double lng, boolean saveOldMarkers){
        Timber.d("api request runnable started!");
        saveMarkers = saveOldMarkers;
        runRad = radius;
        runLat = lat;
        runLng = lng;
        delayedRequestHandler.removeCallbacks(stationRequestRunnable);
        delayedRequestHandler.postDelayed(stationRequestRunnable, RUNNABLE_DELAY);
    }

    private String formatRawLine(String l1, String l2, String l3) {
        return TruckStopUtils.formatTruckStopRawLine(l1, l2, l3);
    }

    // extract into separate method and TDD
    public int calculateZoomLevel(double radius, int screenWidth) {
        double diameter = radius * 2;
        double milesPerPixel = EQUATOR_LENGTH / EQUATOR_PIXEL_VALUE;
        int zoomlvl = 1;
        while ((milesPerPixel * screenWidth) > diameter) {
            milesPerPixel /= 2;
            ++zoomlvl;
        }
        return zoomlvl;
    }

    // extract into separate method and TDD
    public int calculateRadius(int screenWidth, int zoomLevel) {
        int calcZoom = 1;
        if (zoomLevel == calcZoom) {
            return (int) EQUATOR_LENGTH / 2;
        }

        double milesPerPixel = EQUATOR_LENGTH / EQUATOR_PIXEL_VALUE;

        while (calcZoom != zoomLevel) {
            milesPerPixel /= 2;
            calcZoom++;
        }

        double diameter = milesPerPixel * screenWidth;

        return (int) diameter / 2;
    }

    private Runnable userInteractionRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("user interaction runnable go!");
            if(getView() != null){
                getView().turnTrackingOn();
            }
        }
    };

    void determineUserInteraction(int delay){
        Timber.d("user interaction runnable started!");
        userInteractionHandler.removeCallbacks(userInteractionRunnable);
        userInteractionHandler.postDelayed(userInteractionRunnable, delay);
    }

    void killTrackingMode(){
        userInteractionHandler.removeCallbacks(userInteractionRunnable);
    }

    // **********************************************************************************
    // ****************************** SUBSCRIPTION METHODS ******************************
    // **********************************************************************************

    public void getTruckStationsByLocation(String radius, double lat, double lng, final boolean save) {
        truckStationSub = dataManager.getStationsFromApi(radius, lat, lng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StationsResponse>() {
                    @Override
                    public void onCompleted() {
                        saveMarkers = save;
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(StationsResponse stationsResponse) {
                        stationsRetrieved(stationsResponse.getTruckStopList());
                        Timber.d("*** Size of response: %s ***", stationsResponse.getTruckStopList().size());
                    }
                });
    }

    void unSubscribeObserves() {
        truckStationSub.unsubscribe();
    }
}
