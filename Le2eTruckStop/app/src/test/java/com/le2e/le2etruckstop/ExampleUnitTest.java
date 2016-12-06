package com.le2e.le2etruckstop;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.gms.dynamic.zzd;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.internal.zzf;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.home.MapsHomeActivity;
import com.le2e.le2etruckstop.ui.home.MapsHomePresenter;
import com.le2e.le2etruckstop.ui.home.MapsHomeView;
import java.io.FileReader;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import timber.log.Timber;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class ExampleUnitTest {
    Application application;
    MapsHomeView homeView;
    MapsHomePresenter presenter;
    DataManager dataManager;
    SharedPreferences sharedPreferences;
    MapsHomeActivity homeActivity;
    HashMap<Marker, TruckStop> stopsMap;

    @Before
    public void setup() {
        application = Mockito.mock(Application.class);
        homeActivity = Mockito.mock(MapsHomeActivity.class);
        homeView = Mockito.mock(MapsHomeView.class);
        sharedPreferences = Mockito.mock(SharedPreferences.class);
        dataManager = new DataManager(null, sharedPreferences);
        presenter = new MapsHomePresenter(dataManager);
        presenter.attachView(homeView);
        stopsMap = new HashMap<>();

    }

    @Test
    public void ensureSetupWasSuccessful(){
        assertTrue(application != null);
        assertTrue(homeView != null);
        assertTrue(sharedPreferences != null);
        assertTrue(dataManager != null);
        assertTrue(presenter != null);
        assertTrue(homeActivity != null);
    }

    @Test
    public void ensureToggleTrackingFabIconIsCalled(){
        presenter.toggleTrackingFabIcon(true);
        verify(homeView).toggleTrackingIcon(true);

        presenter.toggleTrackingFabIcon(false);
        verify(homeView).toggleTrackingIcon(false);
    }

    @Test
    public void searchButtonClick(){
        populateStopMapWithMockData();
        assertTrue(stopsMap.size() == 3);
        //presenter.performSearch("name", "city", "state", "zip");
    }

    private void populateStopMapWithMockData(){
        StationsResponse response;
        try {
            JsonReader jsonReader = new JsonReader(new FileReader("TruckStopsJson.json"));
            Gson gson = new Gson();
            response = gson.fromJson(jsonReader, StationsResponse.class);
            int numStops = response.getTruckStopList().size();

            for (int i = 0; i < numStops; ++i) {
                stopsMap.put(createMaker(), response.getTruckStopList().get(i));
            }

        } catch (Exception e){
            Timber.e(e, "test json read error");
        }
    }

    private Marker createMaker(){
        return new Marker(new zzf() {
            @Override
            public void remove() throws RemoteException {

            }

            @Override
            public String getId() throws RemoteException {
                return null;
            }

            @Override
            public void setPosition(final LatLng latLng) throws RemoteException {

            }

            @Override
            public LatLng getPosition() throws RemoteException {
                return null;
            }

            @Override
            public void setTitle(final String s) throws RemoteException {

            }

            @Override
            public String getTitle() throws RemoteException {
                return null;
            }

            @Override
            public void setSnippet(final String s) throws RemoteException {

            }

            @Override
            public String getSnippet() throws RemoteException {
                return null;
            }

            @Override
            public void setDraggable(final boolean b) throws RemoteException {

            }

            @Override
            public boolean isDraggable() throws RemoteException {
                return false;
            }

            @Override
            public void showInfoWindow() throws RemoteException {

            }

            @Override
            public void hideInfoWindow() throws RemoteException {

            }

            @Override
            public boolean isInfoWindowShown() throws RemoteException {
                return false;
            }

            @Override
            public void setVisible(final boolean b) throws RemoteException {

            }

            @Override
            public boolean isVisible() throws RemoteException {
                return false;
            }

            @Override
            public boolean zzj(final zzf zzf) throws RemoteException {
                return false;
            }

            @Override
            public int hashCodeRemote() throws RemoteException {
                return 0;
            }

            @Override
            public void zzL(final zzd zzd) throws RemoteException {

            }

            @Override
            public void setAnchor(final float v, final float v1) throws RemoteException {

            }

            @Override
            public void setFlat(final boolean b) throws RemoteException {

            }

            @Override
            public boolean isFlat() throws RemoteException {
                return false;
            }

            @Override
            public void setRotation(final float v) throws RemoteException {

            }

            @Override
            public float getRotation() throws RemoteException {
                return 0;
            }

            @Override
            public void setInfoWindowAnchor(final float v, final float v1) throws RemoteException {

            }

            @Override
            public void setAlpha(final float v) throws RemoteException {

            }

            @Override
            public float getAlpha() throws RemoteException {
                return 0;
            }

            @Override
            public void setZIndex(final float v) throws RemoteException {

            }

            @Override
            public float getZIndex() throws RemoteException {
                return 0;
            }

            @Override
            public void zzM(final zzd zzd) throws RemoteException {

            }

            @Override
            public zzd zzIZ() throws RemoteException {
                return null;
            }

            @Override
            public IBinder asBinder() {
                return null;
            }
        });
    }
}