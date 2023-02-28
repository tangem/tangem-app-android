package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.databinding.DialogRussiansCardholdersWarningBinding

class RussianCardholdersWarningBottomSheetDialog(
    context: Context,
    private val dialogData: WalletDialog.RussianCardholdersWarningDialog.Data?,
) : BottomSheetDialog
(context) {

    private var binding: DialogRussiansCardholdersWarningBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(Token.Topup.P2PScreenOpened())
        binding = DialogRussiansCardholdersWarningBinding
            .inflate(LayoutInflater.from(context))
            .also { setContentView(it.root) }
    }

    override fun show() {
        super.show()
        setOnDismissListener {
            binding = null
            store.dispatchDialogHide()
        }

        binding?.btnYes?.setOnClickListener {
            if (dialogData != null) {
                store.dispatchOpenUrl(dialogData.topUpUrl)
            } else {
                store.dispatch(WalletAction.TradeCryptoAction.Buy(checkUserLocation = false))
            }
            dismiss()
        }
        binding?.btnNo?.setOnClickListener {
            store.dispatch(NavigationAction.OpenUrl(INSTRUCTION_URL))
            dismiss()
        }
    }

    companion object {
        private const val INSTRUCTION_URL = "https://tangem.com/howtobuy.html"
    }
}
