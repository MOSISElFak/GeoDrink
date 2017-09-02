package com.njamb.geodrink.authentication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.Email
import com.mobsandgeeks.saripaar.annotation.NotEmpty
import com.mobsandgeeks.saripaar.annotation.Password
import com.njamb.geodrink.R

class LoginActivity : AppCompatActivity(), Validator.ValidationListener {

    private lateinit var mValidator: Validator
    @NotEmpty
    @Email
    private lateinit var email: EditText
    @Password private lateinit var password: EditText

    private lateinit var pd: ProgressDialog
    private lateinit var mAuth: FirebaseAuth


    override fun onBackPressed() {
        finishAffinity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        mValidator = Validator(this)
        mValidator.setValidationListener(this)

        mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser != null) finish() // TODO: how to remove this hack?

        email = findViewById(R.id.login_et_username) as EditText
        password = findViewById(R.id.login_et_password) as EditText

        val login = findViewById(R.id.login_btn_login) as Button
        login.setOnClickListener { mValidator.validate() }

        val register = findViewById(R.id.login_btn_register) as Button
        register.setOnClickListener {
            val registerActivity = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivityForResult(registerActivity, REQUEST_REGISTER)
        }

        val btnResetPass = findViewById(R.id.btn_rst_pass) as Button
        btnResetPass.setOnClickListener { startActivity(Intent(this@LoginActivity, ResetPasswordActivity::class.java)) }

        configProgressDialog()
    }

    private fun configProgressDialog() {
        pd = ProgressDialog(this, R.style.TransparentProgressDialogStyle)
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small)
        pd.isIndeterminate = true
        pd.setCancelable(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_REGISTER -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Successful registration!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser() {
        val email = this.email.text.toString()
        val pass = password.text.toString()

        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        pd.show()
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    pd.dismiss()

                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        finish()
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this@LoginActivity,
                                       R.string.msg_auth_fail,
                                       Toast.LENGTH_LONG).show()
                    }
                }
    }

    override fun onValidationSucceeded() {
        loginUser()
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
        private val TAG = "LoginActivity"
        private val REQUEST_REGISTER = 1
    }
}
