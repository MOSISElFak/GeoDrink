package com.njamb.geodrink.services;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

public class PlacesService extends Service implements GeoQueryEventListener {
    public static final String ACTION_ADD_MARKER = "com.njamb.geodrink.placeaddmarker";

    private final Binder mBinder = new PlacesService.PlacesBinder();

    private GeoFire mGeoFirePlaces;
    private GeoQuery mGeoQueryPlaces;


    public PlacesService() {}

    @Override
    public IBinder onBind(Intent intent) {
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        double rad = intent.getDoubleExtra("rad", 0.25/*km*/);

        mGeoFirePlaces = new GeoFire(FirebaseDatabase.getInstance().getReference("placesGeoFire"));
        mGeoQueryPlaces = mGeoFirePlaces.queryAtLocation(new GeoLocation(lat, lng), rad);
        mGeoQueryPlaces.addGeoQueryEventListener(this);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mGeoFirePlaces = null;
        mGeoQueryPlaces = null;

        return super.onUnbind(intent);
    }

    public void setRadius(double rad) {
        mGeoQueryPlaces.setRadius(rad);
    }

    public void setLocation(GeoLocation loc) {
        mGeoQueryPlaces.setCenter(loc);
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Intent intent = new Intent(PlacesService.ACTION_ADD_MARKER);
        intent.putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude)
                .putExtra("key", key);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onKeyExited(String key) {
        Intent intent = new Intent(UsersService.ACTION_REMOVE_MARKER);
        intent.putExtra("key", key);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Intent intent = new Intent(UsersService.ACTION_REPOSITION_MARKER);
        intent.putExtra("key", key)
                .putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //region Unused methods
    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }
    //endregion


    public class PlacesBinder extends Binder {
        public PlacesService getService() {
            return PlacesService.this;
        }
    }
}