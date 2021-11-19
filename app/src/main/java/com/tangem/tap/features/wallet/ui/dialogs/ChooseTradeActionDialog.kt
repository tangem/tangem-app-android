package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_wallet_trade.*

class ChooseTradeActionDialog(context: Context) : BottomSheetDialog(context) {

    init {
        this.setContentView(R.layout.dialog_wallet_trade)
    }

    override fun show() {
        super.show()

        this.setOnDismissListener {
            store.dispatch(WalletAction.HideDialog)
        }

        dialog_btn_buy.setOnClickListener {
            store.dispatch(WalletAction.TradeCryptoAction.Buy)
            dismiss()
        }
        dialog_btn_sell.setOnClickListener {
            store.dispatch(WalletAction.TradeCryptoAction.Sell)
            dismiss()
        }
    }
}