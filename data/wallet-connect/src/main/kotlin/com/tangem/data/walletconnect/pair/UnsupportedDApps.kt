package com.tangem.data.walletconnect.pair

/**
 * These dApps are not supported by WC due to various issues with signing formats
 * @see <a href = "https://www.notion.so/tangem/WalletConnect-1935d34eb678805fb9aff2e046dc1dfa?source=copy_link#1a15d34eb6788073925ce9c2b9fd12a5">Documentation<a/>
 */
object UnsupportedDApps {
    val list = listOf(
        "dydx.trade",
        "dydx.exchange",
        "pro.apex.exchange",
        "services.dfx.swiss",
        "sandbox.game",
        "app.paradex.trade",
    )
}