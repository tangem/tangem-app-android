package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.TokenActionButtonConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from loaded [TokenItemState.Content] to ImmutableList<[TokenActionButtonConfig]>
 *
 * @property clickIntents screen click intents
 *
 */
internal class TokenActionsProvider(private val clickIntents: WalletClickIntents) {

    fun provideActions(cryptoCurrencyStatus: CryptoCurrencyStatus): ImmutableList<TokenActionButtonConfig> {
        // TODO: [REDACTED_JIRA]
        return mockTokenActionButtonConfig(cryptoCurrencyStatus).toImmutableList()
    }

    private fun mockTokenActionButtonConfig(cryptoCurrencyStatus: CryptoCurrencyStatus): List<TokenActionButtonConfig> {
        return listOf(
            TokenActionButtonConfig(
                text = "Send",
                iconResId = R.drawable.ic_plus_24,
                onClick = { clickIntents.onMultiCurrencySendClick(cryptoCurrencyStatus) },
            ),
            TokenActionButtonConfig(
                text = "Buy",
                iconResId = R.drawable.ic_plus_24,
                onClick = {},
            ),
            TokenActionButtonConfig(
                text = "Sell",
                iconResId = R.drawable.ic_plus_24,
                onClick = {},
            ),
            TokenActionButtonConfig(
                text = "Swap",
                iconResId = R.drawable.ic_plus_24,
                onClick = {},
            ),
        )
    }
}