package com.tangem.common.ui.account

import com.tangem.common.ui.R
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.components.token.state.TokenItemState.Subtitle2State
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.quote.PriceChange
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

class AccountCryptoPortfolioItemStateConverter(
    private val appCurrency: AppCurrency,
    private val account: Account.Crypto,
    private val priceChangeLce: Lce<Unit, PriceChange>? = null,
    private val onItemClick: ((Account.Crypto) -> Unit)? = null,
    private val onItemLongClick: ((Account.Crypto) -> Unit)? = null,
) : Converter<TotalFiatBalance, TokenItemState> {

    override fun convert(value: TotalFiatBalance): TokenItemState {
        return when (value) {
            is TotalFiatBalance.Loaded -> account.mapToContentState(value)
            TotalFiatBalance.Failed -> account.mapToUnreachableState()
            TotalFiatBalance.Loading -> account.mapToLoadingState()
        }
    }

    private fun Account.Crypto.mapToContentState(fiatBalance: TotalFiatBalance.Loaded): TokenItemState.Content {
        val subtitle2State = priceChangeLce?.fold(
            ifLoading = { priceChange -> priceChange?.toSubtitle2State() ?: Subtitle2State.Loading },
            ifError = { null },
            ifContent = { priceChange -> priceChange.toSubtitle2State() },
        )
        return TokenItemState.Content(
            id = account.accountId.toItemId(),
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
            subtitle2State = subtitle2State,
            onItemClick = onItemClick?.let { onItemClick -> { onItemClick(account) } },
            onItemLongClick = onItemLongClick?.let { onItemLongClick -> { onItemLongClick(account) } },
        )
    }

    private fun Account.Crypto.mapToLoadingState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = account.accountId.toItemId(),
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
            fiatAmountState = FiatAmountState.Loading,
            subtitle2State = Subtitle2State.Loading,
            onItemLongClick = null,
            onItemClick = onItemClick?.let { onItemClick -> { onItemClick(account) } },
        )
    }

    private fun Account.Crypto.mapToUnreachableState(): TokenItemState.Unreachable {
        return TokenItemState.Unreachable(
            id = account.accountId.toItemId(),
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

    private fun AccountId.toItemId() = this.value

    private fun BigDecimal.getPriceChangeType(): PriceChangeType = PriceChangeConverter.fromBigDecimal(value = this)

    private fun StatusSource.isFlickering(): Boolean = this == StatusSource.CACHE

    private fun PriceChange.toSubtitle2State(): Subtitle2State = Subtitle2State.PriceChangeContent(
        priceChangePercent = this.value.format { percent() },
        type = this.value.getPriceChangeType(),
        isFlickering = this.source.isFlickering(),
    )
}