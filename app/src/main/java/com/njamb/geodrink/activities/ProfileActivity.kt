package com.njamb.geodrink.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.njamb.geodrink.R
import com.njamb.geodrink.adapters.FriendListAdapter
import com.njamb.geodrink.models.User

class ProfileActivity : AppCompatActivity() {

    private var profileImg: ImageView? = null
    private var username: TextView? = null
    private var name: TextView? = null
    private var email: TextView? = null
    private var birthday: TextView? = null
    private var mRecyclerView: RecyclerView? = null

    private var pb: ProgressBar? = null

    private var mDatabase: DatabaseReference? = null

    private var userId: String? = null
    private var mAdapter: FriendListAdapter? = null


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

        mDatabase = FirebaseDatabase.getInstance().getReference(String.format("users/%s", userId))

        profileImg = findViewById(R.id.profile_image) as ImageView
        username = findViewById(R.id.profile_username) as TextView
        name = findViewById(R.id.profile_name) as TextView
        email = findViewById(R.id.profile_email) as TextView
        birthday = findViewById(R.id.profile_birthday) as TextView

        mRecyclerView = findViewById(R.id.friends_list) as RecyclerView
        mRecyclerView!!.layoutManager = LinearLayoutManager(this)
        mRecyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        pb = findViewById(R.id.progressbar_profile_img) as ProgressBar

        val uploadPic = findViewById(R.id.profile_add_pic) as Button
        uploadPic.setOnClickListener { choosePictureFromGallery() }

        val logoutBtn = findViewById(R.id.profile_logout_btn) as Button
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }

        mDatabase!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                fillProfile(user)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "addListenerForSingleValueEvent: " + databaseError.message)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECT_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val imageUri = data.data
                    setProfileImage(imageUri)
                    uploadProfilePicture(imageUri)
                }
            }
        }
    }

    private fun setProfileImage(uri: Uri) {
        Glide.with(this).load(uri).into(profileImg!!)
    }

    private fun fillProfile(user: User?) {
        pb!!.visibility = View.VISIBLE

        mAdapter = FriendListAdapter(this@ProfileActivity, mRecyclerView, user)
        mRecyclerView!!.adapter = mAdapter

        Glide.with(this)
                .load(user!!.profileUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        pb!!.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        pb!!.visibility = View.GONE
                        return false
                    }
                })
                .into(profileImg!!)
        username!!.text = String.format("Username: %s", user.username)
        name!!.text = String.format("Name: %s", user.fullName)
        email!!.text = String.format("Email: %s", user.email)
        birthday!!.text = String.format("Birthday: %s", user.birthday)
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
        val imageUrl = String.format("images/users/%s.jpg", userId)
        val imageRef = FirebaseStorage.getInstance().reference.child(imageUrl)
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnFailureListener { Toast.makeText(this@ProfileActivity, "Upload failed!", Toast.LENGTH_SHORT).show() }.addOnSuccessListener { taskSnapshot ->
            val imageUrl = taskSnapshot.downloadUrl!!.toString()
            addProfilePictureLinkToDatabase(imageUrl)
        }
    }

    private fun addProfilePictureLinkToDatabase(url: String) {
        mDatabase!!.child("profileUrl").setValue(url)
    }

    companion object {
        private val TAG = "ProfileActivity"
        private val SELECT_IMAGE_REQUEST = 1
    }
}
