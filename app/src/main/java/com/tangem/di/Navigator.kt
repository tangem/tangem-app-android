package com.tangem.di

import android.app.Activity
import android.nfc.Tag
import android.os.Bundle
import com.tangem.domain.wallet.CoinData
import com.tangem.domain.wallet.TangemContext
import com.tangem.tangemcard.data.TangemCard
import com.tangem.presentation.activity.EmptyWalletActivity
import com.tangem.presentation.activity.LoadedWalletActivity
import com.tangem.presentation.activity.MainActivity
import com.tangem.presentation.activity.VerifyCardActivity
import com.tangem.presentation.fragment.LoadedWallet

class Navigator {

    fun showMain(context: Activity) {
        context.startActivity(MainActivity.callingIntent(context))
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, ctx: TangemContext) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, ctx), MainActivity.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity, ctx: TangemContext) {
        context.startActivity(EmptyWalletActivity.callingIntent(context ,ctx))
    }

    fun showVerifyCard(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(VerifyCardActivity.callingIntent(context, ctx), LoadedWallet.REQUEST_CODE_VERIFY_CARD)
    }

    fun showPinSwap(context: Activity) {

    }

//    fun showPreparePayment(context: Activity) {
//
//    }
//
//    fun showCreateNewWallet(context: Activity) {
//
//    }

}