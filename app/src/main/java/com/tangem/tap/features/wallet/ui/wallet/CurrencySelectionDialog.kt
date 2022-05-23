package com.tangem.tap.features.wallet.ui.wallet

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R

object CurrencySelectionDialog {
    fun create(
        dialog: AppDialog.CurrencySelectionDialog,
        context: Context
    ): AlertDialog {
        val currenciesToShow = dialog.currenciesList
            .map { it.displayName }
            .toTypedArray()
        var currentSelection = dialog.currenciesList
            .indexOfFirst { it.code == dialog.currentAppCurrency.code }

        return AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.details_row_title_currency))
            .setNegativeButton(context.getString(R.string.common_cancel)) { _, _ -> /* no-op */ }
            .setPositiveButton(context.getString(R.string.common_done)) { _, _ ->
                val selectedCurrency = dialog.currenciesList[currentSelection]
                store.dispatch(
                    WalletAction.AppCurrencyAction.SelectAppCurrency(
                        fiatCurrency = selectedCurrency
                    )
                )
            }
            .setOnDismissListener {
                store.dispatchDialogHide()
            }
            .setSingleChoiceItems(currenciesToShow, currentSelection) { _, which ->
                currentSelection = which
            }
            .create()
    }
}