package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.utils.converter.Converter

class AccountCryptoPortfolioItemStateConverter(
    private val appCurrency: AppCurrency,
    private val account: Account.CryptoPortfolio,
    private val onItemClick: ((Account.CryptoPortfolio) -> Unit)? = null,
    private val onItemLongClick: ((Account.CryptoPortfolio) -> Unit)? = null,
) : Converter<TotalFiatBalance, TokenItemState> {

    override fun convert(value: TotalFiatBalance): TokenItemState {
        return when (value) {
            is TotalFiatBalance.Loaded -> account.mapToContentState(value)
            TotalFiatBalance.Failed -> account.mapToUnreachableState()
            TotalFiatBalance.Loading -> account.mapToLoadingState()
        }
    }

    private fun Account.CryptoPortfolio.mapToContentState(
        fiatBalance: TotalFiatBalance.Loaded,
    ): TokenItemState.Content {
        return TokenItemState.Content(
            id = account.accountId.value,
            iconState = AccountIconItemStateConverter.convert(this),
            titleState = TokenItemState.TitleState.Content(
                text = accountName.toUM().value,
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = pluralReference(
                    R.plurals.common_tokens_count,
                    count = tokensCount,
                    formatArgs = wrappedList(tokensCount),
                ),
                isAvailable = false,
            ),
            fiatAmountState = FiatAmountState.Content(
                text = fiatBalance.amount
                    .format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
                isFlickering = fiatBalance.source == StatusSource.CACHE,
            ),
            subtitle2State = null,
            onItemClick = onItemClick?.let { onItemClick -> { onItemClick(account) } },
            onItemLongClick = onItemLongClick?.let { onItemLongClick -> { onItemLongClick(account) } },
        )
    }

    private fun Account.CryptoPortfolio.mapToLoadingState(): TokenItemState.Loading {
        return TokenItemState.Loading(
            id = account.accountId.value,
            iconState = AccountIconItemStateConverter.convert(account),
            titleState = TokenItemState.TitleState.Content(
                text = accountName.toUM().value,
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = pluralReference(
                    R.plurals.common_tokens_count,
                    count = tokensCount,
                    formatArgs = wrappedList(tokensCount),
                ),
                isAvailable = false,
            ),
        )
    }

    private fun Account.CryptoPortfolio.mapToUnreachableState(): TokenItemState.Unreachable {
        return TokenItemState.Unreachable(
            id = account.accountId.value,
            iconState = AccountIconItemStateConverter.convert(account),
            titleState = TokenItemState.TitleState.Content(
                text = accountName.toUM().value,
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = pluralReference(
                    R.plurals.common_tokens_count,
                    count = tokensCount,
                    formatArgs = wrappedList(tokensCount),
                ),
                isAvailable = false,
            ),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(account) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(account) }
            },
        )
    }
}