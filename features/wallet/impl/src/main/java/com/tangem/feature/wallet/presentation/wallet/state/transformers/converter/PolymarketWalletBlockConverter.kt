package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.PredictionPositionsValue
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.utils.converter.Converter

internal class PolymarketWalletBlockConverter(
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
) : Converter<AccountStatus.Prediction, PolymarketWalletBlockUM> {

    override fun convert(value: AccountStatus.Prediction): PolymarketWalletBlockUM {
        val statusValue = value.value
        val totalFiatBalance = statusValue.totalFiatBalance
        val source = (totalFiatBalance as? TotalFiatBalance.Loaded)?.source ?: statusValue.source

        return PolymarketWalletBlockUM.Content(
            balance = statusValue.toBalance(totalFiatBalance),
            isBalanceFlickering = source == StatusSource.CACHE,
            isBalanceFromCache = source == StatusSource.ONLY_CACHE,
            onClick = { clickIntents.onPredictionAccountClick(value.account.accountId.userWalletId) },
        )
    }

    private fun PredictionAccountStatusValue.toBalance(
        totalFiatBalance: TotalFiatBalance,
    ): PolymarketWalletBlockUM.Balance = when {
        this is PredictionAccountStatusValue.Loading -> PolymarketWalletBlockUM.Balance.Loading
        this is PredictionAccountStatusValue.Active &&
            positions !is PredictionPositionsValue.Unavailable &&
            totalFiatBalance is TotalFiatBalance.Loaded ->
            PolymarketWalletBlockUM.Balance.Amount(text = totalFiatBalance.amount.formatFiat())
        else -> PolymarketWalletBlockUM.Balance.Unknown
    }

    private fun java.math.BigDecimal.formatFiat() = formatStyled {
        fiat(
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.secondary) },
        )
    }
}