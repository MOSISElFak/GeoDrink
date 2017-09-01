package com.njamb.geodrink.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.DatePicker
import android.widget.EditText

import com.njamb.geodrink.R
import com.njamb.geodrink.authentication.RegisterActivity

import java.util.Calendar


class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstance: Bundle): Dialog {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(activity, R.style.DialogTheme, this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        var month = month
        // chosen date
        val registerActivity = activity as RegisterActivity
        val editText = registerActivity.findViewById(R.id.register_et_birthday) as EditText

        val stringBuilder = StringBuilder()
        stringBuilder.append(dayOfMonth)
        stringBuilder.append("/")
        stringBuilder.append(++month)
        stringBuilder.append("/")
        stringBuilder.append(year)

        // Birthday editText on RegisterActivity:
        editText.setText(stringBuilder.toString())
    }
}
