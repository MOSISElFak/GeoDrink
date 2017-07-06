package com.njamb.geodrink.Activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.Authentication.LoginActivity;
import com.njamb.geodrink.Bluetooth.AddFriendActivity;
import com.njamb.geodrink.R;
import com.njamb.geodrink.Services.BackgroundService;
import com.njamb.geodrink.Utils.UsersGeoQueryListener;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GeoQueryEventListener,
        LocationListener {

    // Const
    private static final String TAG = "MapActivity";
    public static final int REQUEST_LOCATION_PERMISSION = 1;
    private final String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;

    // My location
    private GeoLocation mLocation = new GeoLocation(0, 0);

    // Firebase auth
    private FirebaseAuth mAuth;

    // Map
    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private Circle mCircle;
    private BiMap<String, Marker> mPoiMarkers = HashBiMap.create();

    // GeoFire
    private GeoFire mGeoFireUsers;
    private GeoFire mGeoFirePlaces;
    private GeoQuery mGeoQueryUsers = null;
    private GeoQuery mGeoQueryPlaces;
    private double mRange = 1 /*km*/;
    private LocationManager mLocationManager;
    private String mLocationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();

        mGeoFirePlaces = new GeoFire(FirebaseDatabase.getInstance().getReference().child("placesGeoFire"));
        mGeoFireUsers = new GeoFire(FirebaseDatabase.getInstance().getReference().child("usersGeoFire"));
        mGeoQueryPlaces = mGeoFirePlaces
                .queryAtLocation(new GeoLocation(mLocation.latitude, mLocation.longitude), mRange);
        mGeoQueryPlaces.addGeoQueryEventListener(this);

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

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkIfUserLoggedIn();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        userId = user != null ? user.getUid() : null;
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
//                // TODO: else: open new activity with provided searches for user to select to show on map
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
            case R.id.action_checkin: {
                Intent i = new Intent(this, CheckInActivity.class);
                startActivity(i);
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
        this.map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mGeoQueryUsers = mGeoFireUsers.queryAtLocation(mLocation, 1000/*km*/);
        mGeoQueryUsers.addGeoQueryEventListener(new UsersGeoQueryListener(map));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);

            return;
        }
        map.setMyLocationEnabled(true);

        LocationServices.getFusedLocationProviderClient(this)
                .getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, map.getCameraPosition().zoom));
                        mCircle = map.addCircle(
                                new CircleOptions()
                                        .center(center)
                                        .radius(mRange * 1000/*m*/)
                        );
                        mCircle.setFillColor(Color.argb(60, 255, 0, 0));
                        mCircle.setStrokeColor(Color.argb(60, 255, 0, 0));
                    }
                });
        startBackgroundServiceIfEnabled();
    }

    private void startLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    private void startBackgroundServiceIfEnabled() {
        boolean enableService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true);
        if (enableService) {
            Intent intent = new Intent(this, BackgroundService.class);
            startService(intent);
        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(location.latitude, location.longitude));
        if (map != null) {
            Marker marker = map.addMarker(markerOptions);
            marker.setTag("Place " + key);
            mPoiMarkers.put(key, marker);
        }
    }

    @Override
    public void onKeyExited(String key) {
        Marker marker = mPoiMarkers.remove(key);
        marker.remove();
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        mPoiMarkers.get(key).setAnchor((float) location.latitude, (float) location.longitude);
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        if (mCircle != null) {
            mCircle.setCenter(new LatLng(lat, lng));
        }
        mGeoQueryPlaces.setCenter(new GeoLocation(lat, lng));
        if (mGeoQueryUsers != null) mGeoQueryUsers.setCenter(new GeoLocation(lat, lng));
        mLocation = new GeoLocation(lat, lng);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
