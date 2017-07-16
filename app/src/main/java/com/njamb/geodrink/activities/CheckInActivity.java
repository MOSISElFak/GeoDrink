package com.njamb.geodrink.activities;

import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.njamb.geodrink.R;

import java.util.ArrayList;
import java.util.List;

public class CheckInActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String CHANGE_LOC_NAME = "Change Name";
    private static final String DONE_CHANGING_LOC_NAME = "Done Changing";

    private ImageView imageView;
    private Button checkInBtn;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = (ImageView) findViewById(R.id.checkin_iv_photo);

        Button addPhoto = (Button) findViewById(R.id.checkin_btn_addphoto);
        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        final Button changeLocNameBtn = (Button) findViewById(R.id.checkIn_btn_changeLocName);
        changeLocNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etName = (EditText) findViewById(R.id.checkin_et_location);

                if (changeLocNameBtn.getText().toString().equals(CHANGE_LOC_NAME)) {
                    etName.setEnabled(true);
                    changeLocNameBtn.setText(DONE_CHANGING_LOC_NAME);
                }
                else {
                    etName.setEnabled(false);
                    changeLocNameBtn.setText(CHANGE_LOC_NAME);
                }

                enableDisableBtn();
            }
        });

        checkInBtn = (Button) findViewById(R.id.checkin_btn_checkin);
        enableDisableBtn();

        EditText etName = (EditText) findViewById(R.id.checkin_et_location);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enableDisableBtn();
            }
        });

        setDrinksList();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageView.setImageBitmap(imageBitmap);

                enableDisableBtn();
            }
        }
    }

    // TODO: Dodati i proveru za pica. Da li je nesto selektovao ili je prazno.
    private boolean checkIfShouldEnable() {
        EditText etName = (EditText) findViewById(R.id.checkin_et_location);
        ImageView ivPhoto = (ImageView) findViewById(R.id.checkin_iv_photo);

        if (etName.getText().toString().equals("") || ivPhoto.getDrawable() == null)
            return false;
        else
            return true;
    }

    private void enableDisableBtn() {
        if (checkIfShouldEnable()) {
            checkInBtn.setEnabled(true);
        }
        else {
            checkInBtn.setEnabled(false);
        }
    }

    private void setDrinksList() {
        List<String> drinksArray = new ArrayList<String>();
        drinksArray.add("Beer");
        drinksArray.add("Coffee");
        drinksArray.add("Cocktail");
        drinksArray.add("Juice");
        drinksArray.add("Soda");
        drinksArray.add("Alcohol");

        ArrayAdapter<String> drinksAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, drinksArray);

        ListView lv = (ListView) findViewById(R.id.checkin_lv_drinks);
        lv.setAdapter(drinksAdapter);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }
}
