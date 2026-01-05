package com.tangem.features.send.v2.feeselector.component.extended.entity

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

internal class SelectedTokenItemConverter(
    appCurrency: AppCurrency,
    onTokenClick: () -> Unit,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    private val tokenItemConverter = TokenItemStateConverter(
        appCurrency = appCurrency,
        onItemClick = { _, _ -> onTokenClick() },
    )

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        // TODO
        return tokenItemConverter.convert(value)
    }
}