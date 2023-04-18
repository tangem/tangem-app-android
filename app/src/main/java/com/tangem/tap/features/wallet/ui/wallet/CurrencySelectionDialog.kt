package com.tangem.tap.features.wallet.ui.wallet

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.R

object CurrencySelectionDialog {
    fun create(dialog: WalletDialog.CurrencySelectionDialog, context: Context): AlertDialog {
        val currenciesToShow = dialog.currenciesList
            .map { it.displayName }
            .toTypedArray()
        val currentSelection = dialog.currenciesList
            .indexOfFirst { it.code == dialog.currentAppCurrency.code }

        return AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.details_row_title_currency))
            .setNegativeButton(context.getString(R.string.common_cancel)) { _, _ -> /* no-op */ }
            .setOnDismissListener {
                store.dispatch(WalletAction.DialogAction.Hide)
            }
            .setSingleChoiceItems(currenciesToShow, currentSelection) { _, which ->
                dialog.currenciesList.getOrNull(which)?.let { selectedCurrency ->
                    store.dispatch(
                        WalletAction.AppCurrencyAction.SelectAppCurrency(
                            fiatCurrency = selectedCurrency,
                        ),
                    )
                    store.dispatch(WalletAction.DialogAction.Hide)
                }
            }
            .create()
    }
}
