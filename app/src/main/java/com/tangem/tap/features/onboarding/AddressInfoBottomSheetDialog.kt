package com.tangem.tap.features.onboarding

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tap.common.extensions.copyToClipboard
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.AppDialog
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
            stateDialog.onCopyAddress()
        }
        btn_fl_explore.setOnClickListener {
            stateDialog.onExploreAddress()
        }
    }
}