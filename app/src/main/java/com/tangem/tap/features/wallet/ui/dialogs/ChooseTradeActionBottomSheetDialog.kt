package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.databinding.DialogWalletTradeBinding

class ChooseTradeActionBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    var binding: DialogWalletTradeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogWalletTradeBinding.inflate(LayoutInflater.from(context))
        setContentView(binding!!.root)
    }

    override fun show() {
        super.show()

        this.setOnDismissListener {
            binding = null
            store.dispatch(WalletAction.DialogAction.Hide)
        }

        binding!!.dialogBtnBuy.setOnClickListener {
            dismiss()
            store.dispatch(WalletAction.TradeCryptoAction.Buy())
        }
        binding!!.dialogBtnSell.setOnClickListener {
            dismiss()
            store.dispatch(WalletAction.TradeCryptoAction.Sell)
        }
    }
}
