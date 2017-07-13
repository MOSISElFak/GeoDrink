package com.njamb.geodrink.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.activities.MapActivity;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.utils.UsersGeoFire;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int UPDATE_INTERVAL = 10000/*ms*/;
    private static final int FASTEST_INTERVAL = 2000/*ms*/;

    private LocationRequest mLocationRequest;
    private NotificationCompat.Builder mNotificationBuilder;

    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            onLocationChanged(locationResult.getLastLocation());
        }
    };

    public LocationService() {}

    @Override
    public void onCreate() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(mLocationRequest, mLocationCallback, null/*Looper*/);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        getLastKnownLocation();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("It's not bounded service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(mLocationCallback);

        stopService(new Intent(this, PoiService.class));
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(this)
                .getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            sendBroadcastLocationUpdated(location.getLatitude(), location.getLongitude());
                        }
                    }
                });
    }

    private void onLocationChanged(Location loc) {
        if (loc == null) return;

        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateUserLocationInDatabase(user.getUid(), lat, lng);
            setGeoFireUserLocation(user.getUid(), lat, lng);
        }

        sendBroadcastLocationUpdated(lat, lng);
    }

    private void sendBroadcastLocationUpdated(double lat, double lng) {
        Intent intent = new Intent(MapActivity.ACTION_SET_CENTER);
        intent.putExtra("lat", lat)
                .putExtra("lng", lng);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateUserLocationInDatabase(String id, double lat, double lng) {
        Coordinates newLocation = new Coordinates(lat, lng);
        mDatabase.child(String.format("users/%s/location", id))
                .setValue(newLocation);
    }

    private void setGeoFireUserLocation(String id, double lat, double lng) {
        Intent intent = new Intent(UsersGeoFire.ACTION_SET_LOCATION);
        intent.putExtra("id", id)
                .putExtra("lat", lat)
                .putExtra("lng", lng);
        LocalBroadcastManager.getInstance(LocationService.this)
                .sendBroadcast(intent);
    }
}
