package com.njamb.geodrink.authentication;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.njamb.geodrink.R;

import java.util.List;

public class ResetPasswordActivity extends AppCompatActivity implements Validator.ValidationListener {

    private ProgressDialog pd;

    @NotEmpty
    @Email private EditText email;
    private Validator mValidator = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // TODO: fix Progress Dialog
        pd = new ProgressDialog(this, R.style.TransparentProgressDialogStyle);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        pd.setIndeterminate(true);
        pd.setCancelable(false);

        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        email = (EditText) findViewById(R.id.edit_rst_email);

        Button btnSend = (Button) findViewById(R.id.btn_send_rst_pass);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mValidator.validate();
            }
        });
    }

    private void sendResetPasswordEmail() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        pd.show();
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Email sent", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Sending email failed. Try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onValidationSucceeded() {
        sendResetPasswordEmail();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);
            if (view instanceof EditText) {
                ((EditText) view).setError(message);
            }
            else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
