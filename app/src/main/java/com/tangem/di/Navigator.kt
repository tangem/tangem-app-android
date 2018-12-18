package com.tangem.di

import android.app.Activity
import android.nfc.Tag
import com.tangem.Constant
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.activity.*

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

    fun showPinRequest(context: Activity, mode: String) {
        context.startActivityForResult(PinRequestActivity.callingIntent(context, mode), Constant.REQUEST_CODE_ENTER_PIN_ACTIVITY)
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, ctx: TangemContext) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, ctx), Constant.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity, ctx: TangemContext) {
        context.startActivity(EmptyWalletActivity.callingIntent(context ,ctx))
    }

    fun showVerifyCard(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(VerifyCardActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_VERIFY_CARD)
    }

    fun showPinSwap(context: Activity, newPIN: String, newPIN2: String) {
        context.startActivityForResult(PinSwapActivity.callingIntent(context, newPIN, newPIN2), Constant.REQUEST_CODE_SWAP_PIN)
    }

    fun showPurge(context: Activity) {

    }

    fun showPreparePayment(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(PreparePaymentActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_SEND_PAYMENT)
    }

    fun showCreateNewWallet(context: Activity) {

    }

}