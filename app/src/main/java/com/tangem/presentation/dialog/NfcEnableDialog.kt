package com.tangem.presentation.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog

import com.tangem.wallet.R

class NfcEnableDialog : DialogFragment() {

    companion object {
        val TAG: String = NfcEnableDialog::class.java.simpleName
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_action_nfc_gray)
                .setTitle(R.string.nfc_disabled)
                .setMessage(R.string.enable_nfc)
                .setPositiveButton(R.string.dialog_ok
                ) { _, _ ->
                    // take user to wireless settings
                    activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                .setNegativeButton(R.string.dialog_quit
                ) { dialog, _ ->
                    dialog.cancel()
                    activity.finish()
                }
        return builder.create()
    }

}