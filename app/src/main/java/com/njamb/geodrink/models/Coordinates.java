package com.njamb.geodrink.models;

import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;

public class Coordinates {
    public double lat;
    public double lng;

    public Coordinates() {}

    public Coordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public LatLng toGoogleCoords() {
        return new LatLng(lat, lng);
    }

    public static Coordinates getCoordinatesFromIntent(Intent intent) {
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        return new Coordinates(lat, lng);
    }
}
