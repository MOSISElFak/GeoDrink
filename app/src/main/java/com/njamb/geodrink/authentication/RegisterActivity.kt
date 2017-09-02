package com.njamb.geodrink.authentication

import android.app.Activity
import android.app.DialogFragment
import android.app.FragmentManager
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.Email
import com.mobsandgeeks.saripaar.annotation.NotEmpty
import com.mobsandgeeks.saripaar.annotation.Password
import com.mobsandgeeks.saripaar.annotation.Past
import com.njamb.geodrink.R
import com.njamb.geodrink.fragments.DatePickerFragment
import com.njamb.geodrink.models.User

class RegisterActivity : AppCompatActivity(), Validator.ValidationListener {

    private lateinit var datePickerFragment: DialogFragment

    @NotEmpty private lateinit var username: EditText

    @NotEmpty
    @Password(message = "Password needs to have at least 6 characters")
    private lateinit var password: EditText

    @NotEmpty private lateinit var name: EditText

    @NotEmpty
    @Email
    private lateinit var email: EditText

    @NotEmpty
    @Past(dateFormat = "dd/MM/yyyy")
    private lateinit var birthday: EditText

    private lateinit var register: Button
    private lateinit var pickImage: Button
    private lateinit var cancel: Button
    private lateinit var loginImage: ImageView
    private var mImageUri: Uri? = null

    private lateinit var pd: ProgressDialog

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: DatabaseReference

    private lateinit var mValidator: Validator


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance().reference

        mValidator = Validator(this)
        mValidator.setValidationListener(this)

        datePickerFragment = DatePickerFragment()
        username = findViewById(R.id.register_et_username) as EditText
        password = findViewById(R.id.register_et_password) as EditText
        name = findViewById(R.id.register_et_name) as EditText
        email = findViewById(R.id.register_et_email) as EditText
        birthday = findViewById(R.id.register_et_birthday) as EditText
        register = findViewById(R.id.register_btn_register) as Button
        pickImage = findViewById(R.id.register_btn_pickimage) as Button
        cancel = findViewById(R.id.register_btn_cancel) as Button
        loginImage = findViewById(R.id.register_iv_loginimage) as ImageView

        configProgressDialog()

        // Set datePicker widget for birthday editText field:
        birthday.inputType = InputType.TYPE_NULL
        birthday.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val fragmentManager = fragmentManager
                datePickerFragment.show(fragmentManager, "datePicker")
            }
        }
        birthday.setOnClickListener {
            val fragmentManager = fragmentManager
            datePickerFragment.show(fragmentManager, "datePicker")
        }

        register.setOnClickListener { mValidator.validate() }

        pickImage.setOnClickListener {
            // TODO: Add picture select and change the present imageView showing the AppLogo.
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            val chooser = Intent.createChooser(intent, "Select Picture")
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(chooser, SELECT_IMAGE_REQUEST)
            } else {
                Toast.makeText(this@RegisterActivity, "No available apps for this action.",
                               Toast.LENGTH_SHORT).show()
            }
        }

        cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mImageUri = data.data
                        Glide.with(this).load(mImageUri).into(loginImage)
                    }
                }
            }
        }
    }

    private fun configProgressDialog() {
        pd = ProgressDialog(this, R.style.TransparentProgressDialogStyle)
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small)
        pd.isIndeterminate = true
        pd.setCancelable(false)
    }

    private fun registerUser() {
        val emailReg = email.text.toString()
        val passwordReg = password.text.toString()
        mAuth.createUserWithEmailAndPassword(emailReg, passwordReg)
                .addOnCompleteListener(this) { task ->
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    pd.dismiss()

                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = mAuth.currentUser
                        createUserProfile(user)
                        uploadProfilePhoto(user!!.uid)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this@RegisterActivity, "Authentication failed.",
                                       Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun createUserProfile(user: FirebaseUser?) {
        val dbUser = User(
                name.text.toString(),
                username.text.toString(),
                email.text.toString(),
                birthday.text.toString()
        )

        mDatabase.child("users").child(user!!.uid).setValue(dbUser)

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun uploadProfilePhoto(userId: String) {
        val imageRef = FirebaseStorage.getInstance().getReference("images/users/$userId.jpg")
        val uploadTask = imageRef.putFile(mImageUri!!)

        uploadTask.addOnFailureListener {
            Toast.makeText(this@RegisterActivity, "Upload failed!", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            val imageUrl = taskSnapshot.downloadUrl!!.toString()
            mDatabase.child("users").child(userId).child("profileUrl").setValue(imageUrl)
        }
    }

    override fun onValidationSucceeded() {
        if (mImageUri == null) {
            Toast.makeText(this, "You need to pick image", Toast.LENGTH_SHORT).show()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        pd.show()
        registerUser()
    }

    override fun onValidationFailed(errors: List<ValidationError>) {
        for (error in errors) {
            val view = error.view
            val message = error.getCollatedErrorMessage(this)
            if (view is EditText) {
                view.error = message
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val TAG = "RegisterActivity"
        private val SELECT_IMAGE_REQUEST = 1
    }
}
