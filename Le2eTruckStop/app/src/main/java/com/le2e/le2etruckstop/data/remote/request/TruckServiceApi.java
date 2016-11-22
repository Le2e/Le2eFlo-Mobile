package com.le2e.le2etruckstop.data.remote.request;


import com.le2e.le2etruckstop.data.remote.response.StationsResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

public interface TruckServiceApi {

    @POST("/svc1.transflomobile.com/api/{version}/stations/{radius}")
    Observable<StationsResponse> getStations(@Path("version") String version, @Path("radius") String radius, @Body StationBody body);
}
