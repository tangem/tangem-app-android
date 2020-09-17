package com.tangem.tap.features.details.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import com.tangem.tap.store
import com.tangem.wallet.R

class CurrencySelectionDialog {

    var dialog: AlertDialog? = null

    fun show(currencies: List<FiatCurrency>, currentAppCurrency: FiatCurrencyName, context: Context) {

        if (dialog == null) {
            val currenciesToShow = currencies.map { it.toFormattedString() }.toTypedArray()
            var currentSelection = currencies.indexOfFirst { it.symbol == currentAppCurrency }

            dialog = MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.details_currency))
                    .setNeutralButton(context.getString(R.string.generic_cancel)) { _, _ ->
                        store.dispatch(DetailsAction.AppCurrencyAction.Cancel)
                    }
                    .setPositiveButton(context.getString(R.string.generic_done)) { _, _ ->
                        val selectedCurrency = currencies[currentSelection]
                        store.dispatch(DetailsAction.AppCurrencyAction.SelectAppCurrency(selectedCurrency.symbol))
                    }
                    .setOnDismissListener {
                        store.dispatch(DetailsAction.AppCurrencyAction.Cancel)
                    }
                    .setSingleChoiceItems(currenciesToShow, currentSelection) { _, which ->
                        currentSelection = which
                    }.show()
        }
    }

    fun clear() {
        dialog = null
    }

}