package com.tangem.di

import android.app.Activity
import android.nfc.Tag
import android.os.Bundle
import com.tangem.domain.wallet.CoinData
import com.tangem.domain.wallet.TangemCard
import com.tangem.presentation.activity.EmptyWalletActivity
import com.tangem.presentation.activity.LoadedWalletActivity
import com.tangem.presentation.activity.MainActivity
import com.tangem.presentation.activity.VerifyCardActivity
import com.tangem.presentation.fragment.LoadedWallet

class Navigator {

    fun showMain(context: Activity) {
        context.startActivity(MainActivity.callingIntent(context))
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, cardInfo: Bundle) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, cardInfo), MainActivity.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity) {
        context.startActivity(EmptyWalletActivity.callingIntent(context))
    }

    fun showVerifyCard(context: Activity, card: TangemCard, coinData: CoinData, message: String, error: String) {
        context.startActivityForResult(VerifyCardActivity.callingIntent(context, card, coinData, message, error), LoadedWallet.REQUEST_CODE_VERIFY_CARD)
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