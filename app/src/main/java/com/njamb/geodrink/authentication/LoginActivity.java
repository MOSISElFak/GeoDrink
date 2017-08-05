package com.njamb.geodrink.authentication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.njamb.geodrink.R;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements Validator.ValidationListener {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_REGISTER = 1;

    private Validator mValidator = null;
    @NotEmpty
    @Email private EditText email;
    @Password private EditText password;

    private ProgressDialog pd;
    private FirebaseAuth mAuth;


    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) finish(); // TODO: how to remove this hack?

        email = (EditText) findViewById(R.id.login_et_username);
        password = (EditText) findViewById(R.id.login_et_password);

        Button login = (Button) findViewById(R.id.login_btn_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mValidator.validate();
            }
        });

        Button register = (Button) findViewById(R.id.login_btn_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(registerActivity, REQUEST_REGISTER);
            }
        });

        Button btnResetPass = (Button) findViewById(R.id.btn_rst_pass);
        btnResetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });

        configProgressDialog();
    }

    private void configProgressDialog() {
        pd = new ProgressDialog(this, R.style.TransparentProgressDialogStyle);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_REGISTER:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Successful registration!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void loginUser() {
        String email = this.email.getText().toString();
        String pass = password.getText().toString();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        pd.show();
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        pd.dismiss();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            finish();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this,
                                    R.string.msg_auth_fail,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onValidationSucceeded() {
        loginUser();
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
