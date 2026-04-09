package com.tangem.features.feed.model.search.converter

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.search.model.UserAssetSearchEntry
import com.tangem.domain.search.model.UserAssetSearchItem
import com.tangem.features.feed.ui.search.state.UserAssetItemUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class UserAssetSearchItemConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
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
            cryptoBalance = formatCryptoAmount(value.amount, currency.symbol, currency.decimals),
            fiatBalance = formatFiatAmount(value.fiatAmount),
            isBalanceHidden = isBalanceHidden,
            onClick = {},
        )
    }

    private fun convertGrouped(item: UserAssetSearchItem.Grouped): UserAssetItemUM.Grouped {
        val totalFiat = item.entries.sumOf { it.currencyStatus.value.fiatAmount ?: BigDecimal.ZERO }
        val totalCrypto = item.entries.sumOf { it.currencyStatus.value.amount ?: BigDecimal.ZERO }

        val firstCurrency = item.entries.first().currencyStatus.currency
        val children = item.entries.map { entry ->
            UserAssetItemUM.GroupedChild(
                walletName = entry.userWalletName,
                accountName = entry.accountName.toUM(),
                accountIcon = entry.accountIcon.value,
                accountColor = entry.accountIcon.color,
                cryptoBalance = formatCryptoAmount(
                    entry.currencyStatus.value.amount,
                    entry.currencyStatus.currency.symbol,
                    entry.currencyStatus.currency.decimals,
                ),
                fiatBalance = formatFiatAmount(entry.currencyStatus.value.fiatAmount),
            )
        }.toImmutableList()

        return UserAssetItemUM.Grouped(
            id = "grouped_${item.tokenName}_${item.tokenSymbol}",
            icon = TangemIconUM.Currency(
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(item.entries.first().currencyStatus),
            ),
            tokenName = item.tokenName,
            tokenSymbol = item.tokenSymbol,
            tokensCount = item.entries.size,
            totalCryptoBalance = formatCryptoAmount(totalCrypto, firstCurrency.symbol, firstCurrency.decimals),
            totalFiatBalance = formatFiatAmount(totalFiat),
            isBalanceHidden = isBalanceHidden,
            children = children,
            onClick = {},
        )
    }

    private fun formatCryptoAmount(amount: BigDecimal?, symbol: String, decimals: Int): String {
        return amount?.format { crypto(symbol, decimals) } ?: StringsSigns.DASH_SIGN
    }

    private fun formatFiatAmount(fiatAmount: BigDecimal?): String {
        return fiatAmount?.format { fiat(appCurrency.code, appCurrency.symbol) } ?: StringsSigns.DASH_SIGN
    }
}