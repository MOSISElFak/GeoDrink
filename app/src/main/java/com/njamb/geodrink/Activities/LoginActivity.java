package com.njamb.geodrink.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.njamb.geodrink.R;

public class LoginActivity extends AppCompatActivity {

    static int REGISTER = 1;

    EditText username;
    EditText password;
    Button login;
    Button register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        username = (EditText) findViewById(R.id.login_et_username);
        password = (EditText) findViewById(R.id.login_et_password);
        login = (Button) findViewById(R.id.login_btn_login);
        register = (Button) findViewById(R.id.login_btn_register);

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                areFieldsEmpty();
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
                areFieldsEmpty();
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
    private void areFieldsEmpty() {
        if (username.getText().toString().equals("") ||
                password.getText().toString().equals(""))
        {
            login.setEnabled(false);
        }
        else {
            login.setEnabled(true);
        }
    }

}
