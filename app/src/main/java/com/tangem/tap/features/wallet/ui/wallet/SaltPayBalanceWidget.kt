package com.tangem.tap.features.wallet.ui.wallet

import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.animateVisibility
import com.tangem.tap.common.extensions.formatAmountAsSpannedString
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.databinding.LayoutSingleWalletBalanceBinding
import java.math.BigDecimal

data class SaltPayBalanceWidgetData(
    val state: ProgressState? = null,
    val currencySymbol: String? = null,
    val currency: String? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatCurrency: FiatCurrency? = null,
)

class SaltPayBalanceWidget(
    private val binding: LayoutSingleWalletBalanceBinding,
    private val data: SaltPayBalanceWidgetData,
) {
    fun setup(): Unit = with(binding) {
        if (data.state == ProgressState.Loading) {
            veilBalance.veil()
            veilBalanceCrypto.veil()
        } else {
            veilBalance.unVeil()
            veilBalanceCrypto.unVeil()
        }
        tvProcessing.animateVisibility(
            show = data.state == ProgressState.Error,
        )
        veilBalanceCrypto.animateVisibility(
            show = data.state != ProgressState.Error,
        )
        tvBalance.text = data.fiatAmount?.formatAmountAsSpannedString(
            currencySymbol = data.fiatCurrency?.symbol ?: "",
        )
        tvBalanceCrypto.text = data.currency

        tvCurrencyName.text = data.fiatCurrency?.code

        tvCurrencyName.setOnClickListener {
            store.dispatch(WalletAction.AppCurrencyAction.ChooseAppCurrency)
        }
    }
}


