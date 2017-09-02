package com.njamb.geodrink.activities

import android.Manifest
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.common.collect.HashBiMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.R
import com.njamb.geodrink.authentication.LoginActivity
import com.njamb.geodrink.bluetooth.AddFriendActivity
import com.njamb.geodrink.fragments.FilterDialogFragment
import com.njamb.geodrink.models.Coordinates
import com.njamb.geodrink.models.MarkerTagModel
import com.njamb.geodrink.services.LocationService
import com.njamb.geodrink.services.PoiService
import com.njamb.geodrink.utils.FilterHelper

import java.util.ArrayList

class MapActivity : AppCompatActivity(), OnMapReadyCallback, FilterDialogFragment.OnCompleteListener {

    // My location
    private var mLocation = GeoLocation(0.0, 0.0)

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    // Map
    private var mMap: GoogleMap? = null
    private var mCircle: Circle? = null
    private val mPoiMarkers = HashBiMap.create<String, Marker>()
    private var mRange = DEFAULT_RANGE_VALUE

    // Seekbar
    private lateinit var mSeekBar: SeekBar

    // Local broadcast manager
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // Camera animation flag:
    private var isCameraAnimated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Load map
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set Toolbar/Action bar
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // Get Firebase refs
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        // Local broadcast manager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Slider for range queries
        mSeekBar = findViewById(R.id.seekBar2) as SeekBar
        mSeekBar.max = SEEKBAR_MAX_VALUE
        mSeekBar.progress = mRange.toInt()
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mRange = progress + SEEKBAR_STEP
                mCircle?.radius = mRange
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                sendBroadcastForRadiusChange(mRange / 1000/*->km*/)
            }
        })

        // FAB for adding place (checking in)
        val fabAddPlace = findViewById(R.id.fab_add_place) as FloatingActionButton
        fabAddPlace.setOnClickListener {
            val intent = Intent(this@MapActivity, CheckInActivity::class.java)
            intent.putExtra("lat", mLocation.latitude)
                    .putExtra("lon", mLocation.longitude)
            startActivity(intent)
        }

        // FAB for filtering
        val fabFilter = findViewById(R.id.fab_filter) as FloatingActionButton
        fabFilter.setOnClickListener {
            val fdf = FilterDialogFragment()
            fdf.show(fragmentManager, "filter")
        }

        registerForActions()

        if (!locationPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun locationPermissionsGranted(): Boolean =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun registerForActions() {
        var filter = IntentFilter(PoiService.ACTION_USER_IN_RANGE)
        localBroadcastManager.registerReceiver(mReceiver, filter)

        filter = IntentFilter(PoiService.ACTION_POI_OUT_OF_RANGE)
        localBroadcastManager.registerReceiver(mReceiver, filter)

        filter = IntentFilter(PoiService.ACTION_REPOSITION_POI)
        localBroadcastManager.registerReceiver(mReceiver, filter)

        filter = IntentFilter(PoiService.ACTION_PLACE_IN_RANGE)
        localBroadcastManager.registerReceiver(mReceiver, filter)

        filter = IntentFilter(MapActivity.ACTION_SET_CENTER)
        localBroadcastManager.registerReceiver(mReceiver, filter)
    }

    private fun startServices() {
        startService(Intent(this, LocationService::class.java))
        startPoiService() // Users & Places
    }

    override fun onStart() {
        super.onStart()

        if (mAuth.currentUser == null) {
            startLoginActivity()
        } else {
            if (locationPermissionsGranted()) {
                startServices()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        checkIfUserLoggedIn()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startServices()
                    setMapMyLocationEnabled()
                } else {
                    Toast.makeText(this, "This app cannot work without location permissions",
                                   Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        localBroadcastManager.unregisterReceiver(mReceiver)

        val enableService = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_service", true)
        if (!enableService && locationPermissionsGranted()) {
            stopService(Intent(this, LocationService::class.java))
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfUserLoggedIn() {
        if (mAuth.currentUser == null) {
            startLoginActivity()
        }
    }

    private fun startPoiService() {
        val rad = if (FilterHelper.rangeQueryEnabled)
            mRange / 1000/*->km*/
        else
            RANGE_QUERY_DISABLED_DISTANCE
        val intent = Intent(this, PoiService::class.java)
        intent.putExtra("lat", mLocation.latitude)
                .putExtra("lng", mLocation.longitude)
                .putExtra("rad", rad)
        startService(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_map, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchBar = menu.findItem(R.id.action_search)

        val searchView = MenuItemCompat.getActionView(searchBar) as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(true)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                FilterHelper.getInstance(mPoiMarkers).filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                FilterHelper.getInstance(mPoiMarkers).filter(newText)
                return true
            }
        })

        searchView.setOnCloseListener {
            FilterHelper.getInstance(mPoiMarkers).filter("") // reset to previous state
            true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_scoreboard -> startActivity(Intent(this, ScoreboardActivity::class.java))
            R.id.action_settings   -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_add        -> {
                val i = Intent(this, AddFriendActivity::class.java)
                i.putExtra("userId", mAuth.currentUser!!.uid)
                startActivity(i)
            }
            R.id.action_profile    -> startProfileActivity(mAuth.currentUser!!.uid)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap?.setOnMarkerClickListener { marker ->
            val tag = (marker.tag as MarkerTagModel?)!!
            if (tag.isUser || tag.isFriend) {
                startProfileActivity(tag.id)
            } else if (tag.isPlace) {
                val intent = Intent(this@MapActivity, DetailsActivity::class.java)
                intent.putExtra("placeId", tag.id)

                startActivity(intent)
            }
            true
        }

        if (locationPermissionsGranted()) {
            setMapMyLocationEnabled()
        }
    }

    private fun setMapMyLocationEnabled() {
        try {
            mMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) { // covers NPE
            Log.e(TAG, e.message)
        }

    }

    private fun drawCircleOnMap(center: LatLng, radius: Double) {
        if (mMap == null || !FilterHelper.rangeQueryEnabled) return

        mCircle = mMap?.addCircle(
                CircleOptions()
                        .center(center)
                        .radius(radius)
        )
        mCircle?.fillColor = Color.argb(30, 255, 0, 0)
        mCircle?.strokeColor = Color.argb(50, 255, 0, 0)
    }

    private fun startLoginActivity() {
        val i = Intent(this, LoginActivity::class.java)
        startActivity(i)
    }

    private fun startProfileActivity(id: String) {
        val i = Intent(this, ProfileActivity::class.java)
        i.putExtra("userId", id)
        startActivity(i)
    }

    private fun sendBroadcastForRadiusChange(rad: Double) {
        val intent = Intent(MapActivity.ACTION_SET_RADIUS)
                .putExtra("rad", rad)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun addUserMarkerOnMap(key: String, position: LatLng) {
        if (mMap == null) return

        val markerOptions = MarkerOptions()
        markerOptions.position(position)
        if (mMap != null) {
            val marker = mMap!!.addMarker(markerOptions)
            marker.tag = MarkerTagModel.createUserTag(key, name="", isFriend=false)
            marker.isVisible = FilterHelper.usersVisible
            setUserMarkerTitle(marker)
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            mPoiMarkers.put(key, marker)

            val userId = mAuth.currentUser!!.uid
            mDatabase.getReference("users/$userId/friends/$key")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.value != null) {
                                addUserPhotoOnMarker(marker)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        }
    }

    private fun addUserPhotoOnMarker(marker: Marker) {
        val tag = (marker.tag as MarkerTagModel?)!!
        tag.setIsFriend()
        mDatabase.getReference("users/${tag.id}/profileUrl")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Glide.with(this@MapActivity)
                                .asBitmap()
                                .load(dataSnapshot.value)
                                .into(object : SimpleTarget<Bitmap>(32, 32) {
                                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource))
                                    }
                                })
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private fun setUserMarkerTitle(marker: Marker) {
        val tag = (marker.tag as MarkerTagModel?)!!
        mDatabase.getReference("users/${tag.id}/fullName")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val name = dataSnapshot.value as String
                        marker.title = name
                        tag.name = name
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private fun addPlaceMarkerOnMap(key: String, position: LatLng) {
        if (mMap == null) return

        val markerOptions = MarkerOptions()
        markerOptions.position(position)
        if (mMap != null) {
            val marker = mMap!!.addMarker(markerOptions)
            marker.tag = MarkerTagModel.createPlaceTag(key, name=""/*at this moment*/)
            marker.isVisible = FilterHelper.placesVisible
            mPoiMarkers.put(key, marker)

            mDatabase.getReference("places/$key/name")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.value != null) {
                                val name = dataSnapshot.value as String
                                val mtm = (marker.tag as MarkerTagModel?)!!
                                mtm.name = name
                                marker.title = name
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        }
    }

    private fun removeMarkerFromMap(key: String) {
        if (mMap == null) return

        val marker = mPoiMarkers.remove(key)
        marker?.remove()
    }

    private fun repositionMarkerOnMap(key: String, position: LatLng) {
        mPoiMarkers[key]?.position = position
    }

    override fun onComplete(checked: ArrayList<String>) {
        val fh = FilterHelper.getInstance(mPoiMarkers)

        if (checked.contains("Range"))
            turnOnRangeFilter()
        else
            turnOffRangeFilter()

//        fh.setUsersVisibility(checked.contains("Users"))
//        fh.setFriendsVisibility(checked.contains("Friends"))
//        fh.setPlacesVisibility(checked.contains("Places"))
        fh.setVisibility(checked.contains("Users")) { it.isUser }
        fh.setVisibility(checked.contains("Friends")) { it.isFriend }
        fh.setVisibility(checked.contains("Places")) { it.isPlace }
    }

    private fun turnOnRangeFilter() {
        FilterHelper.rangeQueryEnabled = true
        if (mCircle != null)
            mCircle!!.isVisible = true
        else
            drawCircleOnMap(LatLng(mLocation.latitude, mLocation.longitude), mRange)
        mSeekBar.visibility = View.VISIBLE
        sendBroadcastForRadiusChange(mRange / 1000/*->km*/)
    }

    private fun turnOffRangeFilter() {
        FilterHelper.rangeQueryEnabled = false
        mCircle?.isVisible = false
        mSeekBar.visibility = View.GONE
        sendBroadcastForRadiusChange(RANGE_QUERY_DISABLED_DISTANCE) // set to something big
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                PoiService.ACTION_USER_IN_RANGE    -> {
                    val c = Coordinates.getCoordinatesFromIntent(intent)
                    val key = intent.getStringExtra("key")
                    addUserMarkerOnMap(key, c.toGoogleCoords())
                }
                PoiService.ACTION_PLACE_IN_RANGE   -> {
                    val c = Coordinates.getCoordinatesFromIntent(intent)
                    val key = intent.getStringExtra("key")
                    addPlaceMarkerOnMap(key, c.toGoogleCoords())
                }
                PoiService.ACTION_POI_OUT_OF_RANGE -> removeMarkerFromMap(intent.getStringExtra("key"))
                PoiService.ACTION_REPOSITION_POI   -> {
                    val c = Coordinates.getCoordinatesFromIntent(intent)
                    val key = intent.getStringExtra("key")
                    repositionMarkerOnMap(key, c.toGoogleCoords())
                }
                MapActivity.ACTION_SET_CENTER      -> {
                    val c = Coordinates.getCoordinatesFromIntent(intent)
                    val center = c.toGoogleCoords()

                    if (mCircle != null)
                        mCircle!!.center = center
                    else
                        drawCircleOnMap(center, mRange)

                    mLocation = GeoLocation(c.lat, c.lng)

                    if (!isCameraAnimated && mMap != null) {
                        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0f))
                        isCameraAnimated = true
                    }
                }
            }
        }
    }

    companion object {

        // Const
        val ACTION_SET_RADIUS = "com.njamb.geodrink.setradius"
        val ACTION_SET_CENTER = "com.njamb.geodrink.setcenter"
        val REQUEST_LOCATION_PERMISSION = 1
        private val TAG = "MapActivity"
        private val SEEKBAR_STEP = 100.0/*m*/
        private val SEEKBAR_MAX_VALUE = 9900/*m*/
        private val DEFAULT_RANGE_VALUE = 500.0/*m*/
        private val RANGE_QUERY_DISABLED_DISTANCE = 10000.0/*km*/
    }
}
