package com.tangem.features.send.feeselector.component.token.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.feeselector.component.token.entity.FeeTokenItemState
import com.tangem.features.send.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

internal class FeeTokenForListConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val canPayFee: Boolean,
    private val onTokenClick: () -> Unit,
    private val isSponsoredFee: Boolean = false,
) : Converter<CryptoCurrencyStatus, FeeTokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): FeeTokenItemState {
        // The sponsor pays, so the user's balance in the coin, even an empty or unopened account, does not matter
        val isShown = value.value is CryptoCurrencyStatus.Loaded ||
            isSponsoredFee && value.value !is CryptoCurrencyStatus.Loading
        if (!isShown) {
            return FeeTokenItemState(TokenItemState.Loading(id = value.currency.id.value).withSponsoredFee(value))
        }

        val appCurrency = appCurrencyProvider()
        val isBalanceZero = value.value.amount?.isZero() == true
        val isSelectable = isSponsoredFee || isBalanceZero.not() && canPayFee
        val tokenItemConverter = TokenItemStateConverter(
            appCurrency = appCurrencyProvider(),
            onItemClick = if (isSelectable) {
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
            state = updatedState.withSponsoredFee(value),
            isAvailable = isSelectable,
        )
    }

    private fun TokenItemState.withSponsoredFee(value: CryptoCurrencyStatus): TokenItemState =
        if (isSponsoredFee) asSponsoredFee(value.currency.symbol) else this
}