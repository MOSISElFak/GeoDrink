package com.njamb.geodrink.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.njamb.geodrink.R;

import java.util.ArrayList;
import java.util.Arrays;

public class FilterDialogFragment extends DialogFragment {
    public static interface OnCompleteListener {
        public abstract void onComplete(ArrayList<String> checked);
    }

    private OnCompleteListener mListener;
    private ArrayList<String> mCheckedItems = new ArrayList<>();
    private static boolean[] mInitVals = null;

    public FilterDialogFragment() {}


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mListener = (OnCompleteListener)getActivity();

        final String[] filterItems = getResources().getStringArray(R.array.df_filter_items);
        int cnt = filterItems.length;
        if (mInitVals == null) {
            mInitVals = new boolean[cnt];
            Arrays.fill(mInitVals, true);
        }
        for (int i = 0; i < filterItems.length; ++i) {
            if (mInitVals[i]) {
                mCheckedItems.add(filterItems[i]);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Filter by")
                .setMultiChoiceItems(R.array.df_filter_items, mInitVals, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        mInitVals[which] = isChecked;
                        if (isChecked) {
                            mCheckedItems.add(filterItems[which]);
                        }
                        else if (mCheckedItems.contains(filterItems[which])) {
                            mCheckedItems.remove(filterItems[which]);
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onComplete(mCheckedItems);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return builder.create();
    }
}
