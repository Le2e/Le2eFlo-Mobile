package com.le2e.le2etruckstop.data.model;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.HashMap;

public class TruckStopUtils {
    private HashMap<Marker, TruckStop> stopsMap;
    private final double EQUATOR_LENGTH = 24874;
    private final double EQUATOR_PIXEL_VALUE = 256;

    public TruckStopUtils(){
        stopsMap = new HashMap<>();
    }

    public void clearMarkerMap(){
        stopsMap.clear();
    }

    public void addMarkerToMap(Marker marker, TruckStop data){
        stopsMap.put(marker, data);
    }

    public void removeMarkerFromMap(Marker marker){
        stopsMap.remove(marker);
    }

    public HashMap<Marker, TruckStop> getStopsMap(){
        return stopsMap;
    }

    public TruckStop getTruckStop(Marker marker){
        return stopsMap.get(marker);
    }



    public static String formatTruckStopRawLine(String one, String two, String three){
        StringBuilder sb = new StringBuilder();

        if (one != null)
            sb.append(one);

        if (two != null) {
            sb.append("\n");
            sb.append(two);
        }
        if (three != null) {
            sb.append("\n");
            sb.append(three);
        }

        return sb.toString();
    }

    // extract into separate method and TDD
    public int calculateZoomLevel(double radius, int screenWidth) {
        double diameter = radius * 2;
        double milesPerPixel = EQUATOR_LENGTH / EQUATOR_PIXEL_VALUE;
        int zoomlvl = 1;
        while ((milesPerPixel * screenWidth) > diameter) {
            milesPerPixel /= 2;
            ++zoomlvl;
        }
        return zoomlvl;
    }

    // extract into separate method and TDD
    public int calculateRadius(int screenWidth, int zoomLevel) {
        int calcZoom = 1;
        if (zoomLevel == calcZoom) {
            return (int) EQUATOR_LENGTH / 2;
        }

        double milesPerPixel = EQUATOR_LENGTH / EQUATOR_PIXEL_VALUE;

        while (calcZoom != zoomLevel) {
            milesPerPixel /= 2;
            calcZoom++;
        }

        double diameter = milesPerPixel * screenWidth;

        return (int) diameter / 2;
    }
}
