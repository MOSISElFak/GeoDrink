package com.njamb.geodrink.services;


import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import com.firebase.geofire.GeoLocation;

public class PoiService extends Service {
    public static final String ACTION_ADD_USER_MARKER = "com.njamb.geodrink.useraddmarker";
    public static final String ACTION_REMOVE_MARKER = "com.njamb.geodrink.removemarker";
    public static final String ACTION_REPOSITION_MARKER = "com.njamb.geodrink.repositionmarker";
    public static final String ACTION_ADD_PLACE_MARKER = "com.njamb.geodrink.placeaddmarker";

    private final Binder mBinder = new PoiService.PoiBinder();

    private UsersService mUsersService = null;
    private boolean mBoundUsersService = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            String cname = className.getClassName();
            if (cname.equals(UsersService.class.getName())) {
                UsersService.UsersBinder binder = (UsersService.UsersBinder) service;
                mUsersService = binder.getService();
                mBoundUsersService = true;
            }
            else if (cname.equals(PlacesService.class.getName())) {
                PlacesService.PlacesBinder binder = (PlacesService.PlacesBinder) service;
                mPlacesService = binder.getService();
                mBoundPlacesService = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            String cname = arg0.getClassName();
            if (cname.equals(UsersService.class.getName())) {
                mBoundUsersService = false;
            }
            else if (cname.equals(PlacesService.class.getName())) {
                mBoundPlacesService = false;
            }
        }
    };

    private PlacesService mPlacesService = null;
    private boolean mBoundPlacesService = false;


    public PoiService() {}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Intent usersIntent = new Intent(this, UsersService.class);
        usersIntent.putExtras(intent.getExtras());
        Intent placesIntent = new Intent(this, PlacesService.class);
        placesIntent.putExtras(intent.getExtras());
        bindService(usersIntent, mConnection, Context.BIND_AUTO_CREATE);
        bindService(placesIntent, mConnection, Context.BIND_AUTO_CREATE);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mUsersService.onUnbind(intent);
        mPlacesService.onUnbind(intent);

        return super.onUnbind(intent);
    }

    public void setRadius(double rad) {
        if (mBoundUsersService) {
            mUsersService.setRadius(rad);
        }
        if (mBoundPlacesService) {
            mPlacesService.setRadius(rad);
        }
    }

    public void setLocation(GeoLocation loc) {
        if (mBoundUsersService) {
            mUsersService.setLocation(loc);
        }
        if (mBoundPlacesService) {
            mPlacesService.setLocation(loc);
        }
    }


    public class PoiBinder extends Binder {
        public PoiService getService() {
            return PoiService.this;
        }
    }
}
