package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

// TODO reuse
internal class TokenItemStateConverter(
    private val appCurrency: AppCurrency,
    private val onItemClick: (CryptoCurrencyStatus) -> Unit,
    private val onItemLongClick: (CryptoCurrencyStatus) -> Unit,
) : Converter<Pair<UserWallet, CryptoCurrencyStatus>, TokenItemState> {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    override fun convert(value: Pair<UserWallet, CryptoCurrencyStatus>): TokenItemState {
        val (userWallet, currencyStatus) = value

        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loading -> currencyStatus.mapToLoadingState(userWallet)
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> currencyStatus.mapToTokenItemState(userWallet)
            is CryptoCurrencyStatus.MissedDerivation -> currencyStatus.mapToNoAddressTokenItemState()
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> currencyStatus.mapToUnreachableTokenItemState(userWallet)
        }
    }

    private fun CryptoCurrencyStatus.mapToLoadingState(userWallet: UserWallet): TokenItemState.Loading {
        return TokenItemState.Loading(
            id = currency.id.value,
            iconState = iconStateConverter.convert(value = this),
            titleState = TokenItemState.TitleState.Content(text = userWallet.name),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = currency.name),
        )
    }

    private fun CryptoCurrencyStatus.mapToTokenItemState(userWallet: UserWallet): TokenItemState.Content {
        return TokenItemState.Content(
            id = currency.id.value,
            iconState = iconStateConverter.convert(value = this),
            titleState = TokenItemState.TitleState.Content(text = userWallet.name),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = currency.name),
            fiatAmountState = TokenItemState.FiatAmountState.Content(
                text = getFormattedFiatAmount(),
                hasStaked = getStakedBalance().compareTo(BigDecimal.ZERO) != 0,
            ),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = getFormattedAmount()),
            onItemClick = { onItemClick(this) },
            onItemLongClick = { onItemLongClick(this) },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = value.amount?.plus(getStakedBalance()) ?: return DASH_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, currency.symbol, currency.decimals)
    }

    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val fiatYieldBalance = value.fiatRate?.times(getStakedBalance()).orZero()
        val fiatAmount = value.fiatAmount?.plus(fiatYieldBalance) ?: return DASH_SIGN

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }

    private fun CryptoCurrencyStatus.getStakedBalance() =
        (value.yieldBalance as? YieldBalance.Data)?.getTotalStakingBalance().orZero()

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState(
        userWallet: UserWallet,
    ): TokenItemState.Unreachable {
        return TokenItemState.Unreachable(
            id = currency.id.value,
            iconState = iconStateConverter.convert(value = this),
            titleState = TokenItemState.TitleState.Content(text = userWallet.name),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = currency.name),
            onItemClick = { onItemClick(this) },
            onItemLongClick = { onItemLongClick(this) },
        )
    }

    private fun CryptoCurrencyStatus.mapToNoAddressTokenItemState() = TokenItemState.NoAddress(
        id = currency.id.value,
        iconState = iconStateConverter.convert(this),
        titleState = TokenItemState.TitleState.Content(text = currency.name),
        onItemLongClick = { onItemLongClick(this) },
    )
}