package com.njamb.geodrink.models

import android.content.Intent

import com.google.android.gms.maps.model.LatLng

class Coordinates {
    var lat: Double = 0.toDouble()
    var lng: Double = 0.toDouble()

    constructor() {}

    constructor(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }

    fun toGoogleCoords() = LatLng(lat, lng)

    companion object {

        fun getCoordinatesFromIntent(intent: Intent): Coordinates {
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lng = intent.getDoubleExtra("lng", 0.0)
            return Coordinates(lat, lng)
        }
    }
}
