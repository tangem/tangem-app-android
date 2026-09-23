package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

// ktlint reads `fiat` as unused while `java.math.BigDecimal` is imported — it is the DSL call in formatFiat()
@Suppress("ktlint:standard:no-unused-imports")
internal class CollectiblesWalletBlockConverter(
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
) : Converter<AccountStatus.Collectibles, CollectiblesWalletBlockUM> {

    override fun convert(value: AccountStatus.Collectibles): CollectiblesWalletBlockUM {
        return CollectiblesWalletBlockUM.Content(
            balance = value.value.toBalance(),
            onClick = if (value.value is CollectiblesAccountStatusValue.MissedDerivation) {
                null
            } else {
                { clickIntents.onCollectiblesAccountClick(value.account.accountId.userWalletId) }
            },
        )
    }

    private fun CollectiblesAccountStatusValue.toBalance(): CollectiblesWalletBlockUM.Balance = when {
        this is CollectiblesAccountStatusValue.Loading -> CollectiblesWalletBlockUM.Balance.Loading
        else -> fiatBalance
            ?.let { CollectiblesWalletBlockUM.Balance.Amount(text = it.formatFiat()) }
            ?: CollectiblesWalletBlockUM.Balance.Unknown
    }

    private fun BigDecimal.formatFiat() = formatStyled {
        fiat(
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
        )
    }
}