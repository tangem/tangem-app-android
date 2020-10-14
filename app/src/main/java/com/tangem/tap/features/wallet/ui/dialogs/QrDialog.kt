package com.tangem.tap.features.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_qrcode.*

class QrDialog(context: Context) : Dialog(context) {

    init {
        this.setContentView(R.layout.dialog_qrcode)
    }

    fun showQr(qrCode: Bitmap, shareUrl: String, currencyName: CryptoCurrencyName?) {
        this.setOnDismissListener { store.dispatch(WalletAction.HideDialog) }
        this.btn_done?.setOnClickListener { store.dispatch(WalletAction.HideDialog) }

        this.tv_qr_dialog_address?.text = shareUrl
        this.iv_qrcode?.setImageBitmap(qrCode)
        if (currencyName == null) {
            this.tv_qr_dialog_header.hide()
        } else {
            this.tv_qr_dialog_header.show()
            this.tv_qr_dialog_header.text = context.getString(
                    R.string.wallet_qr_title_format, currencyName
            )
        }
        super.show()
    }
}