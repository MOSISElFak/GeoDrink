package com.njamb.geodrink.Activities;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.njamb.geodrink.Classes.DatePickerFragment;
import com.njamb.geodrink.R;

public class RegisterActivity extends AppCompatActivity {

    DialogFragment datePickerFragment;
    EditText username;
    EditText password;
    EditText email;
    EditText birthday;
    Button register;
    Button cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        datePickerFragment = new DatePickerFragment();
        username = (EditText) findViewById(R.id.register_et_username);
        password = (EditText) findViewById(R.id.register_et_password);
        email = (EditText) findViewById(R.id.register_et_email);
        birthday = (EditText) findViewById(R.id.register_et_birthday);
        register = (Button) findViewById(R.id.register_btn_register);
        cancel = (Button) findViewById(R.id.register_btn_cancel);

        // If any input field is empty -> disable 'register' button:
        areFieldsEmpty();

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

        email.addTextChangedListener(new TextWatcher() {
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

        // Set datePicker widget for birthday editText field:
        birthday.setInputType(InputType.TYPE_NULL);

        birthday.addTextChangedListener(new TextWatcher() {
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

        birthday.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    FragmentManager fragmentManager = getFragmentManager();

                    datePickerFragment.show(fragmentManager, "datePicker");

                    areFieldsEmpty();
                }
            }
        });

        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();

                datePickerFragment.show(fragmentManager, "datePicker");

                areFieldsEmpty();
            }
        });



    }

    // Method that checks if any of the input fields are empty:
    private void areFieldsEmpty() {
        if (username.getText().toString().equals("") ||
                password.getText().toString().equals("") ||
                email.getText().toString().equals("") ||
                birthday.getText().toString().equals(""))
        {
            register.setEnabled(false);
        }
        else {
            register.setEnabled(true);
        }
    }

}
