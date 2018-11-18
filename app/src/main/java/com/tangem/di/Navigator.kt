package com.tangem.di

import android.app.Activity
import android.content.Context
import android.nfc.Tag
import android.os.Bundle
import com.tangem.presentation.activity.EmptyWalletActivity
import com.tangem.presentation.activity.LoadedWalletActivity

import com.tangem.presentation.activity.MainActivity

class Navigator {

    fun showMain(context: Context) {
        context.startActivity(MainActivity.callingIntent(context))
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, cardInfo: Bundle) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, cardInfo), MainActivity.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity) {
        context.startActivity(EmptyWalletActivity.callingIntent(context))
    }

    fun showVerifyCard(context: Context) {

    }



    fun showCreateNewWallet(context: Context) {

    }

}