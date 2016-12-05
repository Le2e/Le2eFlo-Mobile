package com.le2e.le2etruckstop.data.manager;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;

import android.support.v7.app.AlertDialog;
import android.view.View;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.common.TruckStopPopupAdapter;
import com.le2e.le2etruckstop.ui.home.interfaces.MapManagerImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.PopupInfoImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.SearchImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.StationRequestImpl;
import com.le2e.le2etruckstop.ui.home.interfaces.TrackingImpl;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import timber.log.Timber;

public class StationMapManager
    implements LocationListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener,
    SearchImpl, TrackingImpl, StationRequestImpl, PopupInfoImpl, GoogleMap.OnMapClickListener {
    @SuppressWarnings("FieldCanBeLocal") private final int SEARCH_BLOCK_DELAY = 30000;
    @SuppressWarnings("FieldCanBeLocal") private final int API_REQUEST_DELAY = 500;
    @SuppressWarnings("FieldCanBeLocal") private final int DIALOG_DELAY = 15000;

    public static final int REQUEST_PERMISSIONS = 20;

    private HashMap<Marker, TruckStop> markersMap;
    private HashSet<TruckStop> stationSet;
    private MapManagerImpl mapManagerPresenter;

    private GoogleMap googleMap;
    @SuppressWarnings("FieldCanBeLocal") private GoogleApiClient googleApiClient;
    @SuppressWarnings("FieldCanBeLocal") private LocationRequest locationRequest;
    private TruckStopPopupAdapter popupAdapter;
    private Marker currentLocMarker;
    private LatLng currentLoc;
    private Marker lastClickedMarker;

    @SuppressWarnings("FieldCanBeLocal") private float zoomLevel = 7.0f;
    private boolean isMapFirstLoad = false;
    private boolean isTrackingEnabled = false;
    private boolean isTrackingSuspended = false;
    private boolean isSearching = false;
    private boolean isSatellite = false;
    private boolean infoPop = false;

    private StationSearchManager searchManager;
    private StationRequestManager requestManager;
    private TrackingModeManager trackingManager;

    public StationMapManager(MapManagerImpl mapManagerPresenter) {
        this.mapManagerPresenter = mapManagerPresenter;

        searchManager = new StationSearchManager(this);
        trackingManager = new TrackingModeManager(this);
        requestManager = new StationRequestManager(this);

        markersMap = new HashMap<>();
        stationSet = new HashSet<>();
    }

    // Sets up location services
    public void setupLocationServices(GoogleApiClient client, GoogleMap googleMap,
        final WeakReference<Activity> activityRef) {
        this.googleApiClient = client;
        this.googleMap = googleMap;
        popupAdapter = new TruckStopPopupAdapter(activityRef, this);

        if (activityRef.get() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(activityRef.get(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(activityRef.get(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                    Timber.d("PERMS - M - permissions not available");

                    Snackbar.make(activityRef.get().findViewById(R.id.overall),
                        "Please Grant Permissions",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Timber.d("PERMS - SNACKS!");
                                ActivityCompat.requestPermissions(activityRef.get(),
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
                            }
                        }).show();

                    Timber.d("PERMS - Permissions not granted");
                } else {
                    Timber.d("PERMS - Marsh - granted!");
                    doStuff();
                }
            } else {
                doStuff();
            }
        } else {
            Timber.d("PERMS - activity null");
        }
    }

    public void doStuff() {
        Timber.d("PERMS - doing stuff - setting up the map!");
        setupGoogleMap();
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);

        isMapFirstLoad = true;

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    // Sets up map listeners
    private void setupGoogleMap() {
        mapManagerPresenter.getSavedMapType();
        mapManagerPresenter.getSavedTrackingState();
        googleMap.setInfoWindowAdapter(popupAdapter);
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(this);
    }

    // Receives location as user moves
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLoc = new LatLng(location.getLatitude(), location.getLongitude());

            // one time check to set initial view to current location
            if (isMapFirstLoad) {
                moveToCurrentLoc();
                isMapFirstLoad = false;
                updateCurrentLocationMarker(false);
            } else {
                Timber.d("SearchingModeEnabled: %s", isSearching);
                Timber.d("TrackingModeEnabled: %s", isTrackingEnabled);
                if (isTrackingEnabled) {
                    if (!isTrackingSuspended) {
                        Timber.d("unsuspended move");
                        updateCurrentLocationMarker(true);
                    }
                } else {
                    Timber.d("non tracked current loc update");
                    updateCurrentLocationMarker(false);
                }
            }
        }
    }

    // Called when the camera has come to an idle state
    @Override
    public void onCameraIdle() {
        // Prevents api calls / tracking movement while search window is open or after it
        //      has been closed for a specified duration.
        if (!isSearching) {

            // Prevents actions when a info window is opened for a marker.
            if (!infoPop) {
                if (googleMap != null) {
                    // set case whether to save markers or not
                    boolean saveMarkers = false;
                    // get bounds for current view of map
                    LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
                    bounds.getCenter();

                    // Tracking logic
                    if (isTrackingEnabled) {
                        Timber.d("Camera moved while tracking mode is enabled");
                        // Prevent camera recenter w/ isTrackingSuspended
                        saveMarkers = true;
                        isTrackingSuspended = true;

                        // Spawn runnable when user slides screen to recenter camera after
                        //      brief duration.
                        turnTrackingOnByDelay(5000);
                    }
                    // Limiting marker placement by 100 mile for the time being - cluster
                    //      and other optimizations can be made in the future to allow
                    //      a wider view.
                    requestManager.manageStationRequestRunnable(API_REQUEST_DELAY, "100",
                        bounds.getCenter().latitude, bounds.getCenter().longitude, saveMarkers);
                }
            }
        }

        // clear popup block after its been seen for first time
        infoPop = false;
    }

    // Shows marker info window
    @Override
    public boolean onMarkerClick(Marker marker) {
        lastClickedMarker = marker;
        lastClickedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_yellow_36dp));

        // Delay tracking mode restart by specific amount
        if (isTrackingEnabled) {
            Timber.d("delayed by clicking dialog");
            turnTrackingOnByDelay(DIALOG_DELAY);
        }

        // set popup blocker to prevent post pop camera movement / api calls
        infoPop = true;
        return false;
    }

    @Override
    public void onMapClick(final LatLng latLng) {
        if(lastClickedMarker != null){
            if (!lastClickedMarker.isInfoWindowShown()){
                lastClickedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_red_36dp));
            }
        }

        Timber.d("EEEEEE - click!");
    }

    // Clears out set data for marker/truck stop details
    private void clearMapMarkers() {
        Timber.d("clearing markers");
        if (googleMap != null) {
            googleMap.clear();
            setCurrentLocationMarker(currentLoc);
        }

        markersMap.clear();
        stationSet.clear();
    }

    // Moves camera to user's current loc
    public void moveToCurrentLoc() {
        if (currentLoc != null && googleMap != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentLoc, zoomLevel);
            googleMap.animateCamera(update);
        }
    }

    // Maintains current location marker for user's position
    private void updateCurrentLocationMarker(boolean moveCamera) {
        // set current location marker
        if (currentLocMarker != null) {
            if (!currentLocMarker.isInfoWindowShown()) currentLocMarker.remove();
        }

        setCurrentLocationMarker(currentLoc);
        if (moveCamera) {
            moveToCurrentLoc();
        }
    }

    // Sets the current location marker for user's location
    private void setCurrentLocationMarker(LatLng loc) {
        MarkerOptions options = new MarkerOptions().title("You")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_blue_36dp))
            .position(loc);

        currentLocMarker = googleMap.addMarker(options);
    }

    // Adds a marker with TruckStop object data to map
    private void addMarkerToMapView(TruckStop truckStop) {
        // check if stop has already been added to map - if not, add it
        if (!stationSet.contains(truckStop)) {
            stationSet.add(truckStop);

            MarkerOptions options = new MarkerOptions();
            double lat = Double.parseDouble(truckStop.getLat());
            double lng = Double.parseDouble(truckStop.getLng());
            options.position(new LatLng(lat, lng));
            options.title(truckStop.getName());
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_red_36dp));

            Marker marker = googleMap.addMarker(options);

            markersMap.put(marker, truckStop);
        }

        /* revisit functionality to handle clearing map when too many unique marks are present
        if(markersMap.size() > 500){
            Timber.d("hash map getting large, clearing out");
            if(activity != null) {
                activity.clearMarkers();
                markersMap.clear();
            }
        }
        */
    }

    // Handles reset of tracking state after a tracking suspension
    private void turnTrackingOn() {
        Timber.d("tracking unsuspended");

        if (isSearching) Timber.d("search block cleared");

        isTrackingSuspended = false;
        setIsSearching(false);
    }

    // Sets tracking state by passed in param
    public void setTrackingEnabledState(boolean isTracking) {
        isTrackingEnabled = isTracking;
    }

    // Sets the searching state to passed param
    private void setIsSearching(boolean isSearching) {
        this.isSearching = isSearching;
    }

    // ***** BELOW METHODS HAVE BEEN REFACTORED FROM PRESENTER *****

    // ****************************** STATION RESPONSE METHODS ******************************

    public void handleStationResponse(StationsResponse stationsResponse) {
        if (!requestManager.isSaveMarkers()) clearMapMarkers();

        Timber.d("Adding new markers to map");

        for (TruckStop truckStop : stationsResponse.getTruckStopList()) {
            addMarkerToMapView(truckStop);
        }
    }

    // ****************************** MAP TYPE TOGGLE METHODS ******************************

    // Sets the map type
    public void setMapType(int mapType) {
        Timber.d("PERSIST - Map type returned from shared pref: %s", mapType);
        if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
            isSatellite = false;
        } else if (mapType == GoogleMap.MAP_TYPE_SATELLITE) isSatellite = true;

        Timber.d("PERSIST - Map type set to: %s", mapType);
        if (googleMap != null) googleMap.setMapType(mapType);

        saveMapType(mapType);
    }

    // toggles satellite map type on and off with user interaction
    public void toggleMapType() {
        int mapType;
        if (isSatellite) {
            mapType = GoogleMap.MAP_TYPE_NORMAL;
        } else {
            mapType = GoogleMap.MAP_TYPE_SATELLITE;
        }

        setMapType(mapType);
    }

    // Persists the new map type
    private void saveMapType(int mapType) {
        Timber.d("PERSIST - Saving map type to sharedPref: %s", mapType);
        mapManagerPresenter.saveMapTypeToSharedPref(mapType);
    }

    // ****************************** TRACKING FUNCTIONALITY METHODS ******************************

    // toggles tracking mode on and off with user interaction
    public void toggleTrackingMode() {
        // check is tracking
        // - if tracking, kill tracking runnable
        if (isTrackingEnabled) trackingManager.stopTrackingMode();

        // set new tracking mode
        isTrackingEnabled = !isTrackingEnabled;
        // make callback to presenter to toggle icons to reflect new mode in view -- this might should be moved into the view logic
        mapManagerPresenter.toggleTrackingFabIcon(isTrackingEnabled);
        // make callback to persist tracking state
        mapManagerPresenter.saveTrackingState(isTrackingEnabled);
    }

    // restarts tracking runnable by specified delay
    private void turnTrackingOnByDelay(int delay) {
        trackingManager.manageTrackingRunnable(delay);
    }

    // ****************************** SEARCH FUNCTIONALITY METHODS ******************************

    // search currently displayed stops for matches
    public void searchKnownTruckStops(String name, String city, String state, String zip) {
        // perform search - SM
        // - needs marker map
        searchManager.determineSearchParams(name, city, state, zip, markersMap);

        // if isTrackingEnabled
        // - set isSearching block to true - MM
        if (isTrackingEnabled) isSearching = true;
    }

    private void displaySearchResultsOnMap(ArrayList<TruckStop> results) {
        clearMapMarkers();

        for (TruckStop truckStop : results) {
            addMarkerToMapView(truckStop);
        }

        searchManager.clearResults();
        startTimerToClearSearchBlock();
    }

    private void startTimerToClearSearchBlock() {
        // start timer restart tracking after search block if enabled
        if (isTrackingEnabled) turnTrackingOnByDelay(SEARCH_BLOCK_DELAY);

        // start timer to clear search block and restart request runnable
        searchManager.manageSearchBlockRunnable(SEARCH_BLOCK_DELAY);
        requestManager.manageStationRequestRunnable(SEARCH_BLOCK_DELAY, "100", currentLoc.latitude,
            currentLoc.longitude, false);
    }

    public void searchPanelSlideEvent(SlidingUpPanelLayout.PanelState state) {
        // if Expanded
        if (state == SlidingUpPanelLayout.PanelState.EXPANDED) {
            // place search block and stop all requests while user inputs search request
            isSearching = true;
            requestManager.stopRequestRunnable();
            trackingManager.stopTrackingMode();
        }

        // is Collapsed
        if (state == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            // start timer to clear search block - MM + SM
            startTimerToClearSearchBlock();
        }
    }

    // ****************************** INTERFACE IMPLEMENTATIONS ******************************

    // handles passing
    @Override
    public void deliverSearchResults(ArrayList<TruckStop> results) {
        mapManagerPresenter.deliverSearchResults(results.size());
        displaySearchResultsOnMap(results);
    }

    @Override
    public void turnSearchBlockOff() {
        turnTrackingOn();
    }

    @Override
    public void turnTackingBackOn() {
        turnTrackingOn();
    }

    @Override
    public void getStationsByLoc(String radius, double lat, double lng) {
        mapManagerPresenter.getStationsByLoc(radius, lat, lng);
    }

    // Returns TruckStop object based on marker param
    @Override
    public TruckStop getStopInfoFromMarker(Marker marker) {
        return markersMap.get(marker);
    }

    @Override
    public LatLng getCurrentLocation() {
        return currentLoc;
    }
}
