package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import com.tangem.features.onramp.tokenlist.entity.utils.addHeader

internal class UpdateTokenItemsTransformer(
    private val appCurrency: AppCurrency,
    private val onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    private val statuses: Map<Boolean, List<CryptoCurrencyStatus>>,
    private val isBalanceHidden: Boolean,
    private val unavailableTokensHeaderReference: TextReference,
    private val warning: NotificationUM? = null,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        val availableItems = convertStatuses(
            converter = OnrampTokenItemStateConverterFactory.createAvailableItemConverter(appCurrency, onItemClick),
            statuses = statuses[true].orEmpty(),
        )

        val unavailableItems = convertStatuses(
            converter = OnrampTokenItemStateConverterFactory.createUnavailableItemConverter(appCurrency),
            statuses = statuses[false].orEmpty(),
        )

        return prevState.copy(
            availableItems = availableItems.addHeader(
                textReference = resourceReference(id = R.string.exchange_tokens_available_tokens_header),
            ),
            unavailableItems = unavailableItems.addHeader(textReference = unavailableTokensHeaderReference),
            isBalanceHidden = isBalanceHidden,
            warning = warning,
        )
    }

    private fun convertStatuses(
        converter: TokenItemStateConverter,
        statuses: List<CryptoCurrencyStatus>,
    ): List<TokensListItemUM.Token> {
        return converter.convertList(statuses)
            .map(TokensListItemUM::Token)
    }
}