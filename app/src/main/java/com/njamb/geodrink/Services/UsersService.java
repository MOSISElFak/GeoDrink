package com.njamb.geodrink.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

public class UsersService extends Service implements GeoQueryEventListener {
    public static final String ACTION_ADD_MARKER = "com.njamb.geodrink.addmarker";
    public static final String ACTION_REMOVE_MARKER = "com.njamb.geodrink.removemarker";
    public static final String ACTION_REPOSITION_MARKER = "com.njamb.geodrink.repositionmarker";

    private final Binder mBinder = new UsersBinder();

    private GeoFire mGeoFireUsers;
    private GeoQuery mGeoQueryUsers;
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


    public UsersService() {}

    @Override
    public IBinder onBind(Intent intent) {
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        double rad = intent.getDoubleExtra("rad", 0.25/*km*/);

        mGeoFireUsers = new GeoFire(FirebaseDatabase.getInstance().getReference("usersGeoFire"));
        mGeoQueryUsers = mGeoFireUsers.queryAtLocation(new GeoLocation(lat, lng), rad);
        mGeoQueryUsers.addGeoQueryEventListener(this);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mGeoFireUsers = null;
        mGeoQueryUsers = null;

        return super.onUnbind(intent);
    }

    public void setRadius(double rad) {
        mGeoQueryUsers.setRadius(rad);
    }

    public void setLocation(GeoLocation loc) {
        mGeoQueryUsers.setCenter(loc);
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        if (key.equals(userId)) return;

        Intent intent = new Intent(UsersService.ACTION_ADD_MARKER);
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
        if (key.equals(userId)) return;

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


    public class UsersBinder extends Binder {
        public UsersService getService() {
            return UsersService.this;
        }
    }
}
