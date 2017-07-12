package com.njamb.geodrink.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.utils.UsersGeoFire;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int LOCATION_INTERVAL = 1000 /*ms*/;
    private static final float LOCATION_DISTANCE = 100f /*m*/;

    private NotificationCompat.Builder mNotificationBuilder;

    private LocationManager mLocationManager;
    LocationListener[] mLocationListeners = new LocationListener[]{
            new BackgroundLocationListener(LocationManager.GPS_PROVIDER),
            new BackgroundLocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public LocationService() {
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

        stopService(new Intent(this, PoiService.class));
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext()
                    .getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("It's not bounded service");
    }


    /* BACKGROUND LOCATION LISTENER */
    private class BackgroundLocationListener implements LocationListener {
        private Location mLastLocation;
        private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        private FirebaseAuth mAuth = FirebaseAuth.getInstance();

        public BackgroundLocationListener(String provider) {
            setLastLocation(new Location(provider));
        }

        public void setLastLocation(Location newLoc) {
            mLastLocation = newLoc;
            double lat = mLastLocation.getLatitude();
            double lng = mLastLocation.getLongitude();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                try {
                    Coordinates newLocation = new Coordinates(lat, lng);
                    mDatabase.child(String.format("users/%s/location", user.getUid()))
                            .setValue(newLocation);
                    setGeoFireUserLocation(user.getUid(), lat, lng);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }

        private void setGeoFireUserLocation(String id, double lat, double lng) {
            Intent intent = new Intent(UsersGeoFire.ACTION_SET_LOCATION);
            intent.putExtra("id", id)
                    .putExtra("lat", lat)
                    .putExtra("lng", lng);
            LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
        }

        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(LocationService.this, "Location changed", Toast.LENGTH_SHORT).show();
            setLastLocation(location);
        }

        //region Unused LocationListener methods
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override public void onProviderEnabled(String provider) {}

        @Override public void onProviderDisabled(String provider) {}
        //endregion
    }
}
