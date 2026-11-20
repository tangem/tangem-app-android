package com.tangem.feature.referral.model

import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.isFlickering
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.tokens.TokenRowStateConverter
import com.tangem.common.ui.tokens.toTokenIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.referral.models.ReferralStateHolder.AccountAward
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.converter.Converter

@Suppress("LongParameterList")
internal class AccountAwardConverter(
    private val isBalanceHidden: Boolean,
    private val isSingleAccount: Boolean,
    private val appCurrency: AppCurrency,
    private val awardCryptoCurrency: CryptoCurrency,
    private val accountAwardToken: CryptoCurrencyStatus?,
    private val cryptoPortfolio: AccountStatus.CryptoPortfolio,
    private val onAccountClick: () -> Unit,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    private val isRedesignedTokenRowsEnabled: Boolean = false,
) : Converter<Unit, AccountAward> {

    override fun convert(value: Unit): AccountAward {
        val tokenState = if (accountAwardToken != null) {
            val currency = accountAwardToken.currency
            TokenItemState.Content(
                id = currency.id.value,
                iconState = CryptoCurrencyToIconStateConverter().convert(currency),
                titleState = TokenItemState.TitleState.Content(stringReference(currency.name)),
                fiatAmountState = TokenItemState.FiatAmountState.Content(
                    text = accountAwardToken.getTotalFiatAmount().format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(
                    text = accountAwardToken.getTotalCryptoAmount().format {
                        crypto(cryptoCurrency = currency)
                    },
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
            tokenRow = if (isRedesignedTokenRowsEnabled) tokenRow() else null,
        )
    }

    private fun tokenRow(): TangemTokenRow.State {
        if (accountAwardToken != null) {
            return TokenRowStateConverter(appCurrency).convert(
                currencyStatus = accountAwardToken,
                onClick = null,
                subtitle = stringReference(accountAwardToken.currency.symbol),
            )
        }
        return TangemTokenRow.State.Network(
            id = awardCryptoCurrency.id.value,
            icon = CryptoCurrencyToIconStateConverter().convert(awardCryptoCurrency).toTokenIconState(),
            title = stringReference(awardCryptoCurrency.name),
            network = stringReference(awardCryptoCurrency.symbol),
        )
    }
}