package com.tangem.tangemcard.android.presentation.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import com.tangem.tangemcard.R

class NfcEnableDialog : DialogFragment() {
    companion object {
        val TAG: String = NfcEnableDialog::class.java.simpleName
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = context?.let { AlertDialog.Builder(it) }
        builder?.setCancelable(false)
                ?.setIcon(R.drawable.ic_action_nfc_gray)
                ?.setTitle(R.string.nfc_disabled)
                ?.setMessage(R.string.enable_nfc)
                ?.setPositiveButton(R.string.dialog_ok
                ) { _, _ ->
                    activity?.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                ?.setNegativeButton(R.string.dialog_quit
                ) { dialog, _ ->
                    dialog.cancel()
                    activity?.finish()
                }
        return builder!!.create()
    }

}