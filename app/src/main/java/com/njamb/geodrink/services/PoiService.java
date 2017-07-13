package com.njamb.geodrink.services;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.njamb.geodrink.activities.MapActivity;
import com.njamb.geodrink.utils.PlacesGeoFire;
import com.njamb.geodrink.utils.UsersGeoFire;

public class PoiService extends Service {
    public static final String ACTION_USER_IN_RANGE = "com.njamb.geodrink.useraddmarker";
    public static final String ACTION_POI_OUT_OF_RANGE = "com.njamb.geodrink.removemarker";
    public static final String ACTION_REPOSITION_POI = "com.njamb.geodrink.repositionmarker";
    public static final String ACTION_PLACE_IN_RANGE = "com.njamb.geodrink.placeaddmarker";

    private UsersGeoFire mUsersGeoFire;
    private PlacesGeoFire mPlacesGeoFire;

    private LocalBroadcastManager mLocalBcastManager;

    public PoiService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        mLocalBcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter(MapActivity.ACTION_SET_CENTER);
        mLocalBcastManager.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(MapActivity.ACTION_SET_RADIUS);
        mLocalBcastManager.registerReceiver(mReceiver, filter);

        mUsersGeoFire = new UsersGeoFire(this, this);
        mPlacesGeoFire = new PlacesGeoFire(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            double rad = intent.getDoubleExtra("rad", 0.25/*km*/);
            GeoLocation loc = new GeoLocation(lat, lng);

            mUsersGeoFire.setCenter(loc);
            mUsersGeoFire.setRadius(rad);

            mPlacesGeoFire.setCenter(loc);
            mPlacesGeoFire.setRadius(rad);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("It's not bounded service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mUsersGeoFire.onDestroy();
        mPlacesGeoFire.onDestroy();

        mLocalBcastManager.unregisterReceiver(mReceiver);
        Toast.makeText(this, "POI service stopped", Toast.LENGTH_SHORT).show();
    }

    public void keyExited(String key) {
        Intent intent = new Intent(PoiService.ACTION_POI_OUT_OF_RANGE);
        intent.putExtra("key", key);
        mLocalBcastManager.sendBroadcast(intent);
    }

    public void keyMoved(String key, GeoLocation location) {
        Intent intent = new Intent(PoiService.ACTION_REPOSITION_POI);
        intent.putExtra("key", key)
                .putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude);

        mLocalBcastManager.sendBroadcast(intent);
    }

    private void setCenter(Bundle extras) {
        double lat = extras.getDouble("lat");
        double lng = extras.getDouble("lng");
        GeoLocation loc = new GeoLocation(lat, lng);

        mUsersGeoFire.setCenter(loc);
        mPlacesGeoFire.setCenter(loc);
    }

    private void setRadius(Bundle extras) {
        double rad = extras.getDouble("rad");

        mUsersGeoFire.setRadius(rad);
        mPlacesGeoFire.setRadius(rad);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MapActivity.ACTION_SET_CENTER.equals(action)) {
                setCenter(intent.getExtras());
            }
            else if (MapActivity.ACTION_SET_RADIUS.equals(action)) {
                setRadius(intent.getExtras());
            }
        }
    };
}
