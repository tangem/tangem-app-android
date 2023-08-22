package com.tangem.feature.tokendetails.presentation.tokendetails

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal object TokenDetailsPreviewData {

    val tokenDetailsTopAppBarConfig = TokenDetailsTopAppBarConfig(onBackClick = {}, onMoreClick = {})

    val tokenInfoBlockStateWithLongNameInMainCurrency = TokenInfoBlockState(
        name = "Stellar (XLM) with long name test",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        currency = TokenInfoBlockState.Currency.Native,
    )
    val tokenInfoBlockStateWithLongName = TokenInfoBlockState(
        name = "Tether (USDT) with long name test",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        currency = TokenInfoBlockState.Currency.Token(
            networkName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchainName = "Ethereum",
        ),
    )

    val tokenInfoBlockState = TokenInfoBlockState(
        name = "Tether USDT",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
        currency = TokenInfoBlockState.Currency.Token(
            networkName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchainName = "Ethereum",
        ),
    )

    // TODO: https://tangem.atlassian.net/browse/AND-3962
    val actionButtons = persistentListOf(
        ActionButtonConfig(
            text = TextReference.Str(value = "Buy"),
            iconResId = R.drawable.ic_plus_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Send"),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Receive"),
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = TextReference.Str(value = "Exchange"),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = {},
        ),
    )

    // TODO: https://tangem.atlassian.net/browse/AND-3962
    val disabledActionButtons = actionButtons.map { it.copy(enabled = false) }.toPersistentList()

    val balanceLoading = TokenDetailsBalanceBlockState.Loading(actionButtons = disabledActionButtons)
    val balanceContent = TokenDetailsBalanceBlockState.Content(
        actionButtons = actionButtons,
        fiatBalance = "123,00$",
        cryptoBalance = "866,96 USDT",
    )
    val balanceError = TokenDetailsBalanceBlockState.Error(actionButtons = disabledActionButtons)

    private val marketPriceLoading = MarketPriceBlockState.Loading(currencyName = "USDT")

    val tokenDetailsState = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
        tokenBalanceBlockState = balanceLoading,
        marketPriceBlockState = marketPriceLoading,
    )
}
