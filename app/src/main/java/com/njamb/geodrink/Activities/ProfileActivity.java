package com.njamb.geodrink.Activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
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
import com.njamb.geodrink.Classes.FriendListAdapter;
import com.njamb.geodrink.Classes.User;
import com.njamb.geodrink.R;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int SELECT_IMAGE_REQUEST = 1;

    private ImageView profileImg;
    private TextView username;
    private TextView email;
    private TextView birthday;
    private RecyclerView mRecyclerView;

    private ProgressBar pb;

    private DatabaseReference mDatabase;

    private String userId;
    private FriendListAdapter mAdapter;


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

        mRecyclerView = (RecyclerView) findViewById(R.id.friends_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        pb = (ProgressBar) findViewById(R.id.progressbar_profile_img);

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
                    Uri imageUri = data.getData();
                    setProfileImage(imageUri);
                    uploadProfilePicture(imageUri);
                }
            }
        }
    }

    private void setProfileImage(Uri uri) {
        Glide.with(this).load(uri).into(profileImg);
    }

    private void fillProfile(User user) {
        pb.setVisibility(View.VISIBLE);

        mAdapter = new FriendListAdapter(ProfileActivity.this, mRecyclerView, user);
        mRecyclerView.setAdapter(mAdapter);

        Glide.with(this)
                .load(user.profileUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        pb.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        pb.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(profileImg);
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

    private void uploadProfilePicture(Uri imageUri) {
        final String imageUrl = String.format("images/%s.jpg", userId);
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageUrl);
        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(ProfileActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests")
                String imageUrl = taskSnapshot.getDownloadUrl().toString();
                addProfilePictureLinkToDatabase(imageUrl);
            }
        });
    }

    private void addProfilePictureLinkToDatabase(String url) {
        mDatabase.child("profileUrl").setValue(url);
    }
}
