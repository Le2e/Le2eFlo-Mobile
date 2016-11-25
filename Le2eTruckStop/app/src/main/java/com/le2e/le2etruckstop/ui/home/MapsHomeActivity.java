package com.le2e.le2etruckstop.ui.home;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.config.BaseApplication;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.MvpBaseActivity;
import com.le2e.le2etruckstop.ui.common.TruckStopPopupAdapter;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MapsHomeActivity extends MvpBaseActivity<MapsHomeView, MapsHomePresenter>
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, MapsHomeView,
        GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {

    @Inject
    DataManager dataManager;

    @BindView(R.id.sliding_layout)
    SlidingUpPanelLayout searchSlideLayout;
    @BindView(R.id.search_name_et)
    EditText etStopName;
    @BindView(R.id.search_city_et)
    EditText etCityName;
    @BindView(R.id.search_state_et)
    EditText etStateName;
    @BindView(R.id.search_zip_et)
    EditText etZipcode;
    @BindView(R.id.search_btn)
    Button btnSearchStart;
    @BindView(R.id.fab)
    FloatingActionButton fabCurrentLoc;
    @BindView(R.id.track_fab)
    FloatingActionButton fabTrack;

    private final int SEARCH_BLOCK_DELAY = 30000;
    private final int API_REQUEST_DELAY = 500;

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    LocationManager locationManager;
    private Marker currentLocMarker;
    private LatLng currentLoc;

    private TruckStopPopupAdapter popupAdapter;

    private float zoomLevel = 7.0f;
    private boolean infoPop = false;
    private boolean isMapFirstLoad = false;
    private boolean isSatellite = false;
    private boolean isTrackingEnabled = false;
    private boolean isTrackingSuspended = false;
    private boolean isSearching = false;

    // ****************************************************************************
    // ********************** ACTIVITY LIFECYCLE OVERRIDES ************************
    // ****************************************************************************

    @Override
    public void onCreate(Bundle bundle) {
        BaseApplication.get().getAppComponent().inject(this);
        super.onCreate(bundle);

        popupAdapter = new TruckStopPopupAdapter(new WeakReference<Activity>(this), presenter);

        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_maps_home);
            ButterKnife.bind(this);
            presenter.getSavedTrackingState();
            setupFabs();
            initMap();
            setupSlide();
        } else {
            setContentView(R.layout.activity_maps_home_disabled);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLocationEnabled();
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        super.onDestroy();
    }

    // ******************* MISC ********************

    public void isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Timber.e(e, "Request to check gps capability failed");
        }

        if (!gps_enabled) {
            alertUserEnabledLocationServices();
        }

        locationManager = null;
    }

    private void alertUserEnabledLocationServices() {
        new AlertDialog.Builder(this).setIcon(
                android.R.drawable.ic_dialog_alert)
                .setTitle("Location Services Disabled")
                .setMessage("In order to access all the features, you must enable Location Services.")
                .setPositiveButton("Enable Location Services", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSlide() {
        btnSearchStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do search
                presenter.performSearch(
                        etStopName.getText().toString(),
                        etCityName.getText().toString(),
                        etStateName.getText().toString(),
                        etZipcode.getText().toString());

                // dismiss keyboard and slide panel up
                searchSlideLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                dismissKeyboard();

                // determine tracking mode operation
                // --- if tracking
                //      - maintain isSearching block on updates
                if (isTrackingEnabled) {
                    isSearching = true;
                }
            }
        });

        searchSlideLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                // do nothing
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    isSearching = true;
                    // cancel all runnables happening
                    Timber.d("Killed runnables");
                    presenter.killRequestRunnable();
                    presenter.killTrackingMode();
                }

                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    dismissKeyboard();
                    startTimerToClearSearchBlock();
                }

                Timber.d("Panel: %s", newState);
            }
        });
    }

    private void setupFabs() {
        fabCurrentLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLoc != null)
                    moveToCurrentLoc(currentLoc);
            }
        });

        fabTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap != null) {

                    // cancel tracking runnables when tracking is toggled off
                    if (isTrackingEnabled)
                        presenter.killTrackingMode();

                    // update tracking state then save tracking state
                    setTrackingState(!isTrackingEnabled);
                    saveTrackingState(isTrackingEnabled);
                }
            }
        });
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    private void dismissKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void moveToCurrentLoc(LatLng latLng) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel);
        mMap.animateCamera(update);
    }

    public LatLng getCurrentLocation() {
        return currentLoc;
    }

    private void setCurrentLocationMarker(LatLng loc) {
        MarkerOptions options = new MarkerOptions()
                .title("You")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_truck_blue_36dp))
                .position(loc)
                .snippet("Foten \n Next line \n Getting long and stuff!");

        currentLocMarker = mMap.addMarker(options);
    }

    // check to see if google services is available
    public boolean googleServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int isAvailable = apiAvailability.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (apiAvailability.isUserResolvableError(isAvailable)) {
            Dialog dialog = apiAvailability.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    // ****************************************************************************
    // ********************** GOOGLE MAPS / API CLIENT IMPL ***********************
    // ****************************************************************************

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.w("Google APIs connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.e("Google APIs connection failed with code: %d", connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLoc = new LatLng(location.getLatitude(), location.getLongitude());

            // one time check to set initial view to current location
            if (isMapFirstLoad) {
                moveToCurrentLoc(currentLoc);
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

    private void updateCurrentMarker(boolean moveCamera) {
        // set current location marker
        if (currentLocMarker != null) {
            if (!currentLocMarker.isInfoWindowShown())
                currentLocMarker.remove();
        }
        setCurrentLocationMarker(currentLoc);
        if (moveCamera) {
            moveToCurrentLoc(currentLoc);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mMap != null) {
            presenter.getSavedMapType();
            mMap.setInfoWindowAdapter(popupAdapter);

            mMap.setOnCameraIdleListener(this);
            mMap.setOnMarkerClickListener(this);

            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            googleApiClient.connect();

            isMapFirstLoad = true;
        }
    }

    @Override
    public void onCameraIdle() {

        // Prevents api calls / tracking movement while search window is open or after it
        //      has been closed for a specified duration.
        if (!isSearching) {

            // Prevents actions when a info window is opened for a marker.
            if (!infoPop) {
                if (mMap != null) {
                    // get bounds for current view of map
                    LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    bounds.getCenter();

                    // Tracking logic
                    if (isTrackingEnabled) {
                        // Prevent camera recenter w/ isTrackingSuspended
                        isTrackingSuspended = true;

                        Timber.d("suspended runnable started");

                        // Spawn runnable when user slides screen to recenter camera after
                        //      brief duration.
                        presenter.turnTrackingOnByDelay(5000);

                        // Make api call to add points to map as user scrolls, use true param
                        //      to prevent marker deletion.
                        presenter.delayedStationRequest(
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
                        presenter.delayedStationRequest(
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
            presenter.turnTrackingOnByDelay(15000);
        }

        // set popup blocker to prevent post pop camera movement / api calls
        infoPop = true;
        return false;
    }

    // ****************************************************************************
    // ************************* PRESENTER VIEW CALLBACKS *************************
    // ****************************************************************************

    @Override
    public void addTruckStopToMap(MarkerOptions options, TruckStop truckStop) {
        if (mMap != null && options != null) {
            Marker marker = mMap.addMarker(options);
            presenter.addMarkerToMapManager(marker, truckStop);
        }
    }

    @Override
    public void clearMarkers() {
        if (mMap != null) {
            mMap.clear();
            setCurrentLocationMarker(currentLoc);
        }

        presenter.clearMapMarkers();
    }

    @Override
    public void turnTrackingOn() {
        Timber.d("tracking unsuspended");

        if (isSearching)
            Timber.d("search block cleared");

        isTrackingSuspended = false;
        turnSearchingOff();
    }

    @Override
    public void onError(Throwable e) {
        // TODO: Handle it
    }

    // display toast, clear inputs, start timer for delay on return to tracking
    @Override
    public void printResults(String message) {
        clearSearchInputs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startTimerToClearSearchBlock();
    }

    @Override
    public void returnMapType(int mapType) {
        Timber.d("PERSIST - Map type returned from shared pref: %s", mapType);
        setMapType(mapType);
    }

    @Override
    public void returnTrackingState(boolean isTracking) {
        Timber.d("PERSIST - Tracking state returned from shared pref: %s", isTracking);
        setTrackingState(isTracking);
    }

    private void startTimerToClearSearchBlock() {
        Timber.d("***** request delay started! ******");
        if (isTrackingEnabled)
            presenter.turnTrackingOnByDelay(SEARCH_BLOCK_DELAY);

        presenter.turnSearchBlockOffByDelay(SEARCH_BLOCK_DELAY);
        presenter.delayedStationRequest(SEARCH_BLOCK_DELAY, "100", currentLoc.latitude, currentLoc.longitude, false);
    }

    private void turnSearchingOff() {
        isSearching = false;
    }

    private void clearSearchInputs() {
        etStopName.getText().clear();
        etCityName.getText().clear();
        etStateName.getText().clear();
        etZipcode.getText().clear();
    }

    private void saveMapType(int mapType) {
        Timber.d("PERSIST - Saving map type to sharedPref: %s", mapType);
        presenter.saveMapTypeToSharedPref(mapType);
    }

    private void setMapType(int mapType) {
        if (mapType == GoogleMap.MAP_TYPE_NORMAL)
            isSatellite = false;
        else if (mapType == GoogleMap.MAP_TYPE_SATELLITE)
            isSatellite = true;

        Timber.d("PERSIST - Map type set to: %s", mapType);
        mMap.setMapType(mapType);
    }

    private void setTrackingState(boolean isTracking) {
        Timber.d("PERSIST - Tracking state set to: %s", isTracking);
        isTrackingEnabled = isTracking;

        // load appropriate icon for state
        if (isTracking)
            fabTrack.setImageResource(R.drawable.ic_navigation_red_48dp);
        else
            fabTrack.setImageResource(R.drawable.ic_navigation_white_48dp);
    }

    private void saveTrackingState(boolean isTracking) {
        Timber.d("PERSIST - Tracking state saved as: %s", isTracking);
        presenter.saveTrackingState(isTracking);
    }

    // ****************************************************************************
    // *************************** OPTIONS MENU METHODS ***************************
    // ****************************************************************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.menu_item_satellite_toggle_title))) {
            if (mMap != null) {
                int mapType;
                if (isSatellite)
                    mapType = GoogleMap.MAP_TYPE_NORMAL;
                else
                    mapType = GoogleMap.MAP_TYPE_SATELLITE;

                setMapType(mapType);
                saveMapType(mapType);
            }
        }
        return true;
    }

    // ****************************************************************************
    // **************************** MVP IMPLEMENTATION ****************************
    // ****************************************************************************

    @NonNull
    @Override
    public MapsHomePresenter createPresenter() {
        return new MapsHomePresenter(dataManager);
    }
}
