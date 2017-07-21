package com.njamb.geodrink.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.njamb.geodrink.R;
import com.njamb.geodrink.activities.MapActivity;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.models.User;
import com.njamb.geodrink.utils.NotificationHelper;
import com.njamb.geodrink.utils.UsersGeoFire;

import java.util.HashSet;
import java.util.Set;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int UPDATE_INTERVAL = 10000/*ms*/;
    private static final int FASTEST_INTERVAL = 2000/*ms*/;
    private static final int NOTIFICATION_DISTANCE = 500/*m*/;

    private boolean shouldDisplayNotification;

    private Set<String> userNotification = new HashSet<>();
    private Set<String> placeNotification = new HashSet<>();

    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private LocalBroadcastManager mLocalBcastManager;

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            onLocationChanged(locationResult.getLastLocation());
        }
    };

    public LocationService() {}

    @Override
    public void onCreate() {
        shouldDisplayNotification = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true);

        mLocalBcastManager = LocalBroadcastManager.getInstance(this);
        if (shouldDisplayNotification) registerForActions();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(mLocationRequest, mLocationCallback, null/*Looper*/);
        }
        catch (SecurityException e) {
            // this should never happen, unless user disables permission while using app
            stopSelf();
            Toast.makeText(this, "Service stopped because location permission not granted",
                    Toast.LENGTH_SHORT).show();
        }
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

        if (shouldDisplayNotification) mLocalBcastManager.unregisterReceiver(mReceiver);

        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(mLocationCallback);

        stopService(new Intent(this, PoiService.class));
    }

    private void getLastKnownLocation() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                    .getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                mLastLocation = location;
                                sendBroadcastLocationUpdated(location.getLatitude(), location.getLongitude());
                            }
                        }
                    });
        }
        catch (SecurityException e) {
            // this should never happen, unless user disables permission while using app
            stopSelf();
            Toast.makeText(this, "Service stopped because location permission not granted",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onLocationChanged(Location loc) {
        if (loc == null) return;

        mLastLocation = loc;

        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUserLocationInDatabase(user.getUid(), lat, lng);
            setGeoFireUserLocation(user.getUid(), lat, lng);
        }

        sendBroadcastLocationUpdated(lat, lng);
    }

    private void registerForActions() {
        IntentFilter filter = new IntentFilter(PoiService.ACTION_PLACE_IN_RANGE);
        mLocalBcastManager.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(PoiService.ACTION_USER_IN_RANGE);
        mLocalBcastManager.registerReceiver(mReceiver, filter);
    }

    private void sendBroadcastLocationUpdated(double lat, double lng) {
        Intent intent = new Intent(MapActivity.ACTION_SET_CENTER);
        intent.putExtra("lat", lat)
                .putExtra("lng", lng);
        mLocalBcastManager.sendBroadcast(intent);
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
        mLocalBcastManager.sendBroadcast(intent);
    }

    private void displayUserNotificationIfInRange(String id, LatLng loc) {
        if (userNotification.contains(id)) return;

        userNotification.add(id);

        double dist = getDistance(loc);
        if (dist < NOTIFICATION_DISTANCE) {
            NotificationHelper.displayUserNotification(id, this, dist);
        }
    }

    private void displayPlaceNotificationIfInRange(String id, LatLng loc) {
        if (placeNotification.contains(id)) return;

        placeNotification.add(id);

        final double dist = getDistance(loc);
        if (dist < NOTIFICATION_DISTANCE) {
            NotificationHelper.displayPlaceNotification(id, this, dist);
        }
    }

    private double getDistance(LatLng loc) {
        LatLng myLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        return SphericalUtil.computeDistanceBetween(myLoc, loc);
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PoiService.ACTION_USER_IN_RANGE.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                String key = intent.getStringExtra("key");
                displayUserNotificationIfInRange(key, c.toGoogleCoords());
            }
            else if (PoiService.ACTION_PLACE_IN_RANGE.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                String key = intent.getStringExtra("key");
                displayPlaceNotificationIfInRange(key, c.toGoogleCoords());
            }
        }
    };
}
