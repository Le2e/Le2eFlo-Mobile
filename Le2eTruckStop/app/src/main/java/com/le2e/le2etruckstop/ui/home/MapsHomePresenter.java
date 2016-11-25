package com.le2e.le2etruckstop.ui.home;


import android.app.Activity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.manager.StationMapManager;
import com.le2e.le2etruckstop.data.manager.StationRequestManager;
import com.le2e.le2etruckstop.data.manager.StationSearchManager;
import com.le2e.le2etruckstop.data.manager.TrackingModeManager;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;
import com.le2e.le2etruckstop.ui.home.impl.MapManagerImpl;
import com.le2e.le2etruckstop.ui.home.impl.PopupInfoImpl;
import com.le2e.le2etruckstop.ui.home.impl.SearchImpl;
import com.le2e.le2etruckstop.ui.home.impl.StationRequestImpl;
import com.le2e.le2etruckstop.ui.home.impl.TrackingImpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> implements TrackingImpl,
        StationRequestImpl, PopupInfoImpl, SearchImpl, MapManagerImpl {
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
        mapManager = new StationMapManager(this);
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

        if(trackingStateSub != null && !trackingStateSub.isUnsubscribed())
            trackingStateSub.unsubscribe();

        super.detachView();
    }

    // **********************************************************************************
    // **************************** SEARCH MANAGER METHODS ******************************
    // **********************************************************************************

    void startTimerToClearSearchBlock(){
        if(getIsTrackingEnabled())
            turnTrackingOnByDelay(mapManager.getSearchDelay());

        turnSearchBlockOffByDelay(mapManager.getSearchDelay());
        delayedStationRequest(mapManager.getSearchDelay(),
                "100",
                mapManager.getCurrentLoc().latitude,
                mapManager.getCurrentLoc().longitude,
                false);
    }

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
            String s = results.size() + " results found";
            getView().printResults(s);

            mapManager.clearMapMarkers();

            for (TruckStop truckStop : results){
                mapManager.addMarkerToMapView(truckStop);
            }
        }

        searchManager.clearResults();
        mapManager.getMarkersMap().size();
    }

    @Override
    public void turnSearchBlockOff() {
        mapManager.turnTrackingOn();
    }

    private void turnSearchBlockOffByDelay(int delay){
        searchManager.manageSearchBlockRunnable(delay);
    }

    // **********************************************************************************
    // ****************************** MAP MANAGER METHODS *******************************
    // **********************************************************************************

    @Override
    public TruckStop getStopInfoFromMarker(Marker marker) {
        return mapManager.getStopInfoFromMarker(marker);
    }

    @Override
    public LatLng getCurrentLocation() {
        return mapManager.getCurrentLoc();
    }

    void moveToCurrentLocation(){
        mapManager.moveToCurrentLoc();
    }

    void setMapType(int mapType){
        mapManager.setMapType(mapType);
    }

    boolean getIsSatellite(){
        return mapManager.getIsSatellite();
    }

    boolean getIsTrackingEnabled(){
        return mapManager.getIsTrackingEnabled();
    }

    void setIsSearching(boolean isSearching){
        mapManager.setIsSearching(isSearching);
    }

    // **********************************************************************************
    // **************************** STATION REQUEST METHODS *****************************
    // **********************************************************************************

    @Override
    public void delayedStationRequest(int delay, String radius, double lat, double lng, boolean saveOldMarkers) {
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

    @Override
    public void turnTrackingOnByDelay(int delay) {
        trackingManager.manageTrackingRunnable(delay);
    }

    void killTrackingMode() {
        trackingManager.stopTrackingMode();
    }

    @Override
    public void turnTackingBackOn() {
        mapManager.turnTrackingOn();
    }

    void setTrackingState(boolean isTracking){
        mapManager.setTrackingEnabledState(isTracking);
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
                    mapManager.clearMapMarkers();
                } else {
                    Timber.d("adding markers to existing map");
                }

                for (TruckStop truckStop : list) {
                    mapManager.addMarkerToMapView(truckStop);
                }
            }
        }
    }

    @Override
    public void getSavedMapType(){
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
                        setMapType(mapType);
                    }
                });
    }

    @Override
    public void saveMapTypeToSharedPref(int mapType){
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
                        Timber.d("PERSIST - Tracking state returned from shared pref: %s", isTracking);
                        setTrackingState(isTracking);

                        if (isViewAttached())
                            getView().toggleTrackingIcon(isTracking);
                    }
                });
    }

    void saveTrackingState(boolean isTracking){
        dataManager.saveTrackingState(isTracking);
    }

    // *****************************************************************************************
    // ************************************** MVP TEST *****************************************
    // *****************************************************************************************

    void initLocationServices(GoogleApiClient client, GoogleMap map, WeakReference<Activity> weakRef){
        mapManager.setupLocationServices(client, map, weakRef, this);
    }
}
