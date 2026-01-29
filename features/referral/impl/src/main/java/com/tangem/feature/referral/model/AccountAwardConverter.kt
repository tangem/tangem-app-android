package com.tangem.feature.referral.model

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.getFormattedCryptoAmount
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.getFormattedFiatAmount
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.isFlickering
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.referral.models.ReferralStateHolder.AccountAward
import com.tangem.utils.converter.Converter

@Suppress("LongParameterList")
internal class AccountAwardConverter(
    private val isBalanceHidden: Boolean,
    private val isSingleAccount: Boolean,
    private val appCurrency: AppCurrency,
    private val awardCryptoCurrency: CryptoCurrency,
    private val accountAwardToken: CryptoCurrencyStatus?,
    private val cryptoPortfolio: AccountStatus.Crypto.Portfolio,
    private val onAccountClick: () -> Unit,
) : Converter<Unit, AccountAward> {

    override fun convert(value: Unit): AccountAward {
        val tokenState = if (accountAwardToken != null) {
            val currency = accountAwardToken.currency
            TokenItemState.Content(
                id = currency.id.value,
                iconState = CryptoCurrencyToIconStateConverter().convert(currency),
                titleState = TokenItemState.TitleState.Content(stringReference(currency.name)),
                fiatAmountState = TokenItemState.FiatAmountState.Content(
                    text = accountAwardToken.getFormattedFiatAmount(appCurrency = appCurrency),
                ),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(
                    text = accountAwardToken.getFormattedCryptoAmount(),
                    isFlickering = accountAwardToken.value.isFlickering(),
                ),
                subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(currency.symbol)),
                onItemClick = null,
                onItemLongClick = null,
            )
        } else {
            val currency = awardCryptoCurrency
            TokenItemState.Content(
                id = currency.id.value,
                iconState = CryptoCurrencyToIconStateConverter().convert(currency),
                titleState = TokenItemState.TitleState.Content(stringReference(currency.name)),
                fiatAmountState = null,
                subtitle2State = null,
                subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(currency.symbol)),
                onItemClick = null,
                onItemLongClick = null,
            )
        }

        return AccountAward(
            isBalanceHidden = isBalanceHidden,
            tokenState = tokenState,
            accountSelectUM = PortfolioSelectUM(
                icon = CryptoPortfolioIconConverter.convert(cryptoPortfolio.account.icon),
                name = cryptoPortfolio.account.accountName.toUM().value,
                isAccountMode = true,
                onClick = onAccountClick,
                isMultiChoice = !isSingleAccount,
            ),
        )
    }
}