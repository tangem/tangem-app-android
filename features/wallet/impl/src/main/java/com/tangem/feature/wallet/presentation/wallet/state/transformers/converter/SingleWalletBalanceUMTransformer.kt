package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.utils.extensions.isZero
import com.tangem.utils.transformer.Transformer

internal class SingleWalletBalanceUMTransformer(
    private val status: CryptoCurrencyStatus.Value,
    private val appCurrency: AppCurrency,
) : Transformer<WalletBalanceUM> {

    override fun transform(prevState: WalletBalanceUM): WalletBalanceUM {
        return when (status) {
            is CryptoCurrencyStatus.Loading -> prevState.toLoadingState()
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            -> prevState.toErrorState()
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            -> prevState.toContentState(status)
        }
    }

    private fun WalletBalanceUM.toLoadingState(): WalletBalanceUM {
        return WalletBalanceUM.Loading(
            id = id,
            title = title,
        )
    }

    private fun WalletBalanceUM.toErrorState(): WalletBalanceUM {
        return WalletBalanceUM.Error(
            id = id,
            title = title,
        )
    }

    private fun WalletBalanceUM.toContentState(status: CryptoCurrencyStatus.Value): WalletBalanceUM {
        return WalletBalanceUM.Content(
            id = id,
            title = title,
            balance = status.fiatAmount.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = {
                        TangemTheme.typography2.headingRegular28.toSpanStyle()
                    },
                )
            },
            isZeroBalance = status.fiatAmount?.isZero(),
            isBalanceFlickering = (status as? CryptoCurrencyStatus.Loaded)?.sources?.total == StatusSource.CACHE,
        )
    }
}