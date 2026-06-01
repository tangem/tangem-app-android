package com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.transformer

import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.markets.tokenselector.*
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.model.UserPortfolioUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class UserPortfolioSectionsTransformer(
    private val availableData: AvailableToAddData,
    private val rawCurrencyId: CryptoCurrency.RawID,
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val resolveWalletDeviceIcon: (UserWallet) -> DeviceIconUM,
    private val onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    fun transform(): UserPortfolioUM {
        return UserPortfolioUM(
            content = TokenSelectorContentUM(
                sections = buildSections(entries = generateEntries(availableData)).toImmutableList(),
            ),
            isAddEnabled = availableData.isAvailableToAdd,
        )
    }

    private fun generateEntries(data: AvailableToAddData): List<PortfolioEntry> {
        return data.availableToAddWallets.values.flatMap { wallet ->
            wallet.accounts.filterCryptoPortfolio().flatMap { accountStatus ->
                accountStatus.tokenList.flattenCurrencies()
                    .filter { status -> status.currency.matchesRawId(rawCurrencyId) }
                    .map { status ->
                        PortfolioEntry(
                            wallet = wallet.userWallet,
                            account = accountStatus,
                            currencyStatus = status,
                        )
                    }
            }
        }
    }

    private fun buildSections(entries: List<PortfolioEntry>): List<TokenSelectorSectionUM> {
        val sections = mutableListOf<TokenSelectorSectionUM>()
        val byWallet = entries.groupBy { it.wallet.walletId }
        val shouldShowWalletHeaders = byWallet.size > 1

        for ((_, walletEntries) in byWallet) {
            if (shouldShowWalletHeaders) {
                val wallet = walletEntries.first().wallet
                sections.add(
                    TokenSelectorSectionUM.WalletHeader(
                        walletName = wallet.name,
                        deviceIcon = resolveWalletDeviceIcon(wallet),
                    ),
                )
            }

            val byAccount = walletEntries.groupBy { it.account.account.accountId }
            val shouldShowAccountHeaders = byAccount.size > 1

            for ((_, accountEntries) in byAccount) {
                val singles = accountEntries.map(::entryToSingle).toImmutableList()
                val accountHeader = if (shouldShowAccountHeaders) {
                    val first = accountEntries.first()
                    AccountHeaderData(
                        accountName = first
                            .account
                            .account
                            .accountName
                            .toUM()
                            .value,
                        cryptoPortfolioIcon = first.account.account.icon,
                    )
                } else {
                    null
                }
                sections.add(
                    TokenSelectorSectionUM.TokenGroup(accountHeader = accountHeader, items = singles),
                )
            }
        }
        return sections
    }

    private fun entryToSingle(entry: PortfolioEntry): UserAssetItemUM.Single {
        val currency = entry.currencyStatus.currency
        val value = entry.currencyStatus.value
        return UserAssetItemUM.Single(
            id = "${entry.wallet.walletId.stringValue}_${entry.account.account.accountId.value}_${currency.id.value}",
            icon = TangemIconUM.Currency(currencyIconState = iconConverter.convert(entry.currencyStatus)),
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
            onClick = {
                onTokenSelected(
                    AddToPortfolioManager.Result(
                        wallet = entry.wallet,
                        account = entry.account,
                        addedCurrency = entry.currencyStatus,
                    ),
                )
            },
            networkName = currency.network.name,
        )
    }

    private data class PortfolioEntry(
        val wallet: UserWallet,
        val account: AccountStatus.CryptoPortfolio,
        val currencyStatus: CryptoCurrencyStatus,
    )

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

    private fun CryptoCurrency.matchesRawId(target: CryptoCurrency.RawID): Boolean {
        val rawId = id.rawCurrencyId ?: return false
        return getTokenIdIfL2Network(rawId.value) == target.value
    }
}