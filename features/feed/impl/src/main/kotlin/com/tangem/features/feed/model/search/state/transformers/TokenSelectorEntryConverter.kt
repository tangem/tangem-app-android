package com.tangem.features.feed.model.search.state.transformers

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.search.model.UserAssetSearchEntry
import com.tangem.features.feed.ui.search.state.BalanceDisplayState
import com.tangem.features.feed.ui.search.state.UserAssetItemUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class TokenSelectorEntryConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onTokenSelected: (UserAssetSearchEntry) -> Unit,
) : Converter<UserAssetSearchEntry, UserAssetItemUM.Single> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: UserAssetSearchEntry): UserAssetItemUM.Single {
        val currency = value.currencyStatus.currency
        val currencyValue = value.currencyStatus.value

        return UserAssetItemUM.Single(
            id = "${value.userWalletId.stringValue}_${value.accountId.value}_${currency.id.value}",
            icon = TangemIconUM.Currency(
                currencyIconState = iconConverter.convert(value.currencyStatus),
            ),
            tokenName = currency.name,
            tokenSymbol = currency.symbol,
            fiatRate = currencyValue.fiatRate?.format { fiat(appCurrency.code, appCurrency.symbol) },
            priceChangeState = when (currencyValue) {
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.NoAmount,
                -> PriceChangeState.Unknown
                else -> PriceChangeState.Content(
                    type = PriceChangeType.fromBigDecimal(currencyValue.priceChange.orZero()),
                    valueInPercent = currencyValue.priceChange.format { percent() },
                )
            },
            balanceState = convertBalanceState(currencyValue, currency.symbol, currency.decimals),
            isBalanceHidden = isBalanceHidden,
            onClick = { onTokenSelected(value) },
            networkName = value.currencyStatus.currency.network.name,
        )
    }

    private fun convertBalanceState(
        value: CryptoCurrencyStatus.Value,
        symbol: String,
        decimals: Int,
    ): BalanceDisplayState {
        return when {
            value is CryptoCurrencyStatus.Loading && value.amount != null ->
                BalanceDisplayState.Flickering(
                    cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                    fiatBalance = stringReference(
                        value.fiatAmount?.format { fiat(appCurrency.code, appCurrency.symbol) }
                            ?: StringsSigns.DASH_SIGN,
                    ),
                )
            value is CryptoCurrencyStatus.Loading -> BalanceDisplayState.Loading
            value is CryptoCurrencyStatus.Unreachable -> BalanceDisplayState.Unreachable
            value.isError && value.amount != null ->
                BalanceDisplayState.Stale(
                    cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                    fiatBalance = stringReference(
                        value.fiatAmount?.format { fiat(appCurrency.code, appCurrency.symbol) }
                            ?: StringsSigns.DASH_SIGN,
                    ),
                )
            value.isError -> BalanceDisplayState.Unreachable
            else -> BalanceDisplayState.Loaded(
                cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                fiatBalance = stringReference(
                    value.fiatAmount?.format { fiat(appCurrency.code, appCurrency.symbol) }
                        ?: StringsSigns.DASH_SIGN,
                ),
            )
        }
    }

    private fun formatCryptoAmount(amount: BigDecimal?, symbol: String, decimals: Int): String {
        return amount?.format { crypto(symbol, decimals) } ?: StringsSigns.DASH_SIGN
    }
}