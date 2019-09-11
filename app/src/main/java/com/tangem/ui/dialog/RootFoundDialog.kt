package com.tangem.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

import com.tangem.wallet.R

class RootFoundDialog : DialogFragment() {

    companion object {
        val TAG: String = RootFoundDialog::class.java.simpleName
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.dialog_device_is_rooted)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_btn_got_it, null)
                .create()
    }

}