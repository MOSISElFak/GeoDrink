package com.njamb.geodrink.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.services.PoiService;

public class UsersGeoFire implements GeoQueryEventListener {
    public static final String ACTION_SET_LOCATION = "com.njamb.geodrink.setuserlocation";

    private GeoFire mGeoFireUsers;
    private GeoQuery mGeoQueryUsers;
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private Context mContext;
    private PoiService mService;

    public UsersGeoFire(Context c, PoiService ps) {
        mContext = c;
        mService = ps;

        mGeoFireUsers = new GeoFire(FirebaseDatabase.getInstance().getReference("usersGeoFire"));
        mGeoQueryUsers = mGeoFireUsers.queryAtLocation(new GeoLocation(0, 0), 0.1);
        mGeoQueryUsers.addGeoQueryEventListener(this);

        IntentFilter filter = new IntentFilter(UsersGeoFire.ACTION_SET_LOCATION);
        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver(mReceiver, filter);
    }

    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        if (key.equals(userId)) return;

        Intent intent = new Intent(PoiService.ACTION_ADD_USER_MARKER);
        intent.putExtra("lat", location.latitude)
                .putExtra("lng", location.longitude)
                .putExtra("key", key);

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Override
    public void onKeyExited(String key) {
        mService.keyExited(key);
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        if (key.equals(userId)) return;

        mService.keyMoved(key, location);
    }

    //region Unused methods
    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }
    //endregion

    public void setCenter(GeoLocation loc) {
        mGeoQueryUsers.setCenter(loc);
    }

    public void setRadius(double rad) {
        mGeoQueryUsers.setRadius(rad);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsersGeoFire.ACTION_SET_LOCATION.equals(action)) {
                String id = intent.getStringExtra("id");
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);
                GeoLocation loc = new GeoLocation(lat, lng);

                mGeoFireUsers.setLocation(id, loc);
            }
        }
    };
}
