package com.tangem.tap.common.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 28/02/2021.
 */
object SimpleOkDialog {

    fun create(dialog: AppDialog.SimpleOkDialogRes, context: Context): AlertDialog {
        val message = if (dialog.args.isEmpty()) {
            context.getString(dialog.messageId)
        } else {
            context.getString(dialog.messageId, *dialog.args.toTypedArray())
        }
        return AlertDialog.Builder(context).apply {
            setTitle(context.getString(dialog.headerId))
            setMessage(message)
            setPositiveButton(R.string.common_ok) { _, _ -> }
            setOnDismissListener {
                store.dispatchDialogHide()
                dialog.onOk?.invoke()
            }
        }.create()
    }
}
