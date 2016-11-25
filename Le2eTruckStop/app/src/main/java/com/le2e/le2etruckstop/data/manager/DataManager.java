package com.le2e.le2etruckstop.data.manager;


import android.content.SharedPreferences;

import com.google.android.gms.maps.GoogleMap;
import com.le2e.le2etruckstop.data.remote.request.ApiContentHelper;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;

import rx.Observable;
import rx.functions.Func0;

public class DataManager {
    private final String MAP_TYPE_TAG = "MAP_TYPE";
    private final String TRACKING_STATE_TAG = "TRACKING_STATE";

    private ApiContentHelper apiContentHelper;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public DataManager(ApiContentHelper apiContentHelper, SharedPreferences sharedPreferences) {
        this.apiContentHelper = apiContentHelper;
        this.sharedPreferences = sharedPreferences;
    }

    public void saveMapType(int mapType) {
        editor = sharedPreferences.edit();
        editor.putInt(MAP_TYPE_TAG, mapType);
        editor.apply();
    }

    public void saveTrackingState(boolean isTracking){
        editor = sharedPreferences.edit();
        editor.putBoolean(TRACKING_STATE_TAG, isTracking);
        editor.apply();
    }

    public Observable<StationsResponse> getStationsFromApi(final String radius, final double lat, final double lng) {
        return Observable.defer(new Func0<Observable<StationsResponse>>() {
            @Override
            public Observable<StationsResponse> call() {
                return apiContentHelper.getStations(radius, lat, lng);
            }
        });
    }

    public Observable<Integer> getMapTypeFromSharedPref() {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(sharedPreferences.getInt(MAP_TYPE_TAG, GoogleMap.MAP_TYPE_NORMAL));
            }
        });
    }

    public Observable<Boolean> getTrackingStateFromSharedPref(){
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(sharedPreferences.getBoolean(TRACKING_STATE_TAG, false));
            }
        });
    }
}