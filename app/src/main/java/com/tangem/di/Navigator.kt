package com.tangem.di

import android.app.Activity
import android.nfc.Tag
import android.os.Bundle
import com.tangem.Constant
import com.tangem.domain.wallet.CoinData
import com.tangem.domain.wallet.TangemCard
import com.tangem.presentation.activity.*
import com.tangem.presentation.fragment.LoadedWallet

class Navigator {

    fun showMain(context: Activity) {
        context.startActivity(MainActivity.callingIntent(context))
    }

    fun showLogo(context: Activity, autoHide: Boolean) {
        context.startActivity(LogoActivity.callingIntent(context, autoHide))
    }

    fun showPinSave(context: Activity, hasPin2: Boolean) {
        context.startActivity(PinSaveActivity.callingIntent(context, hasPin2))
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, cardInfo: Bundle) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, cardInfo), Constant.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity) {
        context.startActivity(EmptyWalletActivity.callingIntent(context))
    }

    fun showVerifyCard(context: Activity, card: TangemCard, coinData: CoinData) {
        context.startActivityForResult(VerifyCardActivity.callingIntent(context, card, coinData), Constant.REQUEST_CODE_VERIFY_CARD)
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