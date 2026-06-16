package com.tangem.common.ui.markets.tokenselector

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.persistentListOf

@Suppress("StringLiteralDuplication")
class TokenSelectorContentPreviewProvider :
    CollectionPreviewParameterProvider<TokenSelectorContentUM>(
        listOf(
            tokenSelectorPreviewSimple(),
            tokenSelectorPreviewWithAccountHeaders(),
            tokenSelectorPreviewMultiWallet(),
        ),
    )

private fun tokenSelectorPreviewSimple(): TokenSelectorContentUM {
    return TokenSelectorContentUM(
        sections = persistentListOf(
            TokenSelectorSectionUM.TokenGroup(
                accountHeader = null,
                items = persistentListOf(
                    previewTokenItem(id = "eth", name = "Ethereum", symbol = "ETH"),
                    previewTokenItem(id = "btc", name = "Bitcoin", symbol = "BTC"),
                ),
            ),
        ),
    )
}

private fun tokenSelectorPreviewWithAccountHeaders(): TokenSelectorContentUM {
    val accountIcon = CryptoPortfolioIcon.ofCustomAccount(
        value = CryptoPortfolioIcon.Icon.Wallet,
        color = CryptoPortfolioIcon.Color.CaribbeanBlue,
    )
    return TokenSelectorContentUM(
        sections = persistentListOf(
            TokenSelectorSectionUM.TokenGroup(
                accountHeader = AccountHeaderData(
                    accountName = stringReference(value = "Main account"),
                    cryptoPortfolioIcon = accountIcon,
                ),
                items = persistentListOf(
                    previewTokenItem(id = "eth_main", name = "Ethereum", symbol = "ETH"),
                ),
            ),
            TokenSelectorSectionUM.TokenGroup(
                accountHeader = AccountHeaderData(
                    accountName = stringReference(value = "Trading"),
                    cryptoPortfolioIcon = accountIcon,
                ),
                items = persistentListOf(
                    previewTokenItem(id = "sol_trade", name = "Solana", symbol = "SOL"),
                    previewTokenItem(id = "avax_trade", name = "Avalanche", symbol = "AVAX"),
                ),
            ),
        ),
    )
}

private fun tokenSelectorPreviewMultiWallet(): TokenSelectorContentUM {
    return TokenSelectorContentUM(
        sections = persistentListOf(
            TokenSelectorSectionUM.WalletHeader(
                walletName = "Cold wallet",
                deviceIcon = DeviceIconUM.Stub(cardsCount = 2),
            ),
            TokenSelectorSectionUM.TokenGroup(
                accountHeader = null,
                items = persistentListOf(
                    previewTokenItem(id = "btc_cold", name = "Bitcoin", symbol = "BTC"),
                ),
            ),
            TokenSelectorSectionUM.WalletHeader(
                walletName = "Hot wallet",
                deviceIcon = DeviceIconUM.Mobile,
            ),
            TokenSelectorSectionUM.TokenGroup(
                accountHeader = null,
                items = persistentListOf(
                    previewTokenItem(id = "eth_hot", name = "Ethereum", symbol = "ETH"),
                    previewTokenItem(id = "usdt_hot", name = "Tether", symbol = "USDT"),
                ),
            ),
        ),
    )
}

private fun previewTokenItem(id: String, name: String, symbol: String): UserAssetItemUM.Single {
    val cryptoRef = stringReference(value = "1.234 $symbol")
    val fiatRef = stringReference(value = "$1,234.56")
    return UserAssetItemUM.Single(
        id = id,
        icon = TangemIconUM.Currency(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.ic_ethereumpow_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
        tokenName = name,
        tokenSymbol = symbol,
        fiatRate = "$98,765.43",
        priceChangeState = PriceChangeState.Content(
            valueInPercent = "+2.34%",
            type = PriceChangeType.UP,
        ),
        balanceState = BalanceDisplayState.Loaded(
            cryptoBalance = cryptoRef,
            fiatBalance = fiatRef,
        ),
        isBalanceHidden = false,
        onClick = {},
        networkName = "Ethereum",
    )
}