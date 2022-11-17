package com.tangem.tap.features.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.dispatchShare
import com.tangem.tap.common.extensions.dispatchToastNotification
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.DialogOnboardingAddressInfoBinding

/**
 * Created by Anton Zhilenkov on 24/09/2021.
 */
class AddressInfoBottomSheetDialog(
    private val stateDialog: AppDialog.AddressInfoDialog,
    context: Context,
) : BottomSheetDialog(context) {

    var binding: DialogOnboardingAddressInfoBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogOnboardingAddressInfoBinding
            .inflate(LayoutInflater.from(context))
        setContentView(binding!!.root)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setOnCancelListener {
            store.dispatchDialogHide()
            binding = null
        }
    }

    override fun show() {
        super.show()
        Analytics.send(Token.Recieve.ScreenOpened())
        showData(data = stateDialog.addressData)
    }

    private fun showData(data: AddressData) = with(binding!!) {
        pseudoToolbar.imvClose.setOnClickListener {
            dismissWithAnimation = true
            cancel()
        }
        imvQrCode.setImageBitmap(data.qrCode)
        tvAddress.text = data.address
        btnFlCopyAddress.setOnClickListener {
            Analytics.send(Token.Recieve.ButtonCopyAddress())
            context.copyToClipboard(data.address)
            store.dispatchToastNotification(R.string.copy_toast_msg)
        }
        btnFlShare.setOnClickListener {
            Analytics.send(Token.Recieve.ButtonShareAddress())
            store.dispatchShare(data.shareUrl)
        }
        tvReceiveMessage.text = getQRReceiveMessage(tvReceiveMessage.context, stateDialog.currency)
    }
}

fun getQRReceiveMessage(context: Context, currency: Currency): String {
    return when (currency) {
        is Currency.Blockchain -> {
            context.getString(
                R.string.address_qr_code_message_format,
                currency.blockchain.fullName,
                currency.currencySymbol,
            )
        }
        is Currency.Token -> {
            context.getString(
                R.string.address_qr_code_message_token_format,
                currency.token.name,
                currency.currencySymbol,
                currency.blockchain.fullName,
            )
        }
    }
}
