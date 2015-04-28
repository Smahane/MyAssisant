package com.phonesettings.myassistant.utils;

import com.google.android.gms.maps.model.LatLng;

public class Utils {

    public static double feets2Meters(double feet){
        return feet * C.FEET_TO_METER_CONSTANT;
    }

    public static double meters2Feets(double meter){
        return meter / C.FEET_TO_METER_CONSTANT;
    }

    public static double degrees2Radians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static double radians2Degrees(double radians) {
        return radians * 180 / Math.PI;
    }

    /**
     * Find coordinates from given starting coordinates, distance and direction.
     * @param  brng = direction in degrees (0=North, 90=East)
     * @param  dist = distance to the searching locations coordinates
     * @param  location = Starting location from where is searched
     * @return Coordinates for searching destination */
    public static LatLng destinationPoint(double brng, double dist, LatLng location) {
        double lat = location.latitude;
        double lng = location.longitude;

        dist = dist / C.EARTH_RADIUS_IN_KM;
        brng = Math.toRadians(brng);

        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lng);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) +
                Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));

        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dist) *
                Math.cos(lat1),
                Math.cos(dist) - Math.sin(lat1) *
                Math.sin(lat2));

        if (lat2 == 0 || lon2 == 0) return null;

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    //compute distance between 2 points by given coordinates
    public static double haversineFormula(LatLng loc1, LatLng loc2){
        double dist = 0.0;
        double latVal1 = loc1.latitude;
        double lonVal1 = loc1.longitude;
        double latVal2 = loc2.latitude;
        double lonVal2 = loc2.longitude;

        double deltaLat = Math.toRadians(latVal2 - latVal1);
        double deltaLon = Math.toRadians(lonVal2 - lonVal1);

        latVal1 = Math.toRadians(latVal1);
        latVal2 = Math.toRadians(latVal2);
        lonVal1 = Math.toRadians(lonVal1);
        lonVal2 = Math.toRadians(lonVal2);

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(latVal1) * Math.cos(latVal2) * Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        dist = C.EARTH_RADIUS_IN_KM * c;
        return dist;
    }
}
