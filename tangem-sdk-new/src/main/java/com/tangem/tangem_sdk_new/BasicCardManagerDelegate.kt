package com.tangem.tangem_sdk_new

import android.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.tangem.CardError
import com.tangem.CardManagerDelegate

class BasicCardManagerDelegate(val activity: FragmentActivity) : CardManagerDelegate {

    private var dialog: AlertDialog? = null

    override fun showSecurityDelay(seconds: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideSecurityDelay() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestPin(success: () -> String, error: (cardError: CardError) -> CardError) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openNfcPopup() {

        dialog = AlertDialog.Builder(activity)
                .setTitle("NFC reader mode")
                .create()
        dialog?.show()

    }

    override fun closeNfcPopup() {
        dialog?.dismiss()
    }
}