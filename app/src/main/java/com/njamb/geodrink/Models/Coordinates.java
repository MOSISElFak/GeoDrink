package com.njamb.geodrink.Models;

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

}
