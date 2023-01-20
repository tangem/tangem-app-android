package com.tangem.tap.common.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

object SimpleAlertDialog {
    fun create(
        titleRes: Int? = null,
        messageRes: Int? = null,
        title: String? = null,
        message: String? = null,
        primaryButtonRes: Int = R.string.common_ok,
        context: Context,
    ): AlertDialog {
        return SimpleCancelableAlertDialog.create(
            titleRes = titleRes,
            messageRes = messageRes,
            title = title,
            message = message,
            primaryButtonRes = primaryButtonRes,
            secondaryButtonRes = null,
            context = context,
        )
    }
}

object SimpleCancelableAlertDialog {
    fun create(
        titleRes: Int? = null,
        messageRes: Int? = null,
        title: String? = null,
        message: String? = null,
        primaryButtonRes: Int = R.string.common_ok,
        secondaryButtonRes: Int? = R.string.common_cancel,
        primaryButtonAction: () -> Unit = {},
        secondaryButtonAction: () -> Unit = {},
        context: Context,
    ): AlertDialog {
        return AlertDialog.Builder(context).apply {
            setTitle(titleRes?.let { context.getString(it) } ?: title)
            setMessage(messageRes?.let { context.getString(it) } ?: message)
            setPositiveButton(context.getText(primaryButtonRes)) { _, _ -> primaryButtonAction() }
            if (secondaryButtonRes != null) {
                setNegativeButton(context.getText(secondaryButtonRes)) { _, _ -> secondaryButtonAction() }
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}
