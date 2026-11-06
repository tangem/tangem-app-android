package com.tangem.features.send.feeselector.component.token.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.common.ui.tokens.TokenRowStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.feeselector.component.token.entity.FeeTokenItemState
import com.tangem.features.send.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

internal class FeeTokenForListConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val canPayFee: Boolean,
    private val onTokenClick: () -> Unit,
    private val isSponsoredFee: Boolean = false,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    private val isRedesignedTokenRowsEnabled: Boolean = false,
) : Converter<CryptoCurrencyStatus, FeeTokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): FeeTokenItemState {
        // The sponsor pays, so the user's balance in the coin, even an empty or unopened account, does not matter
        val isShown = value.value is CryptoCurrencyStatus.Loaded ||
            isSponsoredFee && value.value !is CryptoCurrencyStatus.Loading
        if (!isShown) {
            return FeeTokenItemState(
                state = TokenItemState.Loading(id = value.currency.id.value).withSponsoredFee(value),
                tokenRow = if (isRedesignedTokenRowsEnabled) {
                    TangemTokenRow.State.Shimmer(id = value.currency.id.value).withSponsoredFee(value)
                } else {
                    null
                },
            )
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
            tokenRow = if (isRedesignedTokenRowsEnabled) {
                tokenRow(value = value, appCurrency = appCurrency, isSelectable = isSelectable)
            } else {
                null
            },
        )
    }

    // TODO [REDACTED_TASK_KEY] Show "Balance: …" as the subtitle once TangemTokenRow supports a text subtitle
    private fun tokenRow(value: CryptoCurrencyStatus, appCurrency: AppCurrency, isSelectable: Boolean) =
        TokenRowStateConverter(appCurrency)
            .convert(currencyStatus = value, onClick = onTokenClick.takeIf { isSelectable })
            .withSponsoredFee(value)

    private fun TangemTokenRow.State.withSponsoredFee(value: CryptoCurrencyStatus): TangemTokenRow.State =
        if (isSponsoredFee) asSponsoredFee(value.currency.symbol) else this

    private fun TokenItemState.withSponsoredFee(value: CryptoCurrencyStatus): TokenItemState =
        if (isSponsoredFee) asSponsoredFee(value.currency.symbol) else this
}