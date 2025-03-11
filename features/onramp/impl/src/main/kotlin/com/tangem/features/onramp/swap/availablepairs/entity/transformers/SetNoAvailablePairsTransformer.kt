package com.tangem.features.onramp.swap.availablepairs.entity.transformers

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import com.tangem.features.onramp.tokenlist.entity.utils.addHeader
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
internal class SetNoAvailablePairsTransformer(
    private val appCurrency: AppCurrency,
    private val unavailableStatuses: List<CryptoCurrencyStatus>,
    private val isBalanceHidden: Boolean,
    private val unavailableTokensHeaderReference: TextReference,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        val unavailableItems = OnrampTokenItemStateConverterFactory.createUnavailableItemConverter(appCurrency)
            .convertList(unavailableStatuses)
            .map(TokensListItemUM::Token)

        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = unavailableItems.addHeader(textReference = unavailableTokensHeaderReference),
            isBalanceHidden = isBalanceHidden,
            warning = NotificationUM.Warning.SwapNoAvailablePair,
        )
    }
}