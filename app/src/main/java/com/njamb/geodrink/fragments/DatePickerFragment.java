package com.njamb.geodrink.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;

import com.njamb.geodrink.R;
import com.njamb.geodrink.authentication.RegisterActivity;

import java.util.Calendar;


public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), R.style.DialogTheme, this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // chosen date
        RegisterActivity registerActivity = (RegisterActivity) getActivity();
        EditText editText = (EditText) registerActivity.findViewById(R.id.register_et_birthday);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dayOfMonth);
        stringBuilder.append("/");
        stringBuilder.append(++month);
        stringBuilder.append("/");
        stringBuilder.append(year);

        // Birthday editText on RegisterActivity:
        editText.setText(stringBuilder.toString());
    }
}
