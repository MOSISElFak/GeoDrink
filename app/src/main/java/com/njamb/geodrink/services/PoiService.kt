package com.njamb.geodrink.services


import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast

import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.activities.MapActivity
import com.njamb.geodrink.utils.PlacesGeoFire
import com.njamb.geodrink.utils.UsersGeoFire

class PoiService : Service() {

    private lateinit var mUsersGeoFire: UsersGeoFire
    private lateinit var mPlacesGeoFire: PlacesGeoFire

    private lateinit var mLocalBcastManager: LocalBroadcastManager

    override fun onCreate() {
        super.onCreate()

        mLocalBcastManager = LocalBroadcastManager.getInstance(this)
        var filter = IntentFilter(MapActivity.ACTION_SET_CENTER)
        mLocalBcastManager.registerReceiver(mReceiver, filter)
        filter = IntentFilter(MapActivity.ACTION_SET_RADIUS)
        mLocalBcastManager.registerReceiver(mReceiver, filter)
        filter = IntentFilter(PoiService.ACTION_ADD_POINTS)
        mLocalBcastManager.registerReceiver(mReceiver, filter)

        mUsersGeoFire = UsersGeoFire(this, this)
        mPlacesGeoFire = PlacesGeoFire(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lng = intent.getDoubleExtra("lng", 0.0)
            val rad = intent.getDoubleExtra("rad", 0.25/*km*/)
            val loc = GeoLocation(lat, lng)

            mUsersGeoFire.setCenter(loc)
            mUsersGeoFire.setRadius(rad)

            mPlacesGeoFire.setCenter(loc)
            mPlacesGeoFire.setRadius(rad)
        }

        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("It's not bounded service")
    }

    override fun onDestroy() {
        super.onDestroy()

        mUsersGeoFire.onDestroy()
        mPlacesGeoFire.onDestroy()

        mLocalBcastManager.unregisterReceiver(mReceiver)
        Toast.makeText(this, "POI service stopped", Toast.LENGTH_SHORT).show()
    }

    fun keyExited(key: String) {
        val intent = Intent(PoiService.ACTION_POI_OUT_OF_RANGE)
        intent.putExtra("key", key)
        mLocalBcastManager.sendBroadcast(intent)
    }

    fun keyMoved(key: String, location: GeoLocation) {
        val intent = Intent(PoiService.ACTION_REPOSITION_POI)
        intent.putExtra("key", key)
                .putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude)

        mLocalBcastManager.sendBroadcast(intent)
    }

    private fun setCenter(extras: Bundle) {
        val lat = extras.getDouble("lat")
        val lng = extras.getDouble("lng")
        val loc = GeoLocation(lat, lng)

        mUsersGeoFire.setCenter(loc)
        mPlacesGeoFire.setCenter(loc)
    }

    private fun setRadius(extras: Bundle) {
        val rad = extras.getDouble("rad")

        mUsersGeoFire.setRadius(rad)
        mPlacesGeoFire.setRadius(rad)
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                MapActivity.ACTION_SET_CENTER -> setCenter(intent.extras)
                MapActivity.ACTION_SET_RADIUS -> setRadius(intent.extras)
                PoiService.ACTION_ADD_POINTS  -> updatePoints(intent.extras.getInt("pts"))
            }
        }
    }

    private fun updatePoints(pts: Int) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val dbRef = FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/points", userId))
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var points: Long = if (dataSnapshot.value == null)
                    0
                else
                    dataSnapshot.value as Long
                points += pts.toLong()
                dbRef.setValue(points)

                Toast.makeText(this@PoiService, "You gained $pts points!", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    companion object {
        val ACTION_USER_IN_RANGE = "com.njamb.geodrink.useraddmarker"
        val ACTION_POI_OUT_OF_RANGE = "com.njamb.geodrink.removemarker"
        val ACTION_REPOSITION_POI = "com.njamb.geodrink.repositionmarker"
        val ACTION_PLACE_IN_RANGE = "com.njamb.geodrink.placeaddmarker"
        val ACTION_ADD_POINTS = "com.njamb.geodrink.addpoints"
    }
}
