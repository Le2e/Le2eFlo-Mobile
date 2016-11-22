package com.le2e.le2etruckstop.data.manager;


import com.le2e.le2etruckstop.data.remote.request.ApiContentHelper;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;

import rx.Observable;
import rx.functions.Func0;

public class DataManager {
    private ApiContentHelper apiContentHelper;

    public DataManager(ApiContentHelper apiContentHelper) {
        this.apiContentHelper = apiContentHelper;
    }

    public Observable<StationsResponse> getStationsFromApi(final String radius, final double lat, final double lng) {
        return Observable.defer(new Func0<Observable<StationsResponse>>() {
            @Override
            public Observable<StationsResponse> call() {
                return apiContentHelper.getStations(radius, lat, lng);
            }
        });
    }
}