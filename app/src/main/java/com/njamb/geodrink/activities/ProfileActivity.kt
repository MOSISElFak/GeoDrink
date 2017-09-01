package com.njamb.geodrink.activities

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.njamb.geodrink.R
import com.njamb.geodrink.adapters.FriendListAdapter
import com.njamb.geodrink.models.User
import kotlinx.android.synthetic.main.content_profile.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var mDatabase: DatabaseReference

    private lateinit var userId: String
    private lateinit var mAdapter: FriendListAdapter


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val bundle = intent.extras
        userId = bundle.getString("userId")

        val layoutWithButtons = findViewById(R.id.profile_act_buttons_layout)
        if (userId != FirebaseAuth.getInstance().currentUser!!.uid) {
            layoutWithButtons.visibility = View.GONE
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("users/$userId")

        friends_list.layoutManager = LinearLayoutManager(this)
        friends_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        profile_add_pic.setOnClickListener { choosePictureFromGallery() }

        profile_logout_btn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }

        mDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)!!

                fillProfile(user)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "addListenerForSingleValueEvent: " + databaseError.message)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SELECT_IMAGE_REQUEST -> when(resultCode) {
                Activity.RESULT_OK -> data?.let {
                    setProfileImage(data.data)
                    uploadProfilePicture(data.data)
                }
            }
        }
    }

    private fun setProfileImage(uri: Uri) {
        Glide.with(this).load(uri).into(profile_image)
    }

    private fun fillProfile(user: User) {
        progressbar_profile_img.visibility = View.VISIBLE

        mAdapter = FriendListAdapter(this@ProfileActivity, friends_list, user)
        friends_list.adapter = mAdapter

        Glide.with(this)
                .load(user.profileUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        progressbar_profile_img.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        progressbar_profile_img.visibility = View.GONE
                        return false
                    }
                })
                .into(profile_image)
        profile_username.text = "Username: ${user.username}"
        profile_name.text = "Name: ${user.fullName}"
        profile_email.text = "Email: ${user.email}"
        profile_birthday.text = "Birthday: ${user.birthday}"
    }

    private fun choosePictureFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Select Picture")
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, SELECT_IMAGE_REQUEST)
        } else {
            Toast.makeText(this, "No available apps for this action.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        val imageRef = FirebaseStorage.getInstance().reference.child("images/users/$userId.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnFailureListener {
            Toast.makeText(this@ProfileActivity, "Upload failed!", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            val imageUrl = taskSnapshot.downloadUrl!!.toString()
            addProfilePictureLinkToDatabase(imageUrl)
        }
    }

    private fun addProfilePictureLinkToDatabase(url: String) {
        mDatabase.child("profileUrl").setValue(url)
    }

    companion object {
        private val TAG = "ProfileActivity"
        private val SELECT_IMAGE_REQUEST = 1
    }
}
