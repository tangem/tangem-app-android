package com.tangem.features.send.v2.feeselector.component.token.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.feeselector.component.token.entity.FeeTokenItemState
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

internal class FeeTokenForListConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val onTokenClick: () -> Unit,
) : Converter<CryptoCurrencyStatus, FeeTokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): FeeTokenItemState {
        if (value.value !is CryptoCurrencyStatus.Loaded) {
            return FeeTokenItemState(TokenItemState.Loading(id = value.currency.id.value))
        }

        val appCurrency = appCurrencyProvider()
        val isBalanceZero = value.value.amount?.isZero() == true
        val tokenItemConverter = TokenItemStateConverter(
            appCurrency = appCurrencyProvider(),
            onItemClick = if (isBalanceZero.not()) {
                { _, _ -> onTokenClick() }
            } else {
                null
            },
        )

        val state = tokenItemConverter.convert(value)

        val updatedState = when (state) {
            is TokenItemState.Content -> {
                val balance = value.value.fiatAmount.format {
                    fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                }

                state.copy(
                    fiatAmountState = TokenItemState.FiatAmountState.Empty,
                    subtitleState = TokenItemState.SubtitleState.TextContent(
                        value = resourceReference(
                            R.string.common_balance,
                            wrappedList(balance),
                        ),
                        isAvailable = false,
                    ),
                    subtitle2State = null,
                )
            }
            else -> state
        }

        return FeeTokenItemState(
            state = updatedState,
            isAvailable = isBalanceZero.not(),
        )
    }
}