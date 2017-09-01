package com.njamb.geodrink.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.R
import com.njamb.geodrink.models.Place
import com.njamb.geodrink.models.User

class DetailsActivity : AppCompatActivity() {

    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mDate: TextView
    private lateinit var mTime: TextView
    private lateinit var mPlaceName: TextView
    private lateinit var mProfileName: TextView
    private lateinit var mReturn: Button
    private lateinit var mProfileImage: ImageView
    private lateinit var mPlaceImage: ImageView
    private lateinit var mPlace: Place

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_page)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val returnBtn = findViewById(R.id.details_btn_return) as Button
        returnBtn.setOnClickListener { finish() }

        // Initialize components:
        getReferences()

        // Set listeners:
        setListeners()
    }

    private fun getReferences() {
        mDatabase = FirebaseDatabase.getInstance()

        mPlaceName = findViewById(R.id.details_tv_placeName) as TextView
        mProfileName = findViewById(R.id.details_tv_userName) as TextView
        mDate = findViewById(R.id.details_tv_date) as TextView
        mTime = findViewById(R.id.details_tv_time) as TextView
        mProfileImage = findViewById(R.id.details_iv_profilepic) as ImageView
        mPlaceImage = findViewById(R.id.details_iv_photo) as ImageView
        mReturn = findViewById(R.id.details_btn_return) as Button
    }

    private fun setListeners() {
        // Return button:
        mReturn.setOnClickListener { finish() }

        // Get parsed placeId:
        val intent = intent
        val placeId = intent.getStringExtra("placeId")

        Log.v("+nj", "PRE mPlacesRef")
        // Map the place data:
        mDatabase.getReference("places/$placeId")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        mPlace = dataSnapshot.getValue(Place::class.java)!!
                        setUI()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
    }

    private fun setUI() {
        mPlaceName.text = "Place Name: ${mPlace.name}"
        mDate.text = mPlace.date
        mTime.text = mPlace.time

        Glide.with(this@DetailsActivity)
                .load(mPlace.imageUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .into(mPlaceImage)

        mDatabase.getReference("users/${mPlace.addedBy}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userPlace = dataSnapshot.getValue(User::class.java)!!

                        mProfileName.text = "Username: ${userPlace.username}"
                        Glide.with(this@DetailsActivity)
                                .load(userPlace.profileUrl)
                                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                                .into(mProfileImage)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
    }
}
