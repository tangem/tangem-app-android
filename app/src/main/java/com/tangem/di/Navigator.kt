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

    fun showQrScanActivity(context: Activity, requestCode: Int) {
        context.startActivityForResult(QrScanActivity.callingIntent(context), requestCode)
    }

    fun showPinRequestRequestPin(context: Activity, mode: String, ctx: TangemContext, newPin: String) {
        context.startActivityForResult(PinRequestActivity.callingIntentRequestPin(context, mode, ctx, newPin), Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    fun showPinRequestRequestPin2(context: Activity, mode: String, ctx: TangemContext, newPin2: String) {
        context.startActivityForResult(PinRequestActivity.callingIntentRequestPin2(context, mode, ctx, newPin2), Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    fun showPinRequestRequestPin2(context: Activity, mode: String, ctx: TangemContext) {
        context.startActivityForResult(PinRequestActivity.callingIntentRequestPin2(context, mode, ctx), Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    fun showPinRequestConfirmNewPin(context: Activity, mode: String, newPin: String) {
        context.startActivityForResult(PinRequestActivity.callingIntentConfirmPin(context, mode, newPin), Constant.REQUEST_CODE_ENTER_NEW_PIN)
    }

    fun showPinRequestConfirmNewPin2(context: Activity, mode: String, newPin2: String) {
        context.startActivityForResult(PinRequestActivity.callingIntentConfirmPin2(context, mode, newPin2), Constant.REQUEST_CODE_ENTER_NEW_PIN2)
    }

    fun showLoadedWallet(context: Activity, lastTag: Tag, ctx: TangemContext) {
        context.startActivityForResult(LoadedWalletActivity.callingIntent(context, lastTag, ctx), Constant.REQUEST_CODE_SHOW_CARD_ACTIVITY)
    }

    fun showEmptyWallet(context: Activity, ctx: TangemContext) {
        context.startActivity(EmptyWalletActivity.callingIntent(context, ctx))
    }

    fun showVerifyCard(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(VerifyCardActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_VERIFY_CARD)
    }

    fun showPinSwap(context: Activity, newPIN: String, newPIN2: String) {
        context.startActivityForResult(PinSwapActivity.callingIntent(context, newPIN, newPIN2), Constant.REQUEST_CODE_SWAP_PIN)
    }

    fun showPurge(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(PurgeActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_PURGE)
    }

    fun showPreparePayment(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(PreparePaymentActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_SEND_PAYMENT)
    }

    fun showCreateNewWallet(context: Activity, ctx: TangemContext) {
        context.startActivityForResult(CreateNewWalletActivity.callingIntent(context, ctx), Constant.REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY)
    }

}