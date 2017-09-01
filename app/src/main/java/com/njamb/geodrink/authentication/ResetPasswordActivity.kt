package com.njamb.geodrink.authentication

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.Email
import com.mobsandgeeks.saripaar.annotation.NotEmpty
import com.njamb.geodrink.R

class ResetPasswordActivity : AppCompatActivity(), Validator.ValidationListener {

    private var pd: ProgressDialog? = null

    @NotEmpty
    @Email
    private var email: EditText? = null
    private var mValidator: Validator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        pd = ProgressDialog(this, R.style.TransparentProgressDialogStyle)
        pd!!.setProgressStyle(android.R.style.Widget_ProgressBar_Small)
        pd!!.isIndeterminate = true
        pd!!.setCancelable(false)

        mValidator = Validator(this)
        mValidator!!.setValidationListener(this)

        email = findViewById(R.id.edit_rst_email) as EditText

        val btnSend = findViewById(R.id.btn_send_rst_pass) as Button
        btnSend.setOnClickListener { mValidator!!.validate() }
    }

    private fun sendResetPasswordEmail() {
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        pd!!.show()
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email!!.text.toString())
                .addOnCompleteListener { task ->
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    pd!!.dismiss()
                    if (task.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity,
                                       "Email sent", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity,
                                       "Sending email failed. Try again.",
                                       Toast.LENGTH_SHORT).show()
                    }
                }
    }

    override fun onValidationSucceeded() {
        sendResetPasswordEmail()
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
}
