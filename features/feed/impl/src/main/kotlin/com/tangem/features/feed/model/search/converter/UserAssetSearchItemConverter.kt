package com.tangem.features.feed.model.search.converter

import com.tangem.common.ui.markets.toMarketsListItemPriceAnnotated
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.search.model.UserAssetSearchEntry
import com.tangem.domain.search.model.UserAssetSearchItem
import com.tangem.features.feed.ui.search.state.BalanceDisplayState
import com.tangem.features.feed.ui.search.state.UserAssetItemUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class UserAssetSearchItemConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val onSingleClick: (UserAssetSearchEntry) -> Unit,
    private val onGroupedClick: (UserAssetSearchItem.Grouped) -> Unit,
) : Converter<UserAssetSearchItem, UserAssetItemUM> {

    override fun convert(value: UserAssetSearchItem): UserAssetItemUM {
        return when (value) {
            is UserAssetSearchItem.Single -> convertSingle(value.entry)
            is UserAssetSearchItem.Grouped -> convertGrouped(value)
        }
    }

    private fun convertSingle(entry: UserAssetSearchEntry): UserAssetItemUM.Single {
        val currency = entry.currencyStatus.currency
        val value = entry.currencyStatus.value

        return UserAssetItemUM.Single(
            id = "${entry.userWalletId.stringValue}_${entry.accountId.value}_${currency.id.value}",
            icon = TangemIconUM.Currency(
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(entry.currencyStatus),
            ),
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
            balanceState = convertSingleBalanceState(value, currency.symbol, currency.decimals),
            isBalanceHidden = isBalanceHidden,
            onClick = { onSingleClick(entry) },
            networkName = entry.currencyStatus.currency.network.name,
        )
    }

    private fun convertSingleBalanceState(
        value: CryptoCurrencyStatus.Value,
        symbol: String,
        decimals: Int,
    ): BalanceDisplayState {
        return when {
            value is CryptoCurrencyStatus.Loading && value.amount != null ->
                BalanceDisplayState.Flickering(
                    cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                    fiatBalance = value.fiatAmount?.toMarketsListItemPriceAnnotated(
                        appCurrencyCode = appCurrency.code, appCurrencySymbol = appCurrency.symbol,
                    ) ?: stringReference(StringsSigns.DASH_SIGN),
                )
            value is CryptoCurrencyStatus.Loading -> BalanceDisplayState.Loading
            value is CryptoCurrencyStatus.Unreachable -> BalanceDisplayState.Unreachable
            value.isError && value.amount != null ->
                BalanceDisplayState.Stale(
                    cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                    fiatBalance = value.fiatAmount?.toMarketsListItemPriceAnnotated(
                        appCurrencyCode = appCurrency.code, appCurrencySymbol = appCurrency.symbol,
                    ) ?: stringReference(StringsSigns.DASH_SIGN),
                )
            value.isError -> BalanceDisplayState.Unreachable
            else -> BalanceDisplayState.Loaded(
                cryptoBalance = stringReference(formatCryptoAmount(value.amount, symbol, decimals)),
                fiatBalance = value.fiatAmount?.toMarketsListItemPriceAnnotated(
                    appCurrencyCode = appCurrency.code, appCurrencySymbol = appCurrency.symbol,
                ) ?: stringReference(StringsSigns.DASH_SIGN),
            )
        }
    }

    private fun convertGrouped(item: UserAssetSearchItem.Grouped): UserAssetItemUM.Grouped {
        val firstCurrency = item.entries.first().currencyStatus.currency

        val entryCurrencyStatus = item.entries.first().currencyStatus

        return UserAssetItemUM.Grouped(
            id = "grouped_${item.tokenName}_${item.tokenSymbol}",
            icon = TangemIconUM.Currency(
                currencyIconState = CurrencyIconState.CoinIcon(
                    url = entryCurrencyStatus.currency.iconUrl,
                    fallbackResId = entryCurrencyStatus.currency.networkIconResId,
                    isGrayscale = entryCurrencyStatus.currency.network.isTestnet || entryCurrencyStatus.value.isError,
                    shouldShowCustomBadge = entryCurrencyStatus.currency.isCustom,
                ),
            ),
            tokenName = item.tokenName,
            tokenSymbol = item.tokenSymbol,
            tokensCount = item.entries.size,
            balanceState = convertGroupedBalanceState(item.entries, firstCurrency.symbol, firstCurrency.decimals),
            isBalanceHidden = isBalanceHidden,
            onClick = { onGroupedClick(item) },
        )
    }

    private fun convertGroupedBalanceState(
        entries: List<UserAssetSearchEntry>,
        symbol: String,
        decimals: Int,
    ): BalanceDisplayState {
        val hasAnyLoading = entries.any { it.currencyStatus.value is CryptoCurrencyStatus.Loading }
        val hasAnyError = entries.any { it.currencyStatus.value.isError }
        val hasAnyAmount = entries.any { it.currencyStatus.value.amount != null }
        val isAllError = entries.all { it.currencyStatus.value.isError }

        val balance = when {
            hasAnyLoading && !hasAnyAmount -> BalanceDisplayState.Loading
            hasAnyLoading && hasAnyAmount -> computeGroupBalanceFlickering(entries, symbol, decimals)
            isAllError -> BalanceDisplayState.Unreachable
            hasAnyError && entries.size == 1 && !hasAnyAmount -> BalanceDisplayState.Unreachable
            hasAnyError && entries.size > 1 -> computeGroupBalance(entries, symbol, decimals)
            else -> computeGroupBalance(entries, symbol, decimals)
        }
        return balance
    }

    private fun computeGroupBalance(
        entries: List<UserAssetSearchEntry>,
        symbol: String,
        decimals: Int,
    ): BalanceDisplayState.Loaded {
        val totalFiat = entries.sumOf { it.currencyStatus.value.fiatAmount ?: BigDecimal.ZERO }
        val totalCrypto = entries.sumOf { it.currencyStatus.value.amount ?: BigDecimal.ZERO }
        return BalanceDisplayState.Loaded(
            cryptoBalance = stringReference(formatCryptoAmount(totalCrypto, symbol, decimals)),
            fiatBalance = totalFiat.toMarketsListItemPriceAnnotated(appCurrency.code, appCurrency.symbol),
        )
    }

    private fun computeGroupBalanceFlickering(
        entries: List<UserAssetSearchEntry>,
        symbol: String,
        decimals: Int,
    ): BalanceDisplayState.Flickering {
        val totalFiat = entries.sumOf { it.currencyStatus.value.fiatAmount ?: BigDecimal.ZERO }
        val totalCrypto = entries.sumOf { it.currencyStatus.value.amount ?: BigDecimal.ZERO }
        return BalanceDisplayState.Flickering(
            cryptoBalance = stringReference(formatCryptoAmount(totalCrypto, symbol, decimals)),
            fiatBalance = totalFiat.toMarketsListItemPriceAnnotated(appCurrency.code, appCurrency.symbol),
        )
    }

    private fun formatCryptoAmount(amount: BigDecimal?, symbol: String, decimals: Int): String {
        return amount?.format { crypto(symbol, decimals) } ?: StringsSigns.DASH_SIGN
    }
}