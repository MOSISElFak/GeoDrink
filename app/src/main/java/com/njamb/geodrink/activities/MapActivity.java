package com.njamb.geodrink.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njamb.geodrink.authentication.LoginActivity;
import com.njamb.geodrink.bluetooth.AddFriendActivity;
import com.njamb.geodrink.fragments.FilterDialogFragment;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.models.MarkerTagModel;
import com.njamb.geodrink.R;
import com.njamb.geodrink.services.LocationService;
import com.njamb.geodrink.services.PoiService;
import com.njamb.geodrink.utils.FilterHelper;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        FilterDialogFragment.OnCompleteListener{

    // Const
    public static final String ACTION_SET_RADIUS = "com.njamb.geodrink.setradius";
    public static final String ACTION_SET_CENTER = "com.njamb.geodrink.setcenter";
    public static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String TAG = "MapActivity";
    private static final double SEEKBAR_STEP = 100/*m*/;
    private static final int SEEKBAR_MAX_VALUE = 9900/*m*/;
    private static final double DEFAULT_RANGE_VALUE = 500/*m*/;
    private static final double RANGE_QUERY_DISABLED_DISTANCE = 10000/*km*/;

    // My location
    private GeoLocation mLocation = new GeoLocation(0, 0);

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Map
    private GoogleMap mMap = null;
    private Circle mCircle = null;
    private BiMap<String, Marker> mPoiMarkers = HashBiMap.create();
    private double mRange = DEFAULT_RANGE_VALUE;

    // Seekbar
    private SeekBar mSeekBar;

    // Local broadcast manager
    private LocalBroadcastManager localBroadcastManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set Toolbar/Action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get Firebase refs
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Local broadcast manager
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Slider for range queries
        mSeekBar = (SeekBar) findViewById(R.id.seekBar2);
        mSeekBar.setMax(SEEKBAR_MAX_VALUE);
        mSeekBar.setProgress((int)mRange);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRange = progress + SEEKBAR_STEP;
                mCircle.setRadius(mRange);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBroadcastForRadiusChange(mRange/1000/*->km*/);
            }
        });

        // FAB for adding place (checking in)
        FloatingActionButton fabAddPlace = (FloatingActionButton) findViewById(R.id.fab_add_place);
        fabAddPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, CheckInActivity.class);
                intent.putExtra("lat", mLocation.latitude)
                        .putExtra("lon", mLocation.longitude);
                startActivity(intent);
            }
        });

        // FAB for filtering
        FloatingActionButton fabFilter = (FloatingActionButton) findViewById(R.id.fab_filter);
        fabFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterDialogFragment fdf = new FilterDialogFragment();
                fdf.show(getFragmentManager(), "filter");
            }
        });

        registerForActions();

        if (!locationPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION
            );
        }
    }

    private boolean locationPermissionsGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void registerForActions() {
        IntentFilter filter = new IntentFilter(PoiService.ACTION_USER_IN_RANGE);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(PoiService.ACTION_POI_OUT_OF_RANGE);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(PoiService.ACTION_REPOSITION_POI);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(PoiService.ACTION_PLACE_IN_RANGE);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(MapActivity.ACTION_SET_CENTER);
        localBroadcastManager.registerReceiver(mReceiver, filter);
    }

    private void startServices() {
        startService(new Intent(this, LocationService.class));
        startPoiService(); // Users & Places
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) {
            startLoginActivity();
        }
        else {
            if (locationPermissionsGranted()) {
                startServices();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIfUserLoggedIn();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startServices();
                    setMapMyLocationEnabled();
                }
                else {
                    Toast.makeText(this, "This app cannot work without location permissions",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        localBroadcastManager.unregisterReceiver(mReceiver);

        boolean enableService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true);
        if (!enableService && locationPermissionsGranted()) {
            stopService(new Intent(this, LocationService.class));
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfUserLoggedIn() {
        if (mAuth.getCurrentUser() == null) {
            startLoginActivity();
        }
    }

    private void startPoiService() {
        double rad = FilterHelper.rangeQueryEnabled
                ? mRange/1000/*->km*/
                : RANGE_QUERY_DISABLED_DISTANCE;
        Intent intent = new Intent(this, PoiService.class);
        intent.putExtra("lat", mLocation.latitude)
                .putExtra("lng", mLocation.longitude)
                .putExtra("rad", rad);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchBar = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchBar);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                FilterHelper.getInstance(mPoiMarkers).filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                FilterHelper.getInstance(mPoiMarkers).filter(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                FilterHelper.getInstance(mPoiMarkers).filter(""); // reset to previous state
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_scoreboard: {
                startActivity(new Intent(this, ScoreboardActivity.class));
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
//            case R.id.action_search: {
//                // ToDo: Search field (hide icons), show only magnifying glass (quick search).
//                // On next click on it (thorough search) - new activity.
//                break;
//            }
            case R.id.action_add: {
                Intent i = new Intent(this, AddFriendActivity.class);
                i.putExtra("userId", mAuth.getCurrentUser().getUid());
                startActivity(i);
                break;
            }
            case R.id.action_profile: {
                startProfileActivity(mAuth.getCurrentUser().getUid());
                break;
            }
            case R.id.action_details: {
                Intent i = new Intent(this, DetailsActivity.class);
                startActivity(i);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                MarkerTagModel tag = (MarkerTagModel) marker.getTag();
                assert tag != null;
                if (tag.isUser || tag.isFriend) {
                    startProfileActivity(tag.id);
                }
                else if (tag.isPlace) {
                    // TODO: show place info
                    Toast.makeText(MapActivity.this, marker.getTag().toString(), Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });

        if (locationPermissionsGranted()) {
            setMapMyLocationEnabled();
        }
    }

    private void setMapMyLocationEnabled() {
        try {
            mMap.setMyLocationEnabled(true);
        }
        catch (SecurityException e) { // covers NPE
            Log.e(TAG, e.getMessage());
        }
    }

    private void drawCircleOnMap(LatLng center, double radius) {
        if (mMap == null || !FilterHelper.rangeQueryEnabled) return;

        mCircle = mMap.addCircle(
                new CircleOptions()
                        .center(center)
                        .radius(radius)
        );
        mCircle.setFillColor(Color.argb(30, 255, 0, 0));
        mCircle.setStrokeColor(Color.argb(50, 255, 0, 0));
    }

    private void startLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    private void startProfileActivity(@NonNull String id) {
        Intent i = new Intent(this, ProfileActivity.class);
        i.putExtra("userId", id);
        startActivity(i);
    }

    private void sendBroadcastForRadiusChange(double rad) {
        Intent intent = new Intent(MapActivity.ACTION_SET_RADIUS)
                .putExtra("rad", rad);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void addUserMarkerOnMap(final String key, LatLng position) {
        if (mMap == null) return;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        if (mMap != null) {
            final Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(MarkerTagModel.createUserTag(key, null/*at this moment*/, false));
            marker.setVisible(FilterHelper.usersVisible);
            setUserMarkerTitle(marker);
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            mPoiMarkers.put(key, marker);

            String userId = mAuth.getCurrentUser().getUid();
            mDatabase.getReference(String.format("users/%s/friends/%s", userId, key))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                addUserPhotoOnMarker(marker);
                            }
                        }

                        @Override public void onCancelled(DatabaseError databaseError) {}
                    });
        }
    }

    private void addUserPhotoOnMarker(final Marker marker) {
        final MarkerTagModel tag = (MarkerTagModel) marker.getTag();
        assert tag != null;
        tag.setIsFriend();
        mDatabase.getReference(String.format("users/%s/profileUrl", tag.id))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Glide.with(MapActivity.this)
                                .asBitmap()
                                .load(dataSnapshot.getValue())
                                .into(new SimpleTarget<Bitmap>(32,32) {
                                    @Override
                                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource));
                                    }
                                });
                    }

                    @Override public void onCancelled(DatabaseError databaseError) {}
                });
    }

    private void setUserMarkerTitle(final Marker marker) {
        final MarkerTagModel tag = (MarkerTagModel) marker.getTag();
        assert tag != null;
        mDatabase.getReference(String.format("users/%s/fullName", tag.id))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String name = (String) dataSnapshot.getValue();
                        marker.setTitle(name);
                        tag.name = name;
                    }

                    @Override public void onCancelled(DatabaseError databaseError) {}
                });
    }

    private void addPlaceMarkerOnMap(final String key, LatLng position) {
        // TODO: add place marker on map
    }

    private void removeMarkerFromMap(String key) {
        if (mMap == null) return;

        Marker marker = mPoiMarkers.remove(key);
        if (marker != null) marker.remove();
    }

    private void repositionMarkerOnMap(String key, LatLng position) {
        mPoiMarkers.get(key).setPosition(position);
    }

    @Override
    public void onComplete(ArrayList<String> checked) {
        FilterHelper fh = FilterHelper.getInstance(mPoiMarkers);

        if (checked.contains("Range")) turnOnRangeFilter();
        else turnOffRangeFilter();

        fh.setUsersVisibility(checked.contains("Users"));
        fh.setFriendsVisibility(checked.contains("Friends"));
        fh.setPlacesVisibility(checked.contains("Places"));
    }

    private void turnOnRangeFilter() {
        FilterHelper.rangeQueryEnabled = true;
        if (mCircle != null) mCircle.setVisible(true);
        else drawCircleOnMap(new LatLng(mLocation.latitude, mLocation.longitude), mRange);
        mSeekBar.setVisibility(View.VISIBLE);
        sendBroadcastForRadiusChange(mRange/1000/*->km*/);
    }

    private void turnOffRangeFilter() {
        FilterHelper.rangeQueryEnabled = false;
        mCircle.setVisible(false);
        mSeekBar.setVisibility(View.GONE);
        sendBroadcastForRadiusChange(RANGE_QUERY_DISABLED_DISTANCE); // set to something big
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PoiService.ACTION_USER_IN_RANGE.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                String key = intent.getStringExtra("key");
                addUserMarkerOnMap(key, c.toGoogleCoords());
            }
            else if (PoiService.ACTION_PLACE_IN_RANGE.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                String key = intent.getStringExtra("key");
                addPlaceMarkerOnMap(key, c.toGoogleCoords());
            }
            else if (PoiService.ACTION_POI_OUT_OF_RANGE.equals(action)) {
                removeMarkerFromMap(intent.getStringExtra("key"));
            }
            else if (PoiService.ACTION_REPOSITION_POI.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                String key = intent.getStringExtra("key");
                repositionMarkerOnMap(key, c.toGoogleCoords());
            }
            else if (MapActivity.ACTION_SET_CENTER.equals(action)) {
                Coordinates c = Coordinates.getCoordinatesFromIntent(intent);
                LatLng center = c.toGoogleCoords();

                if (mCircle != null) mCircle.setCenter(center);
                else drawCircleOnMap(center, mRange);

                mLocation = new GeoLocation(c.lat, c.lng);

                if (mMap != null) {
                    float currZoom = mMap.getCameraPosition().zoom;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, Math.max(15.0f, currZoom)));
                }
            }
        }
    };
}
