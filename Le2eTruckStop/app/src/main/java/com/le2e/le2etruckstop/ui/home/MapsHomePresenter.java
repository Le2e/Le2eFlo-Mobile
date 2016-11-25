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
import com.le2e.le2etruckstop.ui.home.interfaces.MapManagerImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.PopupInfoImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.SearchImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.StationRequestImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.TrackingImpl;

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

        if (mapTypeSub != null && !mapTypeSub.isUnsubscribed())
            mapTypeSub.unsubscribe();

        if (trackingStateSub != null && !trackingStateSub.isUnsubscribed())
            trackingStateSub.unsubscribe();

        super.detachView();
    }

    // ************************* SEARCH MANAGER METHODS *************************

    // Launches search logic in searchManager
    void performSearch(String name, String city, String state, String zip) {
        searchManager.determineSearchParams(name, city, state, zip);
    }

    // Clears current markers off map and displays matches from search for 30 seconds
    @Override
    public void deliverSearchResults(ArrayList<TruckStop> results) {
        Timber.d("Results found: %s", results.size());

        if (isViewAttached()) {
            String s = results.size() + " results found";
            getView().printResults(s);

            mapManager.clearMapMarkers();

            for (TruckStop truckStop : results) {
                mapManager.addMarkerToMapView(truckStop);
            }
        }

        searchManager.clearResults();
        mapManager.getMarkersMap().size();
    }

    // Sets the search block for tracking mode
    void setIsSearching(boolean isSearching) {
        mapManager.setIsSearching(isSearching);
    }

    // Handles setting timer to clear search block and resume tracking mode
    void startTimerToClearSearchBlock() {
        if (getIsTrackingEnabled())
            turnTrackingOnByDelay(mapManager.getSearchDelay());

        searchManager.manageSearchBlockRunnable(mapManager.getSearchDelay());
        delayedStationRequest(mapManager.getSearchDelay(),
                "100",
                mapManager.getCurrentLoc().latitude,
                mapManager.getCurrentLoc().longitude,
                false);
    }

    // Clears the search block
    @Override
    public void turnSearchBlockOff() {
        mapManager.turnTrackingOn();
    }

    // ************************* MAP MANAGER METHODS *************************

    // Setup mapManager with needed dependencies
    void initLocationServices(GoogleApiClient client, GoogleMap map, WeakReference<Activity> weakRef) {
        mapManager.setupLocationServices(client, map, weakRef, this);
    }

    // Get marker info for selected marker from marker set
    @Override
    public TruckStop getStopInfoFromMarker(Marker marker) {
        return mapManager.getStopInfoFromMarker(marker);
    }

    // Get user's current location
    @Override
    public LatLng getCurrentLocation() {
        return mapManager.getCurrentLoc();
    }

    // Move camera to user's current location
    void moveToCurrentLocation() {
        mapManager.moveToCurrentLoc();
    }

    // Set the map to the specified type
    void setMapType(int mapType) {
        mapManager.setMapType(mapType);
    }

    // Determine what current map type is
    boolean getIsSatellite() {
        return mapManager.getIsSatellite();
    }

    // Determine what curret tacking state is
    boolean getIsTrackingEnabled() {
        return mapManager.getIsTrackingEnabled();
    }

    // Return the current set of marker on map
    @Override
    public HashMap<Marker, TruckStop> getMarkerMap() {
        return mapManager.getMarkersMap();
    }

    // ************************* API REQUEST METHODS *************************

    // Manage timers and actions for making call to pull new station information
    @Override
    public void delayedStationRequest(int delay, String radius, double lat, double lng, boolean saveOldMarkers) {
        stationRequestManager.manageStationRequestRunnable(delay, radius, lat, lng, saveOldMarkers);
    }

    // Request station information specific to a passed in location
    @Override
    public void getStationsByLoc(String radius, double lat, double lng) {
        getTruckStationsByLocation(radius, lat, lng);
    }

    // Cancel all pending station request calls
    void killRequestRunnable() {
        stationRequestManager.stopRequestRunnable();
    }

    // Call dataManager to use Api to get station information based on location
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
                            getView().onApiError(e);
                        }
                    }

                    @Override
                    public void onNext(StationsResponse stationsResponse) {
                        stationsRetrieved(stationsResponse.getTruckStopList());
                        Timber.d("*** Size of response: %s ***", stationsResponse.getTruckStopList().size());
                    }
                });
    }

    // Handle routing of station information after receiving from API
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

    // ************************* TRACKING MANAGER METHODS *************************

    // Turn tracking mode back on after specified delay - after search, user camera movement, etc.
    @Override
    public void turnTrackingOnByDelay(int delay) {
        trackingManager.manageTrackingRunnable(delay);
    }

    // Cancel all pending tracking updates
    void killTrackingMode() {
        trackingManager.stopTrackingMode();
    }

    // Reset tracking booleans to continue tracking -future refactor - can remove this completely
    @Override
    public void turnTackingBackOn() {
        mapManager.turnTrackingOn();
    }

    // Set the tracking state boolean
    void setTrackingState(boolean isTracking) {
        mapManager.setTrackingEnabledState(isTracking);
    }

    // ************************* PERSISTED OPTIONS METHODS *************************

    // Get previously saved map type - normal by default
    @Override
    public void getSavedMapType() {
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

                        if (isViewAttached())
                            getView().onMapStateError(e);
                    }

                    @Override
                    public void onNext(Integer mapType) {
                        setMapType(mapType);
                    }
                });
    }

    // Persist map type in shared pref for app restart
    @Override
    public void saveMapTypeToSharedPref(int mapType) {
        dataManager.saveMapType(mapType);
    }

    // Get previously saved tracking state - off by default
    void getSavedTrackingState() {
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

                        if (isViewAttached())
                            getView().onTrackingStateError(e);
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

    // Persist tracking state for app restart
    void saveTrackingState(boolean isTracking) {
        dataManager.saveTrackingState(isTracking);
    }
}
