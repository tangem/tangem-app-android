package com.tangem.tap.features.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_qrcode.*

class QrDialog(context: Context) : Dialog(context) {

    init {
        this.setContentView(R.layout.dialog_qrcode)
    }

    fun showQr(qrCode: Bitmap, shareUrl: String) {
        this.setOnDismissListener { store.dispatch(WalletAction.HideQrCode) }
        this.btn_done?.setOnClickListener { store.dispatch(WalletAction.HideQrCode) }

        this.tv_qr_dialog_address?.text = shareUrl
        this.iv_qrcode?.setImageBitmap(qrCode)
        super.show()
    }
}