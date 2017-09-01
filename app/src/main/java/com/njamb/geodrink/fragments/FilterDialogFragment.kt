package com.njamb.geodrink.fragments

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog

import com.njamb.geodrink.R

import java.util.ArrayList
import java.util.Arrays

class FilterDialogFragment : DialogFragment() {
    interface OnCompleteListener {
        fun onComplete(checked: ArrayList<String>)
    }

    private var mListener: OnCompleteListener? = null
    private val mCheckedItems = ArrayList<String>()


    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        mListener = activity as OnCompleteListener

        val filterItems = resources.getStringArray(R.array.df_filter_items)
        val cnt = filterItems.size
        if (mInitVals == null) {
            mInitVals = BooleanArray(cnt)
            Arrays.fill(mInitVals!!, true)
        }
        for (i in filterItems.indices) {
            if (mInitVals!![i]) {
                mCheckedItems.add(filterItems[i])
            }
        }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Filter by")
                .setMultiChoiceItems(R.array.df_filter_items, mInitVals) { dialog, which, isChecked ->
                    mInitVals[which] = isChecked
                    if (isChecked) {
                        mCheckedItems.add(filterItems[which])
                    } else if (mCheckedItems.contains(filterItems[which])) {
                        mCheckedItems.remove(filterItems[which])
                    }
                }
                .setPositiveButton("OK") { dialog, which -> mListener!!.onComplete(mCheckedItems) }
                .setNegativeButton("Cancel") { dialog, which -> }

        return builder.create()
    }

    companion object {
        private var mInitVals: BooleanArray? = null
    }
}
