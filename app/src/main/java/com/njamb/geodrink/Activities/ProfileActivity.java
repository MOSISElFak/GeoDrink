package com.njamb.geodrink.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.njamb.geodrink.Classes.User;
import com.njamb.geodrink.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int SELECT_IMAGE_REQUEST = 1;

    private ImageView profileImg;
    private TextView username;
    private TextView email;
    private TextView birthday;

    private DatabaseReference mDatabase;

    private String userId;

//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        userId = bundle.getString("userId");

        mDatabase = FirebaseDatabase.getInstance().getReference(String.format("users/%s", userId));

        profileImg = (ImageView) findViewById(R.id.profile_image);
        username = (TextView) findViewById(R.id.profile_username);
        email = (TextView) findViewById(R.id.profile_email);
        birthday = (TextView) findViewById(R.id.profile_birthday);

        Button uploadPic = (Button) findViewById(R.id.profile_add_pic);
        uploadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePictureFromGallery();
            }
        });

        Button logoutBtn = (Button) findViewById(R.id.profile_logout_btn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                finish();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // TODO: <<BUG>> when choosing another pic, old is shown (new is uploaded)
                    setProfileImage(data.getData());
                    uploadProfilePicture();
                }
            }
        }
    }

    private void setProfileImage(Uri uri) {
        profileImg.setImageURI(uri);
    }

    private void fillProfile(User user) {
        // TODO: fetch profile pic: is this ok?
        fetchProfilePicture();
        // TODO: progress bar/dialog
        username.setText(String.format("Username: %s", user.username));
        email.setText(String.format("Email: %s", user.email));
        birthday.setText(String.format("Birthday: %s", user.birthday));
    }

    private void choosePictureFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        Intent chooser = Intent.createChooser(intent, "Select Picture");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, SELECT_IMAGE_REQUEST);
        }
        else {
            Toast.makeText(this, "No available apps for this action.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfilePicture() {
        // TODO: can this be simpler?
        profileImg.setDrawingCacheEnabled(true);
        profileImg.buildDrawingCache();
        profileImg.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        profileImg.layout(0, 0, profileImg.getMeasuredWidth(), profileImg.getMeasuredHeight());
        Bitmap bitmap = profileImg.getDrawingCache();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        final String imageUrl = String.format("images/%s.jpg", userId);
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageUrl);
        UploadTask uploadTask = imageRef.putBytes(data);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                addProfilePictureLinkToDatabase(imageUrl);
            }
        });
    }

    private void fetchProfilePicture() {
        String imageUrl = String.format("images/%s.jpg", userId);
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageUrl);

        File localFile = null;
        try {
            localFile = File.createTempFile("images", "jpg");

            final File finalLocalFile = localFile;
            imageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    // Local temp file has been created
                    setProfileImage(Uri.fromFile(finalLocalFile));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addProfilePictureLinkToDatabase(String url) {
        mDatabase.child("profileUrl").setValue(url);
    }
}
