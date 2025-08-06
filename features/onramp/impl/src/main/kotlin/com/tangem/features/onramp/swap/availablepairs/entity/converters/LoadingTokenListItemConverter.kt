package com.tangem.features.onramp.swap.availablepairs.entity.converters

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

/**
 * Convert from [CryptoCurrencyStatus] to loading [TokensListItemUM.Token] state
 *
[REDACTED_AUTHOR]
 */
internal object LoadingTokenListItemConverter : Converter<CryptoCurrencyStatus, TokensListItemUM.Token> {

    override fun convert(value: CryptoCurrencyStatus): TokensListItemUM.Token {
        return TokensListItemUM.Token(
            state = TokenItemState.Loading(
                id = value.currency.id.value,
                iconState = CurrencyIconState.Loading,
                titleState = TokenItemState.TitleState.Loading,
                subtitleState = TokenItemState.SubtitleState.Loading,
            ),
        )
    }
}