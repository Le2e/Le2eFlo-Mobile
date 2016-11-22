package com.le2e.le2etruckstop.data.remote.response;


import com.google.gson.annotations.SerializedName;

public class TruckStop {
    @SerializedName("name")
    private String name;
    @SerializedName("city")
    private String city;
    @SerializedName("state")
    private String state;
    @SerializedName("country")
    private String country;
    @SerializedName("zip")
    private String zip;
    @SerializedName("lat")
    private String lat;
    @SerializedName("lng")
    private String lng;
    @SerializedName("rawLine1")
    private String rawLine1;
    @SerializedName("rawLine2")
    private String rawLine2;
    @SerializedName("rawLine3")
    private String rawLine3;

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getRawLine1() {
        return rawLine1;
    }

    public String getRawLine2() {
        return rawLine2;
    }

    public String getRawLine3() {
        return rawLine3;
    }
}
