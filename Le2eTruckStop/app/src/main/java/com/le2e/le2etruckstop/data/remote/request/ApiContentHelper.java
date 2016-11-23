package com.le2e.le2etruckstop.data.remote.request;


import com.le2e.le2etruckstop.BuildConfig;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;

import rx.Observable;
import rx.functions.Func0;

public class ApiContentHelper {
    private TruckServiceApi api;

    public ApiContentHelper(TruckServiceApi api) {
        this.api = api;
    }

    public Observable<StationsResponse> getStations(final String radius, final double lat, final double lng){
        return Observable.defer(new Func0<Observable<StationsResponse>>() {
            @Override
            public Observable<StationsResponse> call() {
                return api.getStations(BuildConfig.API_VERSION, radius, new StationBody(lat, lng));
            }
        });
    }
}