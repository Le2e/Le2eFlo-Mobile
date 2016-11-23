package com.le2e.le2etruckstop.data.model;

import com.le2e.le2etruckstop.data.remote.response.TruckStop;

public class TruckStopMarker {
    private TruckStop truckStopDetails;
    private String stopName;
    private String stopLat;
    private String stopLng;

    public TruckStopMarker(TruckStop truckStopDetails) {
        this.truckStopDetails = truckStopDetails;
        stopName = truckStopDetails.getName();
        stopLat = truckStopDetails.getLat();
        stopLng = truckStopDetails.getLng();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TruckStopMarker that = (TruckStopMarker) o;

        if (stopName != null ? !stopName.equals(that.stopName) : that.stopName != null)
            return false;
        if (stopLat != null ? !stopLat.equals(that.stopLat) : that.stopLat != null) return false;
        return stopLng != null ? stopLng.equals(that.stopLng) : that.stopLng == null;

    }

    @Override
    public int hashCode() {
        int result = stopName != null ? stopName.hashCode() : 0;
        result = 31 * result + (stopLat != null ? stopLat.hashCode() : 0);
        result = 31 * result + (stopLng != null ? stopLng.hashCode() : 0);
        return result;
    }
}
