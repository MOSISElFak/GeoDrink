package com.njamb.geodrink.Activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.njamb.geodrink.Classes.DatePickerFragment;
import com.njamb.geodrink.Classes.User;
import com.njamb.geodrink.R;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    DialogFragment datePickerFragment;
    EditText username;
    EditText password;
    EditText email;
    EditText birthday;
    Button register;
    Button cancel;

    private ProgressDialog pd;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        datePickerFragment = new DatePickerFragment();
        username = (EditText) findViewById(R.id.register_et_username);
        password = (EditText) findViewById(R.id.register_et_password);
        email = (EditText) findViewById(R.id.register_et_email);
        birthday = (EditText) findViewById(R.id.register_et_birthday);
        register = (Button) findViewById(R.id.register_btn_register);
        cancel = (Button) findViewById(R.id.register_btn_cancel);

        configProgressDialog();

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


        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                pd.show();
                registerUser();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    private void configProgressDialog() {
        pd = new ProgressDialog(this, R.style.TransparentProgressDialogStyle);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
    }

    // Method that checks if any of the input fields are empty:
    private void areFieldsEmpty() {
        if (username.getText().toString().trim().equals("") ||
                password.getText().toString().trim().equals("") ||
                email.getText().toString().trim().equals("") ||
                birthday.getText().toString().trim().equals(""))
        {
            register.setEnabled(false);
        }
        else {
            register.setEnabled(true);
        }
    }

    private void registerUser() {
        String emailReg = email.getText().toString();
        String passwordReg = password.getText().toString();
        mAuth.createUserWithEmailAndPassword(emailReg, passwordReg)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        pd.dismiss();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            createUserProfile(user);
                        }
                        else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createUserProfile(FirebaseUser user) {
        User dbUser = new User(username.getText().toString(), email.getText().toString(),
                birthday.getText().toString());

        mDatabase.child("users").child(user.getUid()).setValue(dbUser);

        setResult(Activity.RESULT_OK);
        finish();
    }

}
