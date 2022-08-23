package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R

class AddMoreBackupCardsDialog {
        companion object {
            fun create(context: Context): AlertDialog {
                return AlertDialog.Builder(context).apply {
                    setTitle(R.string.alert_title)
                    setMessage(R.string.onboarding_alert_message_not_max_backup_cards_added)
                    setPositiveButton(R.string.common_continue) { _, _ ->
                        store.dispatch(BackupAction.ShowAccessCodeInfoScreen)
                    }
                    setNegativeButton(R.string.onboarding_button_add_more_cards) { _, _ ->

                    }
                    setOnDismissListener {
                        store.dispatch(GlobalAction.HideDialog)
                    }
                }.create()
            }
        }
}