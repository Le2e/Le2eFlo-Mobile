package com.le2e.le2etruckstop.data.model;


public class TruckStopUtils {
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
}
