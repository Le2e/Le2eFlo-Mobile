package com.le2e.le2etruckstop.data.remote.response;


import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class StationsResponse {
    @SerializedName("truckStops")
    private ArrayList<TruckStop> truckStopList;

    public ArrayList<TruckStop> getTruckStopList() {
        return truckStopList;
    }
}
