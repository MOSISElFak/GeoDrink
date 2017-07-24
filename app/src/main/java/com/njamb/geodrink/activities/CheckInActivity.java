package com.njamb.geodrink.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njamb.geodrink.R;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.models.Drinks;
import com.njamb.geodrink.models.Place;
import com.njamb.geodrink.models.Places;
import com.njamb.geodrink.utils.PlacesGeoFire;
import com.njamb.geodrink.utils.UsersGeoFire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckInActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CHANGE_LOC_NAME = "Change Name";
    private static final String DONE_CHANGING_LOC_NAME = "Done Changing";

    private ImageView imageView;
    private Button checkInBtn;
    private ListView drinksListView;
    private DatabaseReference databaseReference;
    private FirebaseUser user;
    private LocalBroadcastManager mLocalBcastManager;

    private Drinks drinks;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLocalBcastManager = LocalBroadcastManager.getInstance(this);

        checkInUser();

        imageView = (ImageView) findViewById(R.id.checkin_iv_photo);

        Button addPhoto = (Button) findViewById(R.id.checkin_btn_addphoto);
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        final Button changeLocNameBtn = (Button) findViewById(R.id.checkIn_btn_changeLocName);
        changeLocNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etName = (EditText) findViewById(R.id.checkin_et_location);

                if (changeLocNameBtn.getText().toString().equals(CHANGE_LOC_NAME)) {
                    etName.setEnabled(true);
                    changeLocNameBtn.setText(DONE_CHANGING_LOC_NAME);
                }
                else {
                    etName.setEnabled(false);
                    changeLocNameBtn.setText(CHANGE_LOC_NAME);
                }

                enableDisableBtn();
            }
        });

        EditText etName = (EditText) findViewById(R.id.checkin_et_location);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enableDisableBtn();
            }
        });

        drinksListView = (ListView) findViewById(R.id.checkin_lv_drinks);
        setDrinksList();

        checkInBtn = (Button) findViewById(R.id.checkin_btn_checkin);
        checkInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDrinks();
                checkIn();

                //TODO: Checkin photo to firebase (upon checking in)

                // Terminate activity upon checking in:
                finish();
            }
        });
        enableDisableBtn();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(imageBitmap);

                enableDisableBtn();
            }
        }
    }

    private boolean checkIfShouldEnable() {
        EditText etName = (EditText) findViewById(R.id.checkin_et_location);
        ImageView ivPhoto = (ImageView) findViewById(R.id.checkin_iv_photo);

        if (etName.getText().toString().equals("") || ivPhoto.getDrawable() == null || drinksListView.getCheckedItemPositions() == null)
            return false;
        else
            return true;
    }

    private void enableDisableBtn() {
        if (checkIfShouldEnable()) {
            checkInBtn.setEnabled(true);
        }
        else {
            checkInBtn.setEnabled(false);
        }
    }

    private void setDrinksList() {
        List<String> drinksArray = new ArrayList<String>();
        drinksArray.add("Beer");
        drinksArray.add("Coffee");
        drinksArray.add("Cocktail");
        drinksArray.add("Juice");
        drinksArray.add("Soda");
        drinksArray.add("Alcohol");

        ArrayAdapter<String> drinksAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, drinksArray);

        drinksListView.setAdapter(drinksAdapter);
        drinksListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }

    private void checkInUser() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void updateDrinks() {
        final DatabaseReference drinksRef = databaseReference.child("users").child(user.getUid())
                .child("drinks");

        drinksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                drinks = dataSnapshot.getValue(Drinks.class);
                updateDrinksValues();
                drinksRef.setValue(drinks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateDrinksValues() {
        int len = drinksListView.getCount();
        SparseBooleanArray checked = drinksListView.getCheckedItemPositions();
        String drink;

        for (int i = 0; i < len; i++) {
            drink = "";
            if (checked.get(i)) {
                drink = drinksListView.getItemAtPosition(i).toString();
                drinks.incrementDrinkByName(drink);
            }
        }
    }

    private void checkIn() {
        //DatabaseReference refPlaces = databaseReference.child("places").push();
        final String key = databaseReference.child("places").push().getKey();
        DatabaseReference refPlaces = databaseReference.child("places").child(key);
        Log.v("+nj", "databaseReference.getKey() = " + key);
        final DatabaseReference places = FirebaseDatabase.getInstance().getReference().child("users")
                .child(user.getUid()).child("places");

        final DatabaseReference userRef = databaseReference.child("users").child(user.getUid());

        // ** com.njamb.geodrink.models.Place **
        final Place place = new Place();
        Bundle bundle = getIntent().getExtras();
        // Setting our mapped data for firebase:

        place.location = new Coordinates(bundle.getDouble("lat"),bundle.getDouble("lon"));
        place.name = ((EditText) findViewById(R.id.checkin_et_location)).getText().toString();
        place.date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
        place.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        place.imageUrl = "www.geodrink.com"; // TODO: Change this ffs: imageUrl @Storage on Firebase

        setGeoFirePlaceLocation(key, place.location.lat, place.location.lng);

        // Create new place:
        refPlaces.setValue(place);
// ^ kreiranje lokacije radi kako treba!
        places.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //TODO: Pronaci nacin da se ubaci 'key' promenljiva u userID.places sa value 0:
                Places plcs = new Places();

                plcs = dataSnapshot.getValue(Places.class);
                if (plcs != null) {
                    Log.v("+nj",plcs.toString());
                    plcs.addPlace(key);
                    places.setValue(plcs);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void setGeoFirePlaceLocation(String id, double lat, double lng) {
        Intent intent = new Intent(PlacesGeoFire.ACTION_SET_LOCATION);
        intent.putExtra("id", id)
                .putExtra("lat", lat)
                .putExtra("lng", lng);
        mLocalBcastManager.sendBroadcast(intent);
    }
}
