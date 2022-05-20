package com.tangem.tap.features.details.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.store
import com.tangem.wallet.R

class CurrencySelectionDialog {

    var dialog: AlertDialog? = null

    fun show(
        currenciesList: List<FiatCurrency>,
        currentAppCurrency: FiatCurrency,
        context: Context
    ) {
        if (dialog == null) {
            val currenciesToShow = currenciesList
                .map { it.displayName }
                .toTypedArray()
            var currentSelection = currenciesList
                .indexOfFirst { it.code == currentAppCurrency.code }

            dialog = AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.details_row_title_currency))
                .setNegativeButton(context.getString(R.string.common_cancel)) { _, _ ->
                    store.dispatch(DetailsAction.AppCurrencyAction.Cancel)
                }
                .setPositiveButton(context.getString(R.string.common_done)) { _, _ ->
                    val selectedCurrency = currenciesList[currentSelection]
                    store.dispatch(
                        DetailsAction.AppCurrencyAction.SelectAppCurrency(
                            fiatCurrency = selectedCurrency
                        )
                    )
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