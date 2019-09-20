package com.tangem.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

import com.tangem.wallet.R

class NoExtendedLengthSupportDialog : DialogFragment() {

    companion object {
        val TAG: String = NoExtendedLengthSupportDialog::class.java.simpleName
        var allReadyShowed = false
        var message = ""// = R.string.the_nfc_adapter_length_apdu
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.dialog_warning)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_btn_got_it) { _, _ -> NoExtendedLengthSupportDialog.allReadyShowed = false }
                .create()
    }

}