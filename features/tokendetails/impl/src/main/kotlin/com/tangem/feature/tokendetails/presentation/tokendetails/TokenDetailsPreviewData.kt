package com.tangem.feature.tokendetails.presentation.tokendetails

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R

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

    val tokenDetailsState = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
    )
}