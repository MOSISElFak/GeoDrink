package com.njamb.geodrink.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.SphericalUtil
import com.njamb.geodrink.activities.MapActivity
import com.njamb.geodrink.models.Coordinates
import com.njamb.geodrink.utils.NotificationHelper
import com.njamb.geodrink.utils.UsersGeoFire

import java.util.HashSet

class LocationService : Service() {

    private var shouldDisplayNotification: Boolean = false

    private val userNotification = HashSet<String>()
    private val placeNotification = HashSet<String>()

    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLastLocation: Location

    private val mDatabase = FirebaseDatabase.getInstance().reference
    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mLocalBcastManager: LocalBroadcastManager

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            onLocationChanged(locationResult!!.lastLocation)
        }
    }

    override fun onCreate() {
        shouldDisplayNotification = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true)

        mLocalBcastManager = LocalBroadcastManager.getInstance(this)
        if (shouldDisplayNotification) registerForActions()

        mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        mLocationRequest.interval = UPDATE_INTERVAL.toLong()
        mLocationRequest.fastestInterval = FASTEST_INTERVAL.toLong()

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(mLocationRequest, mLocationCallback, null/*Looper*/)
        } catch (e: SecurityException) {
            // this should never happen, unless user disables permission while using app
            stopSelf()
            Toast.makeText(this, "Service stopped because location permission not granted",
                           Toast.LENGTH_SHORT).show()
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        getLastKnownLocation()

        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("It's not bounded service")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (shouldDisplayNotification) mLocalBcastManager.unregisterReceiver(mReceiver)

        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(mLocationCallback)

        stopService(Intent(this, PoiService::class.java))
    }

    private fun getLastKnownLocation() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            mLastLocation = location
                            sendBroadcastLocationUpdated(location.latitude, location.longitude)
                        }
                    }
        } catch (e: SecurityException) {
            // this should never happen, unless user disables permission while using app
            stopSelf()
            Toast.makeText(this, "Service stopped because location permission not granted",
                           Toast.LENGTH_SHORT).show()
        }

    }

    private fun onLocationChanged(loc: Location?) {
        if (loc == null) return

        mLastLocation = loc

        val lat = loc.latitude
        val lng = loc.longitude

        val user = mAuth.currentUser
        if (user != null) {
            updateUserLocationInDatabase(user.uid, lat, lng)
            setGeoFireUserLocation(user.uid, lat, lng)
        }

        sendBroadcastLocationUpdated(lat, lng)
    }

    private fun registerForActions() {
        var filter = IntentFilter(PoiService.ACTION_PLACE_IN_RANGE)
        mLocalBcastManager.registerReceiver(mReceiver, filter)
        filter = IntentFilter(PoiService.ACTION_USER_IN_RANGE)
        mLocalBcastManager.registerReceiver(mReceiver, filter)
    }

    private fun sendBroadcastLocationUpdated(lat: Double, lng: Double) {
        val intent = Intent(MapActivity.ACTION_SET_CENTER)
        intent.putExtra("lat", lat)
                .putExtra("lng", lng)
        mLocalBcastManager.sendBroadcast(intent)
    }

    private fun updateUserLocationInDatabase(id: String, lat: Double, lng: Double) {
        val newLocation = Coordinates(lat, lng)
        mDatabase.child(String.format("users/%s/location", id))
                .setValue(newLocation)
    }

    private fun setGeoFireUserLocation(id: String, lat: Double, lng: Double) {
        val intent = Intent(UsersGeoFire.ACTION_SET_LOCATION)
        intent.putExtra("id", id)
                .putExtra("lat", lat)
                .putExtra("lng", lng)
        mLocalBcastManager.sendBroadcast(intent)
    }

    private fun displayUserNotificationIfInRange(id: String, loc: LatLng) {
        if (userNotification.contains(id)) return

        userNotification.add(id)

        val dist = getDistance(loc)
        if (dist < NOTIFICATION_DISTANCE) {
            NotificationHelper.displayUserNotification(id, this, dist)
        }
    }

    private fun displayPlaceNotificationIfInRange(id: String, loc: LatLng) {
        if (placeNotification.contains(id)) return

        placeNotification.add(id)

        val dist = getDistance(loc)
        if (dist < NOTIFICATION_DISTANCE) {
            NotificationHelper.displayPlaceNotification(id, this, dist)
        }
    }

    private fun getDistance(loc: LatLng): Double {
        val myLoc = LatLng(mLastLocation.latitude, mLastLocation.longitude)
        return SphericalUtil.computeDistanceBetween(myLoc, loc)
    }


    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (PoiService.ACTION_USER_IN_RANGE == action) {
                val c = Coordinates.getCoordinatesFromIntent(intent)
                val key = intent.getStringExtra("key")
                displayUserNotificationIfInRange(key, c.toGoogleCoords())
            } else if (PoiService.ACTION_PLACE_IN_RANGE == action) {
                val c = Coordinates.getCoordinatesFromIntent(intent)
                val key = intent.getStringExtra("key")
                displayPlaceNotificationIfInRange(key, c.toGoogleCoords())
            }
        }
    }

    companion object {
        private val TAG = "LocationService"
        private val UPDATE_INTERVAL = 10000/*ms*/
        private val FASTEST_INTERVAL = 2000/*ms*/
        private val NOTIFICATION_DISTANCE = 500/*m*/
    }
}
