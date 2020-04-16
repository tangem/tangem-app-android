package com.tangem.tangemtest.commons

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog

class DialogController {
    var onDismissCallback: (() -> Unit)? = null
    var onShowCallback: (() -> Unit)? = null
    var view: View? = null

    private var rawDialog: Dialog? = null

    private var inShowingProcess = false
    private var inDismissingProcess = false
    private var autoReleaseOnDismiss = false

    fun createAlert(context: Activity, resLayout: Int): AlertDialog {
        view = LayoutInflater.from(context).inflate(resLayout, null)
        rawDialog = AlertDialog.Builder(context).setView(view).create().apply {
            setOnShowListener { onShow() }
            setOnDismissListener { onDismiss() }
        }
        return rawDialog as AlertDialog
    }

    fun set(dialog: Dialog) {
        rawDialog = dialog
        dialog.setOnShowListener { onShow() }
        dialog.setOnDismissListener { onDismiss() }
    }

    private fun onShow() {
        inShowingProcess = false
        onShowCallback?.invoke()
    }

    private fun onDismiss() {
        inDismissingProcess = false
        if (autoReleaseOnDismiss) release()
        onDismissCallback?.invoke()
    }

    fun show() {
        val dialog = rawDialog ?: return
        if (inShowingProcess) return
        if (dialog.isShowing) return

        inShowingProcess = true
        dialog.show()
    }

    fun dismiss(autoRelease: Boolean = true) {
        val dialog = rawDialog ?: return
        if (inDismissingProcess) return
        if (!dialog.isShowing) return

        autoReleaseOnDismiss = autoRelease
        inDismissingProcess = true
        dialog.dismiss()
    }

    fun release() {
        onDismissCallback = null
        onShowCallback = null

        inShowingProcess = false
        inDismissingProcess = false
        autoReleaseOnDismiss = false

        view = null
        rawDialog = null
    }
}