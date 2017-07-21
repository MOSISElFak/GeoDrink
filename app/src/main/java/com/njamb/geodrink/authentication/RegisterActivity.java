package com.njamb.geodrink.authentication;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Past;
import com.njamb.geodrink.fragments.DatePickerFragment;
import com.njamb.geodrink.models.User;
import com.njamb.geodrink.R;

import java.util.List;

public class RegisterActivity extends AppCompatActivity implements Validator.ValidationListener {
    private static final String TAG = "RegisterActivity";
    private static final int SELECT_IMAGE_REQUEST = 1;

    private DialogFragment datePickerFragment;

    @NotEmpty private EditText username;

    @NotEmpty
    @Password(message = "Password needs to have at least 6 characters")
    private EditText password;

    @NotEmpty private EditText name;

    @NotEmpty
    @Email private EditText email;

    @NotEmpty
    @Past(dateFormat = "dd/MM/yyyy")
    private EditText birthday;

    private Button register;
    private Button pickImage;
    private Button cancel;
    private ImageView loginImage;
    private Uri mImageUri = null;

    private ProgressDialog pd;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private Validator mValidator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        datePickerFragment = new DatePickerFragment();
        username = (EditText) findViewById(R.id.register_et_username);
        password = (EditText) findViewById(R.id.register_et_password);
        name = (EditText) findViewById(R.id.register_et_name);
        email = (EditText) findViewById(R.id.register_et_email);
        birthday = (EditText) findViewById(R.id.register_et_birthday);
        register = (Button) findViewById(R.id.register_btn_register);
        pickImage = (Button) findViewById(R.id.register_btn_pickimage);
        cancel = (Button) findViewById(R.id.register_btn_cancel);
        loginImage = (ImageView) findViewById(R.id.register_iv_loginimage);

        configProgressDialog();

        // If any input field is empty -> disable 'register' button:
//        areFieldsEmpty();

//        username.addTextChangedListener(new TextWatcher() {
//                    @Override
//                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                    }
//
//                    @Override
//                    public void onTextChanged(CharSequence s, int start, int before, int count) {
//                        areFieldsEmpty();
//                    }
//
//                    @Override
//                    public void afterTextChanged(Editable s) {
//
//                    }
//                });

//        password.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                areFieldsEmpty();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

//        name.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                areFieldsEmpty();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

//        email.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                areFieldsEmpty();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

        // Set datePicker widget for birthday editText field:
        birthday.setInputType(InputType.TYPE_NULL);

//        birthday.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                areFieldsEmpty();
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

        birthday.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    FragmentManager fragmentManager = getFragmentManager();

                    datePickerFragment.show(fragmentManager, "datePicker");

//                    areFieldsEmpty();
                }
            }
        });

        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();

                datePickerFragment.show(fragmentManager, "datePicker");

//                areFieldsEmpty();
            }
        });


        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mValidator.validate();
//                registerUser();
            }
        });

        pickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add picture select and change the present imageView showing the AppLogo.
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                Intent chooser = Intent.createChooser(intent, "Select Picture");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(chooser, SELECT_IMAGE_REQUEST);
                }
                else {
                    Toast.makeText(RegisterActivity.this, "No available apps for this action.",
                            Toast.LENGTH_SHORT).show();
                }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_IMAGE_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        mImageUri = data.getData();
                        Glide.with(this).load(mImageUri).into(loginImage);
                    }
                }
            }
        }
    }

    private void configProgressDialog() {
        pd = new ProgressDialog(this, R.style.TransparentProgressDialogStyle);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
    }

    // Method that checks if any of the input fields are empty:
//    private void areFieldsEmpty() {
//        if (username.getText().toString().trim().equals("") ||
//                password.getText().toString().trim().equals("") ||
//                name.getText().toString().trim().equals("") ||
//                email.getText().toString().trim().equals("") ||
//                birthday.getText().toString().trim().equals("") ||
//                loginImage.getDrawable() == null)
//        {
//            register.setEnabled(false);
//        }
//        else {
//            register.setEnabled(true);
//        }
//    }

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
                            uploadProfilePhoto(user.getUid());
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
        User dbUser = new User(
                name.getText().toString(),
                username.getText().toString(),
                email.getText().toString(),
                birthday.getText().toString()
        );

        mDatabase.child("users").child(user.getUid()).setValue(dbUser);

        setResult(Activity.RESULT_OK);
        finish();
    }

    private void uploadProfilePhoto(final String userId) {
        final String imageUrl = String.format("images/users/%s.jpg", userId);
        StorageReference imageRef = FirebaseStorage.getInstance().getReference(imageUrl);
        UploadTask uploadTask = imageRef.putFile(mImageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(RegisterActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests")
                String imageUrl = taskSnapshot.getDownloadUrl().toString();
                mDatabase.child("users").child(userId).child("profileUrl").setValue(imageUrl);
            }
        });
    }

    @Override
    public void onValidationSucceeded() {
        if (mImageUri == null) {
            Toast.makeText(this, "You need to pick image", Toast.LENGTH_SHORT).show();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        pd.show();
        registerUser();
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
