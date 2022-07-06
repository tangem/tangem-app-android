package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 28/02/2021.
 */
class SimpleOkDialog {

    companion object {
        fun create(dialog: AppDialog.SimpleOkDialog, context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(dialog.header)
                setMessage(dialog.message)
                setPositiveButton(R.string.common_ok) { _, _ -> }
                setOnDismissListener {
                    store.dispatchDialogHide()
                    dialog.onOk?.invoke()
                }
            }.create()
        }

        fun create(dialog: AppDialog.SimpleOkDialogRes, context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(dialog.headerId))
                setMessage(dialog.messageId)
                setPositiveButton(R.string.common_ok) { _, _ -> }
                setOnDismissListener {
                    store.dispatchDialogHide()
                    dialog.onOk?.invoke()
                }
            }.create()
        }
    }
}
