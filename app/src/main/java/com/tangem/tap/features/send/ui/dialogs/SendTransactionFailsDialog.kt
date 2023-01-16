package com.tangem.tap.features.send.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.tangem_sdk_new.extensions.localizedDescription
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.feedback.SendTransactionFailedEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 28/02/2021.
 */
object SendTransactionFailsDialog {
    fun create(context: Context, dialog: SendAction.Dialog.SendTransactionFails.CardSdkError): AlertDialog {
        return create(context, dialog.error.localizedDescription(context))
    }

    fun create(context: Context, dialog: SendAction.Dialog.SendTransactionFails.BlockchainSdkError): AlertDialog {
        val errorConverter = BlockchainSdkErrorConverter(context)
        return create(context, errorConverter.convert(dialog.error))
    }

    private fun create(context: Context, errorMessage: String): AlertDialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.alert_failed_to_send_transaction_title)
            setMessage(context.getString(R.string.alert_failed_to_send_transaction_message, errorMessage))
            setNeutralButton(R.string.alert_button_send_feedback) { _, _ ->
                store.dispatch(GlobalAction.SendEmail(SendTransactionFailedEmail(errorMessage)))
            }
            setPositiveButton(R.string.common_cancel) { _, _ -> }
            setOnDismissListener { store.dispatch(SendAction.Dialog.Hide) }
        }.create()
    }
}

private class BlockchainSdkErrorConverter(
    private val context: Context,
) : ModuleMessageConverter<BlockchainSdkError, String> {

    override fun convert(message: BlockchainSdkError): String {
        return when (message) {
            is BlockchainSdkError.CreateAccountUnderfunded -> {
                val resStringId = when (message.blockchain) {
                    Blockchain.Polkadot, Blockchain.PolkadotTestnet -> R.string.no_account_polkadot
                    else -> R.string.send_error_no_target_account
                }
                val reserveValueString = message.minReserve.value?.stripZeroPlainString() ?: "0"
                val argument = "$reserveValueString ${message.minReserve.currencySymbol}"
                context.getString(resStringId, argument)
            }
            else -> message.customMessage
        }
    }
}
