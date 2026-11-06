package com.tangem.common.ui.tokens

import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.R
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns

class TokenRowStateConverter(private val appCurrency: AppCurrency) {

    private val currencyToIconStateConverter = CryptoCurrencyToIconStateConverter()

    fun convert(
        currencyStatus: CryptoCurrencyStatus,
        onClick: (() -> Unit)?,
        badge: TangemTokenRow.Badge? = null,
        messageBubble: TangemTokenRow.MessageBubble? = null,
    ): TangemTokenRow.State {
        val id = currencyStatus.currency.id.value
        val icon = currencyToIconStateConverter.convert(currencyStatus).toTokenIconState()
        val title = stringReference(currencyStatus.currency.name)

        return when (val value = currencyStatus.value) {
            CryptoCurrencyStatus.Loading -> TangemTokenRow.State.Shimmer(id = id)
            is CryptoCurrencyStatus.Unreachable -> TangemTokenRow.State.Unreachable(
                id = id,
                icon = icon,
                title = title,
                badge = resourceReference(R.string.common_unreachable),
                onClick = onClick,
            )
            is CryptoCurrencyStatus.MissedDerivation -> TangemTokenRow.State.NoAddress(
                id = id,
                icon = icon,
                title = title,
                message = resourceReference(R.string.common_no_address),
            )
            is CryptoCurrencyStatus.NoAmount -> TangemTokenRow.State.Content(
                id = id,
                icon = icon,
                title = title,
                fiatBalance = stringReference(StringsSigns.DASH_SIGN),
                messageBubble = messageBubble,
                onClick = onClick,
            )
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                val isFlickering = value.sources.total == StatusSource.CACHE
                val yieldSupply = value.yieldSupplyStatus
                TangemTokenRow.State.Content(
                    id = id,
                    icon = icon,
                    title = title,
                    badge = badge,
                    hasPending = value.hasCurrentNetworkTransactions,
                    quote = stringReference(
                        value.fiatRate.format {
                            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                        },
                    ),
                    priceChange = value.priceChange?.toPriceChangeState(),
                    fiatBalance = stringReference(
                        currencyStatus.getTotalFiatAmount().format {
                            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                        },
                    ),
                    cryptoBalance = stringReference(
                        currencyStatus.getTotalCryptoAmount().format { crypto(currencyStatus.currency) },
                    ),
                    shouldShowContractWarning = yieldSupply?.isActive == true && !yieldSupply.isAllowedToSpend,
                    shouldShowUpdateWarning = value.sources.total == StatusSource.ONLY_CACHE,
                    isQuoteFlickering = isFlickering,
                    isBalanceFlickering = isFlickering,
                    messageBubble = messageBubble,
                    onClick = onClick,
                )
            }
        }
    }
}