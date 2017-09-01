package com.njamb.geodrink.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.njamb.geodrink.services.PoiService

class PlacesGeoFire(context: Context, private val mService: PoiService) : GeoQueryEventListener {

    private val mGeoFirePlaces: GeoFire
    private val mGeoQueryPlaces: GeoQuery

    private val mLocalBcastManager: LocalBroadcastManager


    init {

        mGeoFirePlaces = GeoFire(FirebaseDatabase.getInstance().getReference("placesGeoFire"))
        mGeoQueryPlaces = mGeoFirePlaces.queryAtLocation(GeoLocation(0.0, 0.0), 0.1)
        mGeoQueryPlaces.addGeoQueryEventListener(this)

        mLocalBcastManager = LocalBroadcastManager.getInstance(context)

        val filter = IntentFilter(PlacesGeoFire.ACTION_SET_LOCATION)
        mLocalBcastManager.registerReceiver(mReceiver, filter)
    }

    fun onDestroy() {
        mLocalBcastManager.unregisterReceiver(mReceiver)
    }

    override fun onKeyEntered(key: String, location: GeoLocation) {
        val intent = Intent(PoiService.ACTION_PLACE_IN_RANGE)
        intent.putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude)
                .putExtra("key", key)

        mLocalBcastManager.sendBroadcast(intent)
    }

    override fun onKeyExited(key: String) {
        mService.keyExited(key)
    }

    override fun onKeyMoved(key: String, location: GeoLocation) {
        mService.keyMoved(key, location)
    }

    //region Unused methods
    override fun onGeoQueryReady() {

    }

    override fun onGeoQueryError(error: DatabaseError) {

    }
    //endregion

    fun setCenter(loc: GeoLocation) {
        mGeoQueryPlaces.center = loc
    }

    fun setRadius(rad: Double) {
        mGeoQueryPlaces.radius = rad
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (PlacesGeoFire.ACTION_SET_LOCATION == action) {
                val id = intent.getStringExtra("id")
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lng = intent.getDoubleExtra("lng", 0.0)
                val loc = GeoLocation(lat, lng)

                mGeoFirePlaces.setLocation(id, loc)
            }
        }
    }

    companion object {
        val ACTION_SET_LOCATION = "com.njamb.geofire.setplacelocation"
    }
}