package com.njamb.geodrink.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.njamb.geodrink.R;
import com.njamb.geodrink.models.Coordinates;
import com.njamb.geodrink.models.Place;
import com.njamb.geodrink.services.PoiService;
import com.njamb.geodrink.utils.PlacesGeoFire;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CheckInActivity extends AppCompatActivity {
    public static final String TAG = "CheckInActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CHANGE_LOC_NAME = "Change Name";
    private static final String DONE_CHANGING_LOC_NAME = "Done Changing";

    private ImageView imageView;
    private Button checkInBtn;
    private ListView drinksListView;
    private LocalBroadcastManager mLocalBcastManager;

    private HashMap<String, Long> drinks;

    private FirebaseDatabase mDatabase;
    private String userId;

    private String mPhotoPath;

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

        mDatabase = FirebaseDatabase.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        imageView = (ImageView) findViewById(R.id.checkin_iv_photo);

        Button addPhoto = (Button) findViewById(R.id.checkin_btn_addphoto);
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
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
                // Terminate activity upon checking in:
                finish();
            }
        });
        enableDisableBtn();

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Glide.with(this).load(mPhotoPath).into(imageView);

                enableDisableBtn();
            }
        }
    }

    private boolean checkIfShouldEnable() {
        EditText etName = (EditText) findViewById(R.id.checkin_et_location);
        ImageView ivPhoto = (ImageView) findViewById(R.id.checkin_iv_photo);

        return !(etName.getText().toString().trim().equals("")
                || ivPhoto.getDrawable() == null
                || drinksListView.getCheckedItemPositions() == null);
    }

    private void enableDisableBtn() {
        checkInBtn.setEnabled(checkIfShouldEnable());
//        if (checkIfShouldEnable()) {
//            checkInBtn.setEnabled(true);
//        }
//        else {
//            checkInBtn.setEnabled(false);
//        }
    }

    private void setDrinksList() {
        List<String> drinksArray = new ArrayList<String>();
        drinksArray.add("Beer");
        drinksArray.add("Coffee");
        drinksArray.add("Cocktail");
        drinksArray.add("Juice");
        drinksArray.add("Soda");
        drinksArray.add("Alcohol");

        ArrayAdapter<String> drinksAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, drinksArray);

        drinksListView.setAdapter(drinksAdapter);
        drinksListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }

//    private void checkInUser() {
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//        userId = FirebaseAuth.getInstance().getCurrentUser();
//    }

    private void updateDrinks() {
        final DatabaseReference drinksRef = mDatabase
                .getReference(String.format("users/%s/drinks", userId));

        drinksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                drinks = (HashMap<String, Long>) dataSnapshot.getValue();
                updateDrinksValues();
                drinksRef.setValue(drinks);
            }

            @Override public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void updateDrinksValues() {
        int len = drinksListView.getCount();
        SparseBooleanArray checked = drinksListView.getCheckedItemPositions();
        String drink;

        for (int i = 0; i < len; i++) {
            if (checked.get(i)) {
                drink = drinksListView.getItemAtPosition(i).toString().toLowerCase();
                drinks.put(drink, drinks.get(drink) + 1);
//                drinks.incrementDrinkByName(drink);
            }
        }
    }

    private void checkIn() {
        Intent intent = new Intent(PoiService.ACTION_ADD_POINTS)
                .putExtra("pts", 20);
        mLocalBcastManager.sendBroadcast(intent);

        // add Place in DB
        Bundle bundle = getIntent().getExtras();
        Place place = new Place(
            /*name*/((EditText) findViewById(R.id.checkin_et_location)).getText().toString(),
            /*date*/new SimpleDateFormat("dd/MM/yyyy").format(new Date()),
            /*time*/new SimpleDateFormat("HH:mm:ss").format(new Date()),
            /*addedBy*/userId,
            /*location*/new Coordinates(bundle.getDouble("lat"),bundle.getDouble("lon"))
        );
        String key = mDatabase.getReference("places").push().getKey();
        mDatabase.getReference("places").child(key).setValue(place);

        // add placeId to User
        mDatabase.getReference(String.format("users/%s/places/%s", userId, key)).setValue(true);

        addPhoto(key);

        setGeoFirePlaceLocation(key, place.location.lat, place.location.lng);
    }

    private void addPhoto(final String placeId) {
        String imgUrl = String.format("images/places/%s.jpg", placeId);
        StorageReference imgRef = FirebaseStorage.getInstance().getReference(imgUrl);
        UploadTask uploadTask = imgRef.putFile(Uri.fromFile(new File(mPhotoPath)));

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(CheckInActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests")
                String imageUrl = taskSnapshot.getDownloadUrl().toString();

                // add image url to db
                mDatabase.getReference(String.format("places/%s/imageUrl", placeId)).setValue(imageUrl);
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
