package com.le2e.le2etruckstop.ui.home_screen;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import timber.log.Timber;

public class MapsHomeActivity extends MvpBaseActivity<MapsHomeView, MapsHomePresenter>
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, MapsHomeView,
        GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {

    @Inject DataManager dataManager;

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private TruckStopPopupAdapter popupAdapter;

    private Marker currentLocMarker;
    private Marker lastOpenMarker;
    private LatLng currentLoc;

    private float zoomLevel = 7.0f;

    private boolean infoPop = false;
    private boolean isMapReady = false;
    private boolean isSatellite = false;
    private boolean isTrackingEnabled = false;
    private boolean isTrackingSuspended = false;

    // ****************************************************************************
    // ********************** ACTIVITY LIFECYCLE OVERRIDES ************************
    // ****************************************************************************

    @Override
    public void onCreate(Bundle bundle) {
        BaseApplication.get().getAppComponent().inject(this);
        super.onCreate(bundle);

        popupAdapter = new TruckStopPopupAdapter(new WeakReference<Activity>(this));

        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_maps_home);
            setupFabs();
            initMap();
        } else {
            setContentView(R.layout.activity_maps_home_disabled);
        }
    }

    @Override
    public void onDestroy() {
        googleApiClient.disconnect();
        super.onDestroy();
    }

    private void setupFabs() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (currentLoc != null)
                moveToCurrentLoc(new LatLng(36.665115, -121.636536));
            }
        });

        FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.track_fab);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMap != null) {
                    String msg;

                    if (!isTrackingEnabled)
                        msg = "Tracking Enabled";
                    else {
                        msg = "Tracking Disabled";
                        presenter.killTrackingMode();
                    }

                    isTrackingEnabled = !isTrackingEnabled;
                    Toast.makeText(MapsHomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
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
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_shipping_black_36dp))
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

            if (isTrackingEnabled) {
                Timber.d("enabled");
                if (!isTrackingSuspended) {
                    Timber.d("unsuspended move");
                    //clearMarkers();
                    updateCurrentMarker(true);
                }
            } else {
                Timber.d("non tracked current loc update");
                updateCurrentMarker(false);
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
            mMap.setInfoWindowAdapter(popupAdapter);

            mMap.setOnCameraIdleListener(this);
            mMap.setOnMarkerClickListener(this);

            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            googleApiClient.connect();

            isMapReady = true;
        }
    }

    @Override
    public void onCameraIdle() {
        if(!infoPop) {
            if (mMap != null) {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                bounds.getCenter();

                if (isTrackingEnabled) {
                    // call new api call that adds markers rather than replaces - implement
                    // call runnable for 5 sec
                    isTrackingSuspended = true;
                    Timber.d("suspended runnable started");
                    presenter.determineUserInteraction(5000);
                    presenter.delayedStationRequest("100", bounds.getCenter().latitude, bounds.getCenter().longitude, true);
                } else {
                    // limiting marker placement by 100 mile for the time being - cluster and other optimizations can be made in the future to allow a wider view
                    Timber.d("non tracked api call");
                    presenter.delayedStationRequest("100", bounds.getCenter().latitude, bounds.getCenter().longitude, false);
                }
            }
        }

        infoPop = false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (isTrackingEnabled) {
            Timber.d("delayed by clicking dialog");
            presenter.determineUserInteraction(15000);
        }

        infoPop = true;
        /*
        if (lastOpenMarker != null) {
            lastOpenMarker.hideInfoWindow();

            if (lastOpenMarker.equals(marker)) {
                lastOpenMarker = null;
                return true;
            }
        }
        marker.showInfoWindow();
        lastOpenMarker = marker;
        */
        return false;
    }

    // ****************************************************************************
    // ************************* PRESENTER VIEW CALLBACKS *************************
    // ****************************************************************************

    @Override
    public void addTruckStopToMap(MarkerOptions options, TruckStop truckStop) {
        if (mMap != null && options != null) {
            Marker marker = mMap.addMarker(options);
            popupAdapter.addMarkerToMap(marker, truckStop);
        }
    }

    @Override
    public void clearMarkers() {
        if (mMap != null) {
            mMap.clear();
            setCurrentLocationMarker(currentLoc);
        }

        popupAdapter.clearMarkerMap();
    }

    @Override
    public void turnTrackingOn() {
        Timber.d("tracking unsuspended");
        isTrackingSuspended = false;
    }

    @Override
    public void onError(Throwable e) {
        // TODO: Handle it
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
        if (item.getTitle().equals(getResources().getString(R.string.menu_item_search_title))) {

        } else if (item.getTitle().equals(getResources().getString(R.string.menu_item_satellite_toggle_title))) {
            if (mMap != null) {
                if (!isSatellite)
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                else
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                isSatellite = !isSatellite;
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