package com.le2e.le2etruckstop.ui.home;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.config.BaseApplication;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.ui.base.mvp.MvpBaseActivity;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MapsHomeActivity extends MvpBaseActivity<MapsHomeView, MapsHomePresenter>
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, MapsHomeView,
        GoogleApiClient.OnConnectionFailedListener {

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

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationManager locationManager;

    @NonNull
    @Override
    public MapsHomePresenter createPresenter() {
        return new MapsHomePresenter(dataManager);
    }

    @Override
    public void onCreate(Bundle bundle) {
        BaseApplication.get().getAppComponent().inject(this);
        super.onCreate(bundle);

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

    // Sets up SlidingUpPanel event listeners and logic
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
                if (presenter.getIsTrackingEnabled()) {
                    presenter.setIsSearching(true);
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
                Timber.d("Panel: %s", newState);

                // set searching block - cancel active runnables while EXPANDED
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    presenter.setIsSearching(true);
                    presenter.killRequestRunnable();
                    presenter.killTrackingMode();
                }

                // hidekeyboard if visible, start timer to cancel search block
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    dismissKeyboard();
                    presenter.startTimerToClearSearchBlock();
                }
            }
        });
    }

    // Sets up FAB click events
    private void setupFabs() {
        fabCurrentLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.moveToCurrentLocation();
            }
        });

        fabTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isTracking = presenter.getIsTrackingEnabled();

                // cancel tracking runnables when tracking is toggled off
                if (isTracking)
                    presenter.killTrackingMode();

                // update tracking state then save tracking state
                presenter.setTrackingState(!isTracking);
                toggleTrackingIcon(!isTracking);
                presenter.saveTrackingState(!isTracking);
            }
        });
    }

    // Toolbar mean creation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Toggle satellite and normal mode
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.menu_item_satellite_toggle_title))) {
            int mapType;

            // If isSatellite - swap to normal and update maptype
            if (presenter.getIsSatellite())
                mapType = GoogleMap.MAP_TYPE_NORMAL;
            else
                mapType = GoogleMap.MAP_TYPE_SATELLITE;

            presenter.setMapType(mapType);
        }
        return true;
    }

    // GoogleApiClient connect success - starts mapManager initialization
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        presenter.initLocationServices(googleApiClient, mMap, new WeakReference<Activity>(this));
    }

    // Fires when googleApiClient connection is suspended
    @Override
    public void onConnectionSuspended(int i) {
        Timber.w("Google APIs connection suspended.");
    }

    // Fires when googleApiclient failes to connect
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.e("Google APIs connection failed with code: %d", connectionResult.getErrorCode());
    }

    // Sets up the map fragment view
    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    // Called when async setup in initMap() finishes
    // - gets reference to map fragment
    // - connects googleApiClient
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap != null) {
            mMap = googleMap;

            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            googleApiClient.connect();
        }
    }

    // Error handler for api calls via observables
    @Override
    public void onApiError(Throwable e) {
        showErrorDialog(getResources().getString(R.string.error_title), getResources().getString(R.string.error_api_connection));
    }

    @Override
    public void onMapStateError(Throwable e) {
        showErrorDialog(getResources().getString(R.string.error_title), getResources().getString(R.string.error_map_state));
    }

    @Override
    public void onTrackingStateError(Throwable e) {
        showErrorDialog(getResources().getString(R.string.error_title), getResources().getString(R.string.error_tracking_state));
    }

    private void showErrorDialog(String title, String message){
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(getResources().getString(R.string.btn_ok), null)
                .show();
    }

    // Display toast, clear inputs, start timer for delay on return to tracking
    @Override
    public void printResults(String message) {
        clearSearchInputs();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        presenter.startTimerToClearSearchBlock();
    }

    // Toggles fab icon to reflect tracking mode state
    @Override
    public void toggleTrackingIcon(boolean isTracking) {
        Timber.d("WTF %s", isTracking);
        if (isTracking)
            fabTrack.setImageResource(R.drawable.ic_navigation_red_48dp);
        else
            fabTrack.setImageResource(R.drawable.ic_navigation_white_48dp);
    }

    // Checks to see if google services is available - offers user option to enable
    private boolean googleServicesAvailable() {
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

    // Checks to makes location services are enabled - provides user a way to turn them on
    private void isLocationEnabled() {
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

    // Alert dialog that offers user option to enable location services
    private void alertUserEnabledLocationServices() {
        new AlertDialog.Builder(this).setIcon(
                android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.error_loc_serv_title))
                .setMessage(getResources().getString(R.string.error_loc_serv_msg))
                .setPositiveButton(getResources().getString(R.string.error_loc_ser_positive), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getResources().getString(R.string.error_loc_ser_negative), null)
                .show();
    }

    // Clears out old search inputs
    private void clearSearchInputs() {
        etStopName.getText().clear();
        etCityName.getText().clear();
        etStateName.getText().clear();
        etZipcode.getText().clear();
    }

    // Hides the soft keyboard if present
    private void dismissKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

}
