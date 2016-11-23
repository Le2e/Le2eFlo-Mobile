package com.le2e.le2etruckstop.ui.home_screen;


import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.manager.StationRequestManager;
import com.le2e.le2etruckstop.data.manager.TrackingModeManager;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;
import com.le2e.le2etruckstop.ui.home_screen.impl.StationRequestImpl;
import com.le2e.le2etruckstop.ui.home_screen.impl.TrackingImpl;

import java.util.ArrayList;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> implements TrackingImpl, StationRequestImpl {
    private DataManager dataManager;
    private Subscription truckStationSub;
    private TrackingModeManager trackingManager;
    private StationRequestManager stationRequestManager;

    MapsHomePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
        trackingManager = new TrackingModeManager(this);
        stationRequestManager = new StationRequestManager(this);
    }

    @Override
    public void detachView() {
        if (truckStationSub != null && !truckStationSub.isUnsubscribed())
            truckStationSub.unsubscribe();

        super.detachView();
    }

    // **********************************************************************************
    // **************************** STATION REQUEST METHODS *****************************
    // **********************************************************************************

    void delayedStationRequest(String radius, double lat, double lng, boolean saveOldMarkers) {
        stationRequestManager.manageStationRequestRunnable(radius, lat, lng, saveOldMarkers);
    }

    @Override
    public void getStationsByLoc(String radius, double lat, double lng) {
        getTruckStationsByLocation(radius, lat, lng);
    }

    // **********************************************************************************
    // ***************************** TRACKING MODE METHODS ******************************
    // **********************************************************************************

    void determineUserInteraction(int delay) {
        trackingManager.manageTrackingRunnable(delay);
    }

    void killTrackingMode() {
        trackingManager.stopTrackingMode();
    }

    @Override
    public void turnTackingBackOn() {
        if (getView() != null) {
            getView().turnTrackingOn();
        }
    }

    // **********************************************************************************
    // ****************************** SUBSCRIPTION METHODS ******************************
    // **********************************************************************************

    private void getTruckStationsByLocation(String radius, double lat, double lng) {
        truckStationSub = dataManager.getStationsFromApi(radius, lat, lng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StationsResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "Error getting stations.");

                        if (isViewAttached()) {
                            getView().onError(e);
                        }
                    }

                    @Override
                    public void onNext(StationsResponse stationsResponse) {
                        stationsRetrieved(stationsResponse.getTruckStopList());
                        Timber.d("*** Size of response: %s ***", stationsResponse.getTruckStopList().size());
                    }
                });
    }

    private void stationsRetrieved(ArrayList<TruckStop> list) {
        if (isViewAttached()) {
            if (getView() != null) {
                if (!stationRequestManager.isSaveMarkers()) {
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
}
