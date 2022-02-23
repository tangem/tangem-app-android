package com.tangem.tap.features.onboarding

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.dispatchShare
import com.tangem.tap.common.extensions.dispatchToastNotification
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_onboarding_address_info.*
import kotlinx.android.synthetic.main.layout_pseudo_toolbar.*

/**
[REDACTED_AUTHOR]
 */
class AddressInfoBottomSheetDialog(
    private val stateDialog: AppDialog.AddressInfoDialog,
    context: Context
) : BottomSheetDialog(context) {

    init {
        this.setContentView(R.layout.dialog_onboarding_address_info)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setOnCancelListener { store.dispatchDialogHide() }
    }

    override fun show() {
        super.show()
        val data = stateDialog.addressData

        imv_close.setOnClickListener {
            dismissWithAnimation = true
            cancel()
        }
        imv_qr_code.setImageBitmap(data.qrCode)
        tv_address.text = data.address
        btn_fl_copy_address.setOnClickListener {
            context.copyToClipboard(data.address)
            store.dispatchToastNotification(R.string.copy_toast_msg)
        }
        btn_fl_share.setOnClickListener {
            store.dispatchShare(data.shareUrl)
        }
        tv_receive_message.text = getQRReceiveMessage(tv_receive_message.context, stateDialog.currency)
    }
}

fun getQRReceiveMessage(context: Context, currency: Currency): String {
    return when (currency) {
        is Currency.Blockchain -> {
            context.getString(
                R.string.address_qr_code_message_format,
                currency.blockchain.fullName,
                currency.currencySymbol
            )
        }
        is Currency.Token -> {
            context.getString(
                R.string.address_qr_code_message_token_format,
                currency.token.name,
                currency.currencySymbol,
                currency.blockchain.fullName
            )
        }
    }
}