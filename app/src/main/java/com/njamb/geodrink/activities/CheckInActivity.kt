package com.njamb.geodrink.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.SparseBooleanArray
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast

import com.bumptech.glide.Glide
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
import com.njamb.geodrink.models.Coordinates
import com.njamb.geodrink.models.Place
import com.njamb.geodrink.services.PoiService
import com.njamb.geodrink.utils.PlacesGeoFire

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

class CheckInActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var checkInBtn: Button? = null
    private var drinksListView: ListView? = null
    private var mLocalBcastManager: LocalBroadcastManager? = null

    private var drinks: HashMap<String, Long>? = null

    private var mDatabase: FirebaseDatabase? = null
    private var userId: String? = null

    private var mPhotoPath: String? = null

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mLocalBcastManager = LocalBroadcastManager.getInstance(this)

        mDatabase = FirebaseDatabase.getInstance()
        userId = FirebaseAuth.getInstance().currentUser!!.uid

        imageView = findViewById(R.id.checkin_iv_photo) as ImageView

        val addPhoto = findViewById(R.id.checkin_btn_addphoto) as Button
        addPhoto.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (ex: IOException) {
                    Log.e(TAG, ex.message)
                }

                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }

        val changeLocNameBtn = findViewById(R.id.checkIn_btn_changeLocName) as Button
        changeLocNameBtn.setOnClickListener {
            val etName = findViewById(R.id.checkin_et_location) as EditText

            if (changeLocNameBtn.text.toString() == CHANGE_LOC_NAME) {
                etName.isEnabled = true
                changeLocNameBtn.text = DONE_CHANGING_LOC_NAME
            } else {
                etName.isEnabled = false
                changeLocNameBtn.text = CHANGE_LOC_NAME
            }

            enableDisableBtn()
        }

        val etName = findViewById(R.id.checkin_et_location) as EditText
        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                enableDisableBtn()
            }
        })

        drinksListView = findViewById(R.id.checkin_lv_drinks) as ListView
        setDrinksList()

        checkInBtn = findViewById(R.id.checkin_btn_checkin) as Button
        checkInBtn!!.setOnClickListener {
            updateDrinks()
            checkIn()
            // Terminate activity upon checking in:
            finish()
        }
        enableDisableBtn()

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        mPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                Glide.with(this).load(mPhotoPath).into(imageView!!)

                enableDisableBtn()
            }
        }
    }

    private fun checkIfShouldEnable(): Boolean {
        val etName = findViewById(R.id.checkin_et_location) as EditText
        val ivPhoto = findViewById(R.id.checkin_iv_photo) as ImageView

        return !(etName.text.toString().trim { it <= ' ' } == ""
                || ivPhoto.drawable == null
                || drinksListView!!.checkedItemPositions == null)
    }

    private fun enableDisableBtn() {
        checkInBtn!!.isEnabled = checkIfShouldEnable()
    }

    private fun setDrinksList() {
        val drinksArray = ArrayList<String>()
        drinksArray.add("Beer")
        drinksArray.add("Coffee")
        drinksArray.add("Cocktail")
        drinksArray.add("Juice")
        drinksArray.add("Soda")
        drinksArray.add("Alcohol")

        val drinksAdapter = ArrayAdapter(this,
                                         android.R.layout.simple_list_item_multiple_choice, drinksArray)

        drinksListView!!.adapter = drinksAdapter
        drinksListView!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
    }

    private fun updateDrinks() {
        val drinksRef = mDatabase!!
                .getReference(String.format("users/%s/drinks", userId))

        drinksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                drinks = dataSnapshot.value as HashMap<String, Long>?
                updateDrinksValues()
                drinksRef.setValue(drinks)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun updateDrinksValues() {
        val len = drinksListView!!.count
        val checked = drinksListView!!.checkedItemPositions
        var drink: String

        for (i in 0..len - 1) {
            if (checked.get(i)) {
                drink = drinksListView!!.getItemAtPosition(i).toString().toLowerCase()
                drinks!!.put(drink, drinks!![drink] + 1)
            }
        }
    }

    private fun checkIn() {
        val intent = Intent(PoiService.ACTION_ADD_POINTS)
                .putExtra("pts", 20)
        mLocalBcastManager!!.sendBroadcast(intent)

        // add Place in DB
        val bundle = getIntent().extras
        val place = Place(
                /*name*/(findViewById(R.id.checkin_et_location) as EditText).text.toString(),
                /*date*/SimpleDateFormat("dd/MM/yyyy").format(Date()),
                /*time*/SimpleDateFormat("HH:mm:ss").format(Date()),
                /*addedBy*/userId,
                /*location*/Coordinates(bundle.getDouble("lat"), bundle.getDouble("lon"))
        )
        val key = mDatabase!!.getReference("places").push().key
        mDatabase!!.getReference("places").child(key).setValue(place)

        // add placeId to User
        mDatabase!!.getReference(String.format("users/%s/places/%s", userId, key)).setValue(true)

        addPhoto(key)

        setGeoFirePlaceLocation(key, place.location.lat, place.location.lng)
    }

    private fun addPhoto(placeId: String) {
        val imgUrl = String.format("images/places/%s.jpg", placeId)
        val imgRef = FirebaseStorage.getInstance().getReference(imgUrl)
        val uploadTask = imgRef.putFile(Uri.fromFile(File(mPhotoPath!!)))

        uploadTask.addOnFailureListener { Toast.makeText(this@CheckInActivity, "Upload failed!", Toast.LENGTH_SHORT).show() }.addOnSuccessListener { taskSnapshot ->
            val imageUrl = taskSnapshot.downloadUrl!!.toString()

            // add image url to db
            mDatabase!!.getReference(String.format("places/%s/imageUrl", placeId)).setValue(imageUrl)
        }
    }

    private fun setGeoFirePlaceLocation(id: String, lat: Double, lng: Double) {
        val intent = Intent(PlacesGeoFire.ACTION_SET_LOCATION)
        intent.putExtra("id", id)
                .putExtra("lat", lat)
                .putExtra("lng", lng)
        mLocalBcastManager!!.sendBroadcast(intent)
    }

    companion object {
        val TAG = "CheckInActivity"

        private val REQUEST_IMAGE_CAPTURE = 1
        private val CHANGE_LOC_NAME = "Change Name"
        private val DONE_CHANGING_LOC_NAME = "Done Changing"
    }
}
