package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.utils.converter.Converter

internal class PolymarketWalletBlockConverter(
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
) : Converter<AccountStatus.Prediction, PolymarketWalletBlockUM> {

    override fun convert(value: AccountStatus.Prediction): PolymarketWalletBlockUM {
        val statusValue = value.value

        return PolymarketWalletBlockUM.Content(
            balance = statusValue.toBalance(),
            isBalanceFlickering = statusValue.source == StatusSource.CACHE,
            isBalanceFromCache = statusValue.source == StatusSource.ONLY_CACHE,
            onClick = { clickIntents.onPredictionAccountClick(value.account.accountId.userWalletId) },
        )
    }

    private fun PredictionAccountStatusValue.toBalance(): PolymarketWalletBlockUM.Balance = when {
        this is PredictionAccountStatusValue.Loading -> PolymarketWalletBlockUM.Balance.Loading
        else -> fiatBalance
            ?.let { PolymarketWalletBlockUM.Balance.Amount(text = it.formatFiat()) }
            ?: PolymarketWalletBlockUM.Balance.Unknown
    }

    private fun java.math.BigDecimal.formatFiat() = formatStyled {
        fiat(
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
        )
    }
}