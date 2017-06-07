package com.njamb.geodrink.Activities;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.njamb.geodrink.Classes.User;
import com.njamb.geodrink.R;

import java.io.File;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private ImageView profileImg;
    private TextView username;
    private TextView email;
    private TextView birthday;
    private Button uploadPic;

    private DatabaseReference mDatabase;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Bundle bundle = getIntent().getExtras();
        userId = bundle.getString("userId");

        mDatabase = FirebaseDatabase.getInstance().getReference("users/" + userId);

        profileImg = (ImageView) findViewById(R.id.profile_image);
        username = (TextView) findViewById(R.id.profile_username);
        email = (TextView) findViewById(R.id.profile_email);
        birthday = (TextView) findViewById(R.id.profile_birthday);
        uploadPic = (Button) findViewById(R.id.profile_add_pic);

        uploadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePictureFromGalery();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                fillProfile(user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "addListenerForSingleValueEvent: " + databaseError.getMessage());
            }
        });
    }

    private void fillProfile(User user) {
        // TODO: fetch profile pic
        // TODO: progress bar/dialog
        username.setText(user.username);
        email.setText(user.email);
        birthday.setText(user.birthday);
    }

    private void choosePictureFromGalery() {
        // TODO: choose picture from gallery
        uploadProfilePicture("PATH"); // PROVIDE PATH
    }

    private void uploadProfilePicture(String path) {
        Uri file = Uri.fromFile(new File(path));
        StorageReference riversRef = FirebaseStorage.getInstance().getReference()
                .child("images/"+file.getLastPathSegment()); // CHANGE LINK ??
        UploadTask uploadTask = riversRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();

                addProfilePictureLinkToDatabase(downloadUrl.toString());
            }
        });
    }

    private void addProfilePictureLinkToDatabase(String url) {
        mDatabase.child("profileUrl").setValue(url);
    }
}
