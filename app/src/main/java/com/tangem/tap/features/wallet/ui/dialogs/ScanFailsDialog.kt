package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.feedback.ScanFailsEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 28/02/2021.
 */
class ScanFailsDialog {

    companion object {
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.common_warning))
                setMessage(R.string.alert_troubleshooting_scan_card_title)
                setPositiveButton(R.string.alert_button_request_support) { _, _ ->
                    Analytics.send(IntroductionProcess.ButtonRequestSupport())
                    store.dispatch(GlobalAction.SendEmail(ScanFailsEmail()))
                }
                setNeutralButton(R.string.common_cancel) { _, _ -> }
                setOnDismissListener { store.dispatchDialogHide() }
            }.create()
        }
    }

}
