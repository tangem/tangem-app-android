package com.tangem.common.ui.markets.tokenselector

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
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Builds the token row shared by every token-selector surface, so the balance-state rules stay in one
 * place: the converters differ only in how they discover holdings, never in how a holding renders.
 */
internal class UserAssetRowFactory(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    fun create(id: String, currencyStatus: CryptoCurrencyStatus, onClick: () -> Unit): UserAssetItemUM.Single {
        val currency = currencyStatus.currency
        val value = currencyStatus.value
        return UserAssetItemUM.Single(
            id = id,
            icon = TangemIconUM.Currency(currencyIconState = iconConverter.convert(currencyStatus)),
            tokenName = currency.name,
            tokenSymbol = currency.symbol,
            fiatRate = value.fiatRate?.format { fiat(appCurrency.code, appCurrency.symbol) },
            priceChangeState = when (value) {
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.NoAmount,
                -> PriceChangeState.Unknown
                else -> PriceChangeState.Content(
                    type = PriceChangeType.fromBigDecimal(value.priceChange.orZero()),
                    valueInPercent = value.priceChange.format { percent() },
                )
            },
            balanceState = convertBalanceState(value, currency.symbol, currency.decimals),
            isBalanceHidden = isBalanceHidden,
            onClick = onClick,
            networkName = currency.network.name,
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
                    fiatBalance = stringReference(formatFiatAmount(value.fiatAmount)),
                )
            value is CryptoCurrencyStatus.Loading -> BalanceDisplayState.Loading
            value is CryptoCurrencyStatus.Unreachable -> BalanceDisplayState.Unreachable
            value.isError && value.amount != null ->
                BalanceDisplayState.Stale(
                    cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                    fiatBalance = stringReference(formatFiatAmount(value.fiatAmount)),
                )
            value.isError -> BalanceDisplayState.Unreachable
            else -> BalanceDisplayState.Loaded(
                cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                fiatBalance = stringReference(formatFiatAmount(value.fiatAmount)),
            )
        }
    }

    private fun formatCryptoAmount(amount: BigDecimal?, symbol: String, decimals: Int): String {
        return amount?.format { crypto(symbol, decimals) } ?: StringsSigns.DASH_SIGN
    }

    private fun formatFiatAmount(amount: BigDecimal?): String {
        return amount?.format { fiat(appCurrency.code, appCurrency.symbol) } ?: StringsSigns.DASH_SIGN
    }
}