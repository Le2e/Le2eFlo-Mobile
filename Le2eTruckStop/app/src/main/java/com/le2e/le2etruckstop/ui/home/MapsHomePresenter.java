package com.le2e.le2etruckstop.ui.home;


import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.manager.StationMapManager;
import com.le2e.le2etruckstop.data.manager.StationRequestManager;
import com.le2e.le2etruckstop.data.manager.StationSearchManager;
import com.le2e.le2etruckstop.data.manager.TrackingModeManager;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;
import com.le2e.le2etruckstop.ui.home.impl.PopupInfoImpl;
import com.le2e.le2etruckstop.ui.home.impl.SearchImpl;
import com.le2e.le2etruckstop.ui.home.impl.StationRequestImpl;
import com.le2e.le2etruckstop.ui.home.impl.TrackingImpl;

import java.util.ArrayList;
import java.util.HashMap;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> implements TrackingImpl, StationRequestImpl, PopupInfoImpl, SearchImpl {
    private DataManager dataManager;
    private StationSearchManager searchManager;
    private StationMapManager mapManager;
    private TrackingModeManager trackingManager;
    private StationRequestManager stationRequestManager;

    private Subscription truckStationSub;
    private Subscription mapTypeSub;
    private Subscription trackingStateSub;

    MapsHomePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
        mapManager = new StationMapManager();
        searchManager = new StationSearchManager(this);
        trackingManager = new TrackingModeManager(this);
        stationRequestManager = new StationRequestManager(this);
    }

    @Override
    public void detachView() {
        if (truckStationSub != null && !truckStationSub.isUnsubscribed())
            truckStationSub.unsubscribe();

        if(mapTypeSub != null && !mapTypeSub.isUnsubscribed())
            mapTypeSub.unsubscribe();

        super.detachView();
    }

    // **********************************************************************************
    // **************************** SEARCH MANAGER METHODS ******************************
    // **********************************************************************************

    void performSearch(String name, String city, String state, String zip) {
        searchManager.determineSearchParams(name, city, state, zip);
    }

    @Override
    public HashMap<Marker, TruckStop> getMarkerMap() {
        return mapManager.getMarkersMap();
    }

    // clear the map - put markers on map
    @Override
    public void deliverSearchResults(ArrayList<TruckStop> results) {
        Timber.d("Results found: %s", results.size());

        if (isViewAttached()) {
            getView().clearMarkers();

            String s = results.size() + " results found";
            getView().printResults(s);

            for (TruckStop truckStop : results){
                addStationMarkerToMap(truckStop);
            }
        }

        searchManager.clearResults();
        mapManager.getMarkersMap().size();
    }

    @Override
    public void turnSearchBlockOff() {
        // call view to turn isSearching to false
        if(isViewAttached())
            getView().turnTrackingOn();
    }

    void turnSearchBlockOffByDelay(int delay){
        searchManager.manageSearchBlockRunnable(delay);
    }

    // **********************************************************************************
    // ****************************** MAP MANAGER METHODS *******************************
    // **********************************************************************************

    void clearMapMarkers() {
        mapManager.clearMapMarkers();
    }

    void addMarkerToMapManager(Marker marker, TruckStop truckStop) {
        mapManager.addMarkerToMapManager(marker, truckStop);
    }

    @Override
    public TruckStop getStopInfoFromMarker(Marker marker) {
        return mapManager.getStopInfoFromMarker(marker);
    }

    // **********************************************************************************
    // **************************** STATION REQUEST METHODS *****************************
    // **********************************************************************************

    void delayedStationRequest(int delay, String radius, double lat, double lng, boolean saveOldMarkers) {
        stationRequestManager.manageStationRequestRunnable(delay, radius, lat, lng, saveOldMarkers);
    }

    @Override
    public void getStationsByLoc(String radius, double lat, double lng) {
        getTruckStationsByLocation(radius, lat, lng);
    }

    void killRequestRunnable(){
        stationRequestManager.stopRequestRunnable();
    }

    // **********************************************************************************
    // ***************************** TRACKING MODE METHODS ******************************
    // **********************************************************************************

    void turnTrackingOnByDelay(int delay) {
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
                        // do nothing
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

                for (TruckStop truckStop : list) {
                    addStationMarkerToMap(truckStop);
                }
            }
        }
    }

    private void addStationMarkerToMap(TruckStop truckStop) {
        MarkerOptions options = new MarkerOptions();
        double lat = Double.parseDouble(truckStop.getLat());
        double lng = Double.parseDouble(truckStop.getLng());
        options.position(new LatLng(lat, lng));
        options.title(truckStop.getName());
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_mall_directory_black_36dp));

        if (isViewAttached())
            getView().addTruckStopToMap(options, truckStop);
    }

    void getSavedMapType(){
        Timber.d("PERSIST - Map type requested from shared pref");
        mapTypeSub = dataManager.getMapTypeFromSharedPref()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "Error getting map type from shared preferences");

                        if(isViewAttached())
                            getView().onError(e);
                    }

                    @Override
                    public void onNext(Integer mapType) {
                        if (isViewAttached())
                            getView().returnMapType(mapType);
                    }
                });
    }

    void saveMapTypeToSharedPref(int mapType){
        dataManager.saveMapType(mapType);
    }

    void getSavedTrackingState(){
        trackingStateSub = dataManager.getTrackingStateFromSharedPref()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "Error getting tracking state from shared preferences");

                        if(isViewAttached())
                            getView().onError(e);
                    }

                    @Override
                    public void onNext(Boolean isTracking) {
                        if (isViewAttached())
                            getView().returnTrackingState(isTracking);
                    }
                });
    }

    void saveTrackingState(boolean isTracking){
        dataManager.saveTrackingState(isTracking);
    }
}
