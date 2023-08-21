package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.TokenActionButtonConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from loaded [TokenItemState.Content] to ImmutableList<[TokenActionButtonConfig]>
 *
 * @property currentStateProvider current ui state provider
 *
 */
@Suppress("UnusedPrivateMember")
internal class TokenActionsProvider(
    private val currentStateProvider: Provider<WalletState>,
) {

    @Suppress("UnusedPrivateMember")
    fun provideActions(tokenId: String): ImmutableList<TokenActionButtonConfig> {
        // TODO: [REDACTED_JIRA]
        return mockTokenActionButtonConfig().toImmutableList()
    }

    private fun mockTokenActionButtonConfig(): List<TokenActionButtonConfig> {
        return listOf(
            TokenActionButtonConfig(
                text = "Send",
                iconResId = R.drawable.ic_plus_24,
                onClick = {},
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