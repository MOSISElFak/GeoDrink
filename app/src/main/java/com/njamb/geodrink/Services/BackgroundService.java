package com.njamb.geodrink.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.Models.Coordinates;

public class BackgroundService extends Service implements GeoQueryEventListener {
    private static final String TAG = "BackgroundService";
    private static final double RANGE = 0.5 /*km*/;
    private static final int LOCATION_INTERVAL = 1000 /*ms*/;
    private static final float LOCATION_DISTANCE = 100f /*m*/;
    
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private GeoFire mGeoFirePlaces = new GeoFire(mDatabase.child("placesGeoFire"));
    private GeoFire mGeoFireUsers = new GeoFire(mDatabase.child("usersGeoFire"));
    private GeoQuery mGeoQuery = mGeoFirePlaces.queryAtLocation(new GeoLocation(0, 0), RANGE);
    private NotificationCompat.Builder mNotificationBuilder;

    private LocationManager mLocationManager;
    LocationListener[] mLocationListeners = new LocationListener[]{
            new BackgroundLocationListener(LocationManager.GPS_PROVIDER),
            new BackgroundLocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public BackgroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.e(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.e(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "gps provider does not exist " + ex.getMessage());
        }

        mAuth = FirebaseAuth.getInstance();
        mGeoQuery.addGeoQueryEventListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLocationManager != null) {
            for (LocationListener listener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(listener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext()
                    .getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        // TODO: Display notification when new place nearby
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /* BACKGROUND LOCATION LISTENER */
    private class BackgroundLocationListener implements LocationListener {
        private Location mLastLocation;

        public BackgroundLocationListener(String provider) {
            setLastLocation(new Location(provider));
        }

        public void setLastLocation(Location newLoc) {
            mLastLocation = newLoc;
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();
            mGeoQuery.setCenter(new GeoLocation(lat, lng));
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                try {
                    Coordinates newLocation = new Coordinates(lat, lng);
                    mDatabase.child("users").child(user.getUid()).child("location")
                            .setValue(newLocation);
                    mGeoFireUsers.setLocation(user.getUid(), new GeoLocation(lat, lng));
                } catch (Exception e) {
                    Log.e("EXC", e.getMessage());
                }
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(BackgroundService.this, "Location changed", Toast.LENGTH_SHORT).show();
            setLastLocation(location);
        }

        //region Unused LocationListener methods
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override public void onProviderEnabled(String provider) {}

        @Override public void onProviderDisabled(String provider) {}
        //endregion
    }


    //region Unused GeoQueryEventListener methods
    @Override public void onKeyExited(String key) {}

    @Override public void onKeyMoved(String key, GeoLocation location) {}

    @Override public void onGeoQueryReady() {}

    @Override public void onGeoQueryError(DatabaseError error) {}
    //endregion
}
