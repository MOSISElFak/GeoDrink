package com.njamb.geodrink.Utils;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;


public class UsersGeoQueryListener implements GeoQueryEventListener {
    private GoogleMap mMap;
    private BiMap<String, Marker> mPoiMarkers = HashBiMap.create();
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public UsersGeoQueryListener(GoogleMap map) {
        mMap = map;
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        if (key.equals(userId)) return;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(location.latitude, location.longitude));
        if (mMap != null) {
            Marker marker = mMap.addMarker(markerOptions);
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker.setTag(key);
            mPoiMarkers.put(key, marker);
        }
    }

    @Override
    public void onKeyExited(String key) {
        Marker marker = mPoiMarkers.remove(key);
        if (marker != null) marker.remove();
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        if (key.equals(userId)) return;

        mPoiMarkers.get(key).setPosition(new LatLng(location.latitude, location.longitude));
        //.setAnchor((float) location.latitude, (float) location.longitude);
    }

    //region Unused methods
    @Override public void onGeoQueryReady() {}

    @Override public void onGeoQueryError(DatabaseError error) {}
    //endregion
}
