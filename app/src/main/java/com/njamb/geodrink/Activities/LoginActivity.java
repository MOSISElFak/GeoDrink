package com.njamb.geodrink.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.njamb.geodrink.R;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    static int REGISTER = 1;

    EditText username;
    EditText password;
    Button login;
    Button register;

    private FirebaseAuth mAuth;


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();

        username = (EditText) findViewById(R.id.login_et_username);
        password = (EditText) findViewById(R.id.login_et_password);
        login = (Button) findViewById(R.id.login_btn_login);
        register = (Button) findViewById(R.id.login_btn_register);

        lockLoginBtn();

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lockLoginBtn();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lockLoginBtn();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(registerActivity, REGISTER);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            // TO-DO: after user is registered, make toast (successful registration)
            // & return to the map.
            // After registration, user is logged in automatically.

            Toast.makeText(this, "Successful registration!", Toast.LENGTH_SHORT).show();
        }
        else if (requestCode == Activity.RESULT_CANCELED) {
            // Do nothing.
        }
    }

    // Method that checks if any of the input fields are empty:
    private boolean areFieldsEmpty() {
        if (username.getText().toString().equals("") ||
                password.getText().toString().equals(""))
        {
            return true;
        }
        else {
            return false;
        }
    }

    private void lockLoginBtn() {
        if (areFieldsEmpty()) {
            login.setEnabled(false);
        }
        else {
            login.setEnabled(true);
        }
    }

    private void loginUser() {
        String email = username.getText().toString();
        String pass = password.getText().toString();

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            // TODO: update UI?
                            Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                            intent.putExtra("userId", user.getUid());
                            LoginActivity.this.startActivity(intent);
//                            Toast.makeText(LoginActivity.this, "Logged in", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
