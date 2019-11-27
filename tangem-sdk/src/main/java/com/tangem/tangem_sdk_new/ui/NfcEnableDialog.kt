package com.tangem.tangem_sdk_new.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tangem.tangem_sdk_new.R

class NfcEnableDialog : DialogFragment() {
    companion object {
        val TAG: String = NfcEnableDialog::class.java.simpleName
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_action_nfc_gray)
                .setTitle(R.string.dialog_nfc_enable_title)
                .setMessage(R.string.dialog_nfc_enable_text)
                .setPositiveButton(R.string.general_ok
                ) { _, _ ->
                    activity?.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
        return builder.create()
    }

}