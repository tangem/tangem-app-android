package com.tangem.tap.features.send.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.tap.common.feedback.SendTransactionFailedEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
object RequestFeeErrorDialog {
    fun create(context: Context, dialog: SendAction.Dialog.RequestFeeError): AlertDialog {
        val errorMessage = dialog.error.customMessage

        return AlertDialog.Builder(context).apply {
            setTitle(R.string.common_fee_error)
            setMessage(context.getString(R.string.alert_failed_to_send_transaction_message, errorMessage))
            setNegativeButton(R.string.details_row_title_contact_to_support) { _, _ ->
                Analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.Send))
                store.dispatch(
                    GlobalAction.SendEmail(
                        feedbackData = SendTransactionFailedEmail(errorMessage),
                        scanResponse = dialog.scanResponse,
                    ),
                )
            }
            setPositiveButton(R.string.common_retry) { _, _ -> dialog.onRetry() }
            setNeutralButton(R.string.common_cancel) { _, _ -> }
            setOnDismissListener { store.dispatch(SendAction.Dialog.Hide) }
        }.create()
    }
}
