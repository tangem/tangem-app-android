package com.tangem.tap.features.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.notificationsHandler
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_create_payid.*

class PayIdDialog(context: Context) : Dialog(context) {

    init {
        this.setContentView(R.layout.dialog_create_payid)
    }

    override fun show() {
        notificationsHandler?.replaceBaseLayout(this.cl_payid_dialog)
        this.setOnDismissListener {
            notificationsHandler?.returnBaseLayout()
            store.dispatch(WalletAction.CreatePayId.Cancel)
        }
        this.btn_create_payid?.setOnClickListener {
            if (this.et_payid.text.isNullOrBlank()) {
                store.dispatch(WalletAction.CreatePayId.EmptyField)
            } else {
                val payid = this.et_payid.text!!.toString() +
                        this.context.getString(R.string.wallet_pay_id_address)
                store.dispatch(WalletAction.CreatePayId.CompleteCreatingPayId(payid))
            }
        }
        super.show()
    }

    fun showProgress() {
        this.btn_create_payid?.visibility = View.INVISIBLE
        this.pb_create_payid?.show()
    }

    fun stopProgress() {
        this.btn_create_payid?.show()
        this.pb_create_payid?.hide()
    }
}