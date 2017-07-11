package com.njamb.geodrink.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.njamb.geodrink.models.MarkerTagModel;
import com.njamb.geodrink.R;
import com.njamb.geodrink.services.BackgroundService;
import com.njamb.geodrink.services.PlacesService;
import com.njamb.geodrink.services.PoiService;
import com.njamb.geodrink.services.UsersService;
import com.njamb.geodrink.utils.FilterHelper;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        LocationListener,
        FilterDialogFragment.OnCompleteListener{

    // Const
    private static final String TAG = "MapActivity";
    public static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final double SEEKBAR_STEP = 100/*m*/;
    private static final int SEEKBAR_MAX_VALUE = 9900/*m*/;
    private static final double DEFAULT_RANGE_VALUE = 500/*m*/;

    // My location
    private GeoLocation mLocation = new GeoLocation(0, 0);

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    // Map
    private GoogleMap mMap = null;
    private Circle mCircle;
    private BiMap<String, Marker> mPoiMarkers = HashBiMap.create();
    private double mRange = DEFAULT_RANGE_VALUE;

    private LocationManager mLocationManager;
    private String mLocationProvider;

    // POI service
    private PoiService mPoiService = null;
    private boolean mBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        mLocationProvider = mLocationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location loc = mLocationManager.getLastKnownLocation(mLocationProvider);
        if (loc != null) {
            onLocationChanged(loc);
        }

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar2);
        seekBar.setMax(SEEKBAR_MAX_VALUE);
        seekBar.setProgress((int)mRange);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRange = progress + SEEKBAR_STEP;
                mCircle.setRadius(mRange);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPoiService != null) mPoiService.setRadius(mRange/1000/*->km*/);
            }
        });

        FloatingActionButton fabAddPlace = (FloatingActionButton) findViewById(R.id.fab_add_place);
        fabAddPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapActivity.this, CheckInActivity.class);
                startActivity(i);
            }
        });

        FloatingActionButton fabFilter = (FloatingActionButton) findViewById(R.id.fab_filter);
        fabFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterDialogFragment fdf = new FilterDialogFragment();
                fdf.show(getFragmentManager(), "filter");
            }
        });

        // Start background location service
        Intent intent = new Intent(this, BackgroundService.class);
        startService(intent);
    }

    private void registerForActions() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter(UsersService.ACTION_ADD_MARKER);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(UsersService.ACTION_REMOVE_MARKER);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(UsersService.ACTION_REPOSITION_MARKER);
        localBroadcastManager.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(PlacesService.ACTION_ADD_MARKER);
        localBroadcastManager.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) {
            startLoginActivity();
        }
        else {
            registerForActions();

            Intent intent = new Intent(this, PoiService.class);
            intent.putExtra("lat", mLocation.latitude)
                    .putExtra("lng", mLocation.longitude)
                    .putExtra("rad", mRange/1000/*->km*/);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIfUserLoggedIn();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(mLocationProvider, 400, 1.0f, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLocationManager.removeUpdates(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        if (mMap != null) {
            mMap.clear();
            mPoiMarkers.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        boolean enableService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true);
        if (!enableService) {
            stopService(new Intent(this, BackgroundService.class));
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfUserLoggedIn() {
        if (mAuth.getCurrentUser() == null) {
            startLoginActivity();
        }
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

        // TODO: Videti sta treba uraditi sa iskomentarisanim kodom ispod.
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                // TODO: if only 1 query displayed, automatically filter for that person
//                // TODO: else: open new activity with provided searches for user to select to show on mMap
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                // TODO: filter through friends list and display only those who match newText
//                return false;
//            }
//        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
            case R.id.action_search: {
                // ToDo: Search field (hide icons), show only magnifying glass (quick search).
                // On next click on it (thorough search) - new activity.
                break;
            }
//            case R.id.action_radius: {
//                seekBar.setVisibility(seekBar.getVisibility() == View.VISIBLE ?
//                        View.INVISIBLE : View.VISIBLE);
//                if (item.getTitle().toString().toLowerCase().equals("change radius")) {
//                    item.setTitle("Done Changing");
//                }
//                else {
//                    item.setTitle("Change Radius");
//                }
//                break;
//            }
            case R.id.action_add: {
                Intent i = new Intent(this, AddFriendActivity.class);
                i.putExtra("userId", mAuth.getCurrentUser().getUid());
                startActivity(i);
                break;
            }
            case R.id.action_profile: {
                Intent i = new Intent(this, ProfileActivity.class);
                i.putExtra("userId", mAuth.getCurrentUser().getUid());
                startActivity(i);
                break;
            }
//            case R.id.action_login: {
//                startLoginActivity();
//                break;
//            }
//            case R.id.action_checkin: {
//                Intent i = new Intent(this, CheckInActivity.class);
//                startActivity(i);
//                break;
//            }
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
                // TODO: show user/place info
                Toast.makeText(MapActivity.this, marker.getTag().toString(), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);

            return;
        }
        mMap.setMyLocationEnabled(true);
        LocationServices.getFusedLocationProviderClient(getBaseContext())
                .getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
                        mCircle = mMap.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .radius(mRange)
                        );
                        mCircle.setFillColor(Color.argb(30, 255, 0, 0));
                        mCircle.setStrokeColor(Color.argb(50, 255, 0, 0));
                    }
                });
    }

    private void startLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng center = new LatLng(lat, lng);

        if (mCircle != null) {
            mCircle.setCenter(center);
        }

        mLocation = new GeoLocation(lat, lng);

        if (mPoiService != null) mPoiService.setLocation(new GeoLocation(lat, lng));

        if (mMap != null) {
            float currZoom = mMap.getCameraPosition().zoom;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, Math.max(15.0f, currZoom)));
        }
    }

    //region Unused methods
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override public void onProviderEnabled(String provider) {}

    @Override public void onProviderDisabled(String provider) {}
    //endregion

    private void addUserMarkerOnMap(final String key, LatLng position) {
        if (mMap == null) return;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        if (mMap != null) {
            final Marker marker = mMap.addMarker(markerOptions);
            marker.setTag(MarkerTagModel.createUserTag(key, null/*at this moment*/, false));
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
        // TODO: display range seek bar & red circle
    }

    private void turnOffRangeFilter() {
        // TODO: hide range seek bar & red circle & set range to big value
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (className.getClassName().equals(PoiService.class.getName())) {
                PoiService.PoiBinder binder = (PoiService.PoiBinder) service;
                mPoiService = binder.getService();
                mBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (arg0.getClassName().equals(PoiService.class.getName())) {
                mBound = false;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mBound) return;

            String action = intent.getAction();
            if (UsersService.ACTION_ADD_MARKER.equals(action)) {
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);
                String key = intent.getStringExtra("key");
                addUserMarkerOnMap(key, new LatLng(lat, lng));
            }
            else if (PlacesService.ACTION_ADD_MARKER.equals(action)) {
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);
                String key = intent.getStringExtra("key");
                addPlaceMarkerOnMap(key, new LatLng(lat, lng));
            }
            else if (UsersService.ACTION_REMOVE_MARKER.equals(action)) {
                // covers place marker removal
                removeMarkerFromMap(intent.getStringExtra("key"));
            }
            else if (UsersService.ACTION_REPOSITION_MARKER.equals(action)) {
                // covers place marker repositioning
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);
                String key = intent.getStringExtra("key");
                repositionMarkerOnMap(key, new LatLng(lat, lng));
            }
        }
    };
}
