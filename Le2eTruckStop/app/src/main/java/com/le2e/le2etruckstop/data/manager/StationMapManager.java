package com.le2e.le2etruckstop.data.manager;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

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
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.common.TruckStopPopupAdapter;
import com.le2e.le2etruckstop.ui.home.impl.MapManagerImpl;
import com.le2e.le2etruckstop.ui.home.impl.PopupInfoImpl;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;

import timber.log.Timber;

public class StationMapManager implements LocationListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {
    private final int SEARCH_BLOCK_DELAY = 30000;
    private final int API_REQUEST_DELAY = 500;
    private final int DIALOG_DELAY = 15000;

    private HashMap<Marker, TruckStop> markersMap;
    private HashSet<TruckStop> stationSet;
    private MapManagerImpl mapManagerPresenter;

    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private TruckStopPopupAdapter popupAdapter;
    private Marker currentLocMarker;
    private LatLng currentLoc;

    private float zoomLevel = 7.0f;
    private boolean isMapFirstLoad = false;
    private boolean isTrackingEnabled = false;
    private boolean isTrackingSuspended = false;
    private boolean isSearching = false;
    private boolean isSatellite = false;
    private boolean infoPop = false;

    public StationMapManager(MapManagerImpl mapManagerPresenter) {
        this.mapManagerPresenter = mapManagerPresenter;
        markersMap = new HashMap<>();
        stationSet = new HashSet<>();
    }

    public TruckStop getStopInfoFromMarker(Marker marker) {
        return markersMap.get(marker);
    }

    public HashMap<Marker, TruckStop> getMarkersMap() {
        return markersMap;
    }

    // clears out set data for marker/truck stop details
    public void clearMapMarkers() {
        if (googleMap != null) {
            googleMap.clear();
            setCurrentLocationMarker(currentLoc);
        }

        markersMap.clear();
        stationSet.clear();
    }

    public void setupLocationServices(GoogleApiClient client, GoogleMap googleMap, WeakReference<Activity> activityRef, PopupInfoImpl popupInfoPresenter) {
        this.googleApiClient = client;
        this.googleMap = googleMap;
        popupAdapter = new TruckStopPopupAdapter(activityRef, popupInfoPresenter);
        setupGoogleMap();

        if (activityRef.get() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(activityRef.get(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(activityRef.get(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(5000);

            isMapFirstLoad = true;

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    private void setupGoogleMap() {
        mapManagerPresenter.getSavedMapType();
        googleMap.setInfoWindowAdapter(popupAdapter);
        googleMap.setOnCameraIdleListener(this);
        googleMap.setOnMarkerClickListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLoc = new LatLng(location.getLatitude(), location.getLongitude());

            // one time check to set initial view to current location
            if (isMapFirstLoad) {
                moveToCurrentLoc();
                isMapFirstLoad = false;
                updateCurrentMarker(false);
            } else {
                Timber.d("SearchingModeEnabled: %s", isSearching);
                Timber.d("TrackingModeEnabled: %s", isTrackingEnabled);
                if (isTrackingEnabled) {
                    if (!isTrackingSuspended) {
                        Timber.d("unsuspended move");
                        updateCurrentMarker(true);
                    }
                } else {
                    Timber.d("non tracked current loc update");
                    updateCurrentMarker(false);
                }
            }
        }
    }

    @Override
    public void onCameraIdle() {
        // Prevents api calls / tracking movement while search window is open or after it
        //      has been closed for a specified duration.
        if (!isSearching) {

            // Prevents actions when a info window is opened for a marker.
            if (!infoPop) {
                if (googleMap != null) {
                    // get bounds for current view of map
                    LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
                    bounds.getCenter();

                    // Tracking logic
                    if (isTrackingEnabled) {
                        // Prevent camera recenter w/ isTrackingSuspended
                        isTrackingSuspended = true;

                        Timber.d("suspended runnable started");

                        // Spawn runnable when user slides screen to recenter camera after
                        //      brief duration.
                        mapManagerPresenter.turnTrackingOnByDelay(5000);

                        // Make api call to add points to map as user scrolls, use true param
                        //      to prevent marker deletion.
                        mapManagerPresenter.delayedStationRequest(
                                API_REQUEST_DELAY,
                                "100",
                                bounds.getCenter().latitude,
                                bounds.getCenter().longitude,
                                true);
                    } else {
                        Timber.d("non tracked api call");
                        // Limiting marker placement by 100 mile for the time being - cluster
                        //      and other optimizations can be made in the future to allow
                        //      a wider view.
                        mapManagerPresenter.delayedStationRequest(
                                API_REQUEST_DELAY,
                                "100",
                                bounds.getCenter().latitude,
                                bounds.getCenter().longitude,
                                false);
                    }
                }
            }
        }

        // clear popup block after its been seen for first time
        infoPop = false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Delay tracking mode restart by specific amount
        if (isTrackingEnabled) {
            Timber.d("delayed by clicking dialog");
            mapManagerPresenter.turnTrackingOnByDelay(DIALOG_DELAY);
        }

        // set popup blocker to prevent post pop camera movement / api calls
        infoPop = true;
        return false;
    }

    public void moveToCurrentLoc() {
        if (currentLoc != null && googleMap != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentLoc, zoomLevel);
            googleMap.animateCamera(update);
        }
    }

    private void updateCurrentMarker(boolean moveCamera) {
        // set current location marker
        if (currentLocMarker != null) {
            if (!currentLocMarker.isInfoWindowShown())
                currentLocMarker.remove();
        }

        setCurrentLocationMarker(currentLoc);
        if (moveCamera) {
            moveToCurrentLoc();
        }
    }

    private void setCurrentLocationMarker(LatLng loc) {
        MarkerOptions options = new MarkerOptions()
                .title("You")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_blue_36dp))
                .position(loc);

        currentLocMarker = googleMap.addMarker(options);
    }

    public void setMapType(int mapType) {
        Timber.d("PERSIST - Map type returned from shared pref: %s", mapType);
        if (mapType == GoogleMap.MAP_TYPE_NORMAL)
            isSatellite = false;
        else if (mapType == GoogleMap.MAP_TYPE_SATELLITE)
            isSatellite = true;

        Timber.d("PERSIST - Map type set to: %s", mapType);
        if (googleMap != null)
            googleMap.setMapType(mapType);

        saveMapType(mapType);
    }

    private void saveMapType(int mapType) {
        Timber.d("PERSIST - Saving map type to sharedPref: %s", mapType);
        mapManagerPresenter.saveMapTypeToSharedPref(mapType);
    }

    public LatLng getCurrentLoc() {
        return currentLoc;
    }

    public void addMarkerToMapView(TruckStop truckStop) {
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

    public void turnTrackingOn() {
        Timber.d("tracking unsuspended");

        if (isSearching)
            Timber.d("search block cleared");

        isTrackingSuspended = false;
        turnSearchingOff();
    }

    private void turnSearchingOff() {
        isSearching = false;
    }

    public boolean getIsTrackingEnabled() {
        return isTrackingEnabled;
    }

    public int getSearchDelay() {
        return SEARCH_BLOCK_DELAY;
    }

    public void setTrackingEnabledState(boolean isTracking) {
        isTrackingEnabled = isTracking;
    }

    public boolean getIsSatellite(){
        return isSatellite;
    }

    public void setIsSearching(boolean isSearching){
        this.isSearching = isSearching;
    }
}
