package com.tangem.di;

import android.content.Context;

import com.tangem.presentation.activity.MainActivity;

public class Navigator {

    public void showMain(Context context) {
        context.startActivity(MainActivity.Companion.callingIntent(context));
    }

}