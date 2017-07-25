package com.njamb.geodrink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.njamb.geodrink.R;
import com.njamb.geodrink.models.Place;
import com.njamb.geodrink.models.User;

public class DetailsActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private DatabaseReference mPlacesReference ;
    private DatabaseReference mPlaceUserRef;
    private FirebaseUser mUser;
    private StorageReference mStorage;
    private TextView mDate;
    private TextView mTime;
    private TextView mPlaceName;
    private TextView mProfileName;
    private Button mReturn;
    private ImageView mProfileImage;
    private ImageView mPlaceImage;
    private String placeId;
    private Place mPlace = new Place();
    private User mUserPlace;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button returnBtn = (Button) findViewById(R.id.details_btn_return);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Initialize components:
        getReferences();

        // Set listeners:
        setListeners();

        // Set UI elements:
        //setUI();
    }

    private void getReferences() {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mUser.getUid());

        mStorage = FirebaseStorage.getInstance().getReference().child("images").child("places");

        mPlaceName = (TextView) findViewById(R.id.details_tv_placeName);
        mProfileName = (TextView) findViewById(R.id.details_tv_userName);
        mDate = (TextView) findViewById(R.id.details_tv_date);
        mTime = (TextView) findViewById(R.id.details_tv_time);
        mProfileImage = (ImageView) findViewById(R.id.details_iv_profilepic);
        mPlaceImage = (ImageView) findViewById(R.id.details_iv_photo);
        mReturn = (Button) findViewById(R.id.details_btn_return);
    }

    private void setListeners() {
        // Return button:
        mReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Get parsed placeId:
        Intent intent = getIntent();
        placeId = intent.getStringExtra("placeId");

        Log.v("+nj", "PRE mPlacesRef");
        // Map the place data:
        FirebaseDatabase.getInstance()
                .getReference(String.format("places/%s", placeId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mPlace = dataSnapshot.getValue(Place.class);
                setUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        Log.v("+nj", "POST mPlacesRef");

        Log.v("+nj", mPlace.toString());

        Log.v("+nj", "PRE mPlaceUserRef");
        // Map the checked-in user data:

        Log.v("+nj", "POST mPlaceUserRef");
    }

    private void setUI() {
        mPlaceName.setText("Place Name: " + mPlace.name);
        mDate.setText(mPlace.date);
        mTime.setText(mPlace.time);

        Glide.with(DetailsActivity.this)
                .load(mPlace.imageUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .into(mPlaceImage);

        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s", mPlace.addedBy))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUserPlace = dataSnapshot.getValue(User.class);

                        mProfileName.setText("Username: " + mUserPlace.username);
                        Glide.with(DetailsActivity.this)
                                .load(mUserPlace.profileUrl)
                                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                                .into(mProfileImage);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
