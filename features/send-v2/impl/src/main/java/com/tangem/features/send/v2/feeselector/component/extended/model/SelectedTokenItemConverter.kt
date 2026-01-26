package com.tangem.features.send.v2.feeselector.component.extended.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SelectedTokenItemConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val onTokenClick: () -> Unit,
    private val isTokenSelectionAvailable: Boolean,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        if (value.value !is CryptoCurrencyStatus.Loaded) {
            return TokenItemState.Loading(id = value.currency.id.value)
        }

        val appCurrency = appCurrencyProvider()

        val tokenItemConverter = TokenItemStateConverter(
            appCurrency = appCurrencyProvider(),
            onItemClick = if (isTokenSelectionAvailable) {
                { _, _ -> onTokenClick() }
            } else {
                null
            },
        )

        val state = tokenItemConverter.convert(value)

        return when (state) {
            is TokenItemState.Content -> {
                val balance = value.value.fiatAmount.format {
                    fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                }

                state.copy(
                    fiatAmountState = if (isTokenSelectionAvailable) {
                        TokenItemState.FiatAmountState.Icon(
                            iconRes = R.drawable.ic_select_18_24,
                            tint = IconTint.Informative,
                        )
                    } else {
                        TokenItemState.FiatAmountState.Empty
                    },
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
    }
}