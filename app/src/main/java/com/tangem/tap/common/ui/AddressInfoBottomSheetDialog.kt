package com.tangem.tap.common.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.domain.model.WalletAddressData
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.DialogOnboardingAddressInfoBinding

/**
[REDACTED_AUTHOR]
 */
internal class AddressInfoBottomSheetDialog(
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
        Analytics.send(Token.Receive.ScreenOpened(stateDialog.currency.currencySymbol))
        showData(data = stateDialog.addressData)
    }

    private fun showData(data: WalletAddressData) = with(binding!!) {
        pseudoToolbar.imvClose.setOnClickListener {
            dismissWithAnimation = true
            cancel()
        }
        imvQrCode.setImageBitmap(data.shareUrl.toQrCode())
        tvAddress.text = data.address
        btnFlCopyAddress.setOnClickListener {
            Analytics.send(Token.Receive.ButtonCopyAddress())

            Toast
                .makeText(context, R.string.wallet_notification_address_copied, Toast.LENGTH_SHORT)
                .show()

            store.inject(DaggerGraphState::clipboardManager).setText(text = data.address, isSensitive = true)
        }
        btnFlShare.setOnClickListener {
            Analytics.send(Token.Receive.ButtonShareAddress())
            store.dispatchShare(data.shareUrl)
        }
        val blockchain = stateDialog.currency.blockchain
        tvReceiveMessage.text = tvReceiveMessage.getString(
            id = R.string.address_qr_code_message_format,
            blockchain.fullName,
            blockchain.currency,
            blockchain.fullName,
        )
    }
}