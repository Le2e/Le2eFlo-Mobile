package com.le2e.le2etruckstop.ui.home;


import android.app.Activity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.manager.StationMapManager;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;
import com.le2e.le2etruckstop.ui.home.interfaces.MapManagerImpl;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> implements MapManagerImpl {
    private DataManager dataManager;
    private StationMapManager mapManager;

    private Subscription truckStationSub;
    private Subscription mapTypeSub;
    private Subscription trackingStateSub;

    public MapsHomePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
        mapManager = new StationMapManager(this);
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

    // ************************* MAP MANAGER METHODS *************************

    // Setup mapManager with needed dependencies
    public void initLocationServices(GoogleApiClient client, GoogleMap map, WeakReference<Activity> weakRef) {
        mapManager.setupLocationServices(client, map, weakRef);
    }

    // ************************* API REQUEST METHODS *************************

    // Request station information specific to a passed in location
    @Override
    public void getStationsByLoc(String radius, double lat, double lng) {
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
                        mapManager.handleStationResponse(stationsResponse);
                        Timber.d("*** Size of response: %s ***", stationsResponse.getTruckStopList().size());
                    }
                });
    }

    // ********************************** MAP TYPE TOGGLE EVENTS **********************************

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
                        mapManager.setMapType(mapType);
                    }
                });
    }

    // Persist map type in shared pref for app restart -- look into turning this into an observable
    @Override
    public void saveMapTypeToSharedPref(int mapType) {
        dataManager.saveMapType(mapType);
    }

    void toggleMapType() {
        mapManager.toggleMapType();
    }

    // ********************************** TRACKING EVENTS **********************************

    // Get previously saved tracking state - off by default
    @Override
    public void getSavedTrackingState() {
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
                        mapManager.setTrackingEnabledState(isTracking);
                        toggleTrackingFabIcon(isTracking);
                    }
                });
    }

    void toggleTrackingModeEvent() {
        mapManager.toggleTrackingMode();
    }

    // Persist tracking state for app restart
    @Override
    public void saveTrackingState(boolean isTracking) {
        Timber.d("PERSIST - Saving tracking state: %s", isTracking);
        dataManager.saveTrackingState(isTracking);
    }

    @Override
    public void toggleTrackingFabIcon(boolean isTracking) {
        if (isViewAttached())
            getView().toggleTrackingIcon(isTracking);
    }

    // ********************************** MAP CAMERA MOVE EVENTS **********************************

    // Move camera to user's current location
    void moveToCurrentLocation() {
        mapManager.moveToCurrentLoc();
    }

    // *********************************** SEARCH EVENTS ***********************************

    // Launches search logic in searchManager
    public void performSearch(String name, String city, String state, String zip) {
        mapManager.searchKnownTruckStops(name, city, state, zip);
    }

    // Clears current markers off map and displays matches from search for 30 seconds
    @Override
    public void deliverSearchResults(int numResults) {
        Timber.d("Results found: %s", numResults);

        // look at making this part of the map manager as well by using activity context ref
        if (isViewAttached()) {
            String s = numResults + " results found";
            getView().printResults(s);
        }

    }

    void searchPanelSlideEvent(SlidingUpPanelLayout.PanelState newState) {
        mapManager.searchPanelSlideEvent(newState);
    }
}
