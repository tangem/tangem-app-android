package com.tangem.managetokens.presentation.managetokens.state.factory

import androidx.compose.runtime.mutableStateOf
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.getTintForTokenIcon
import com.tangem.core.ui.extensions.tryGetBackgroundForTokenIcon
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Token
import com.tangem.managetokens.presentation.common.utils.CurrencyUtils
import com.tangem.managetokens.presentation.managetokens.state.QuotesState
import com.tangem.managetokens.presentation.managetokens.state.TokenButtonType
import com.tangem.managetokens.presentation.managetokens.state.TokenIconState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenConverter(
    private val quotesStateConverter: QuotesToQuotesStateConverter,
    private val networksToChooseNetworkStateConverter: NetworksToChooseNetworkStateConverter,
    private val allAddedCurrencies: Provider<List<CryptoCurrency>>,
    private val selectedAppCurrency: Provider<AppCurrency>,
    private val onTokenItemButtonClick: (TokenItemState.Loaded) -> Unit,
) : Converter<Token, TokenItemState.Loaded> {

    override fun convert(value: Token): TokenItemState.Loaded {
        val isAnyAdded = value.networks.any { network ->
            CurrencyUtils.isAdded(
                address = network.address,
                networkId = network.networkId,
                currencies = allAddedCurrencies(),
            )
        }
        val buttonType = when {
            value.isAvailable && isAnyAdded -> TokenButtonType.EDIT
            value.isAvailable && !isAnyAdded -> TokenButtonType.ADD
            else -> TokenButtonType.NOT_AVAILABLE
        }

        val background = tryGetBackgroundForTokenIcon(value.networks.firstOrNull()?.address ?: "")
        val tint = getTintForTokenIcon(background)

        return TokenItemState.Loaded(
            id = value.id,
            name = value.name,
            currencySymbol = value.symbol,
            tokenId = value.id,
            tokenIcon = TokenIconState(
                ImageReference.Url(value.iconUrl),
                placeholderBackground = background,
                placeholderTint = tint,
            ),
            quotes = value.quote?.let { quotesStateConverter.convert(it) } ?: QuotesState.Unknown,
            rate = value.quote?.fiatRate?.let { rate ->
                BigDecimalFormatter.formatFiatAmount(
                    fiatAmount = rate,
                    fiatCurrencyCode = selectedAppCurrency().code,
                    fiatCurrencySymbol = selectedAppCurrency().symbol,
                )
            },
            availableAction = mutableStateOf(buttonType),
            chooseNetworkState = networksToChooseNetworkStateConverter.convert(value.networks),
            onButtonClick = onTokenItemButtonClick,
        )
    }
}