package com.tangem.feature.tokendetails.presentation.tokendetails

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R

internal object TokenDetailsPreviewData {

    val tokenDetailsTopAppBarConfig = TokenDetailsTopAppBarConfig(onBackClick = {}, onMoreClick = {})

    val tokenInfoBlockStateWithLongNameInMainNetwork = TokenInfoBlockState(
        name = "Stellar (XLM) with long long long long name test",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        network = TokenInfoBlockState.Network.MainNetwork,
    )
    val tokenInfoBlockStateWithLongName = TokenInfoBlockState(
        name = "Tether (USDT) with long long long long name test",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        network = TokenInfoBlockState.Network.TokenNetwork(
            network = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchain = "Ethereum",
        ),
    )

    val tokenInfoBlockState = TokenInfoBlockState(
        name = "Tether USDT",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
        network = TokenInfoBlockState.Network.TokenNetwork(
            network = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchain = "Ethereum",
        ),
    )

    val tokenDetailsState = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
    )
}
