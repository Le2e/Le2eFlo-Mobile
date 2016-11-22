package com.le2e.le2etruckstop.data.remote.request;


import com.google.gson.annotations.SerializedName;

public class StationBody {
    @SerializedName("lat")
    private double lat;
    @SerializedName("lng")
    private double lng;

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public StationBody(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }
}
