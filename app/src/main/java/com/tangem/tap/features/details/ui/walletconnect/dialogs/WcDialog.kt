package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog

class WcDialog {
    companion object {
        fun create(dialogData: DialogData, context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(dialogData.title)
                setMessage(dialogData.message)
                setPositiveButton(dialogData.positiveButton.title) { _, _ ->
                    dialogData.positiveButton.action()
                }
                setNegativeButton(dialogData.negativeButton.title) { _, _ ->
                    dialogData.negativeButton.action()
                }
                setOnDismissListener { dialogData.onDismissAction() }
            }.create()
        }
    }
}

data class DialogData(
    val title: String,
    val message: String,
    val positiveButton: ButtonData,
    val negativeButton: ButtonData,
    val onDismissAction: () -> Unit,
)

data class ButtonData(
    val title: String,
    val action: () -> Unit,
)

//class WcDialogMessageBuilder(val messageRes: Int, val data: DialogMessageData? = null) {
//    fun build(context: Context): String {
//        when (data) {
//            null -> context.getString(messageRes)
//        }
//    }
//}

interface DialogMessageData

data class WcTransactionDialogMessageData(
    val cardId: String,
    val dAppName: String,
    val dAppUrl: String,
    val amount: String,
    val gasAmount: String,
    val totalAmount: String,
    val balance: String,
    val isEnoughFundsToSend: Boolean,
)