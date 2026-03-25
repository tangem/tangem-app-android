package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.utils.extensions.isZero
import com.tangem.utils.transformer.Transformer

internal class MultiWalletBalanceUMTransformer(
    private val fiatBalance: TotalFiatBalance,
    private val appCurrency: AppCurrency,
) : Transformer<WalletBalanceUM> {

    override fun transform(prevState: WalletBalanceUM): WalletBalanceUM {
        return when (fiatBalance) {
            is TotalFiatBalance.Loading -> prevState.toLoadingState()
            is TotalFiatBalance.Failed -> prevState.toErrorState()
            is TotalFiatBalance.Loaded -> prevState.toWalletCardState(fiatBalance)
        }
    }

    private fun WalletBalanceUM.toLoadingState(): WalletBalanceUM {
        return WalletBalanceUM.Loading(
            id = id,
            name = name,
            deviceIcon = deviceIcon,
        )
    }

    private fun WalletBalanceUM.toErrorState(): WalletBalanceUM {
        return WalletBalanceUM.Error(
            id = id,
            name = name,
            deviceIcon = deviceIcon,
        )
    }

    private fun WalletBalanceUM.toWalletCardState(fiatBalance: TotalFiatBalance.Loaded): WalletBalanceUM {
        return WalletBalanceUM.Content(
            id = id,
            name = name,
            deviceIcon = deviceIcon,
            balanceInAppBar = fiatBalance.amount.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                )
            },
            balance = fiatBalance.amount.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = {
                        TangemTheme.typography2.headingRegular28.toSpanStyle()
                    },
                )
            },
            isZeroBalance = fiatBalance.amount.isZero(),
            isBalanceFlickering = fiatBalance.source == StatusSource.CACHE,
        )
    }
}