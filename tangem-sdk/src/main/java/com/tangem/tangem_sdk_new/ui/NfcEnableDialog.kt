package com.tangem.tangem_sdk_new.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.tangem.tangem_sdk_new.R

class NfcEnableDialog {

    fun show(activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
                .setIcon(R.drawable.ic_action_nfc_gray)
                .setTitle(R.string.dialog_nfc_enable_title)
                .setMessage(R.string.dialog_nfc_enable_text)
                .setPositiveButton(R.string.general_ok
                ) { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
        builder.create().show()
    }
}