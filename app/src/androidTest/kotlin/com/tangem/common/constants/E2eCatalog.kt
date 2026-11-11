package com.tangem.common.constants

// Non-funded placeholder for local compilation and the add-network tests. CI regenerates it from the shared
// qa-tools /api/e2e/test-data catalog before the build (mirrors the iOS E2EConfig).
// IMPORTANT: never send funds to this wallet. The seed is committed to a repo whose history syncs publicly, so
// anything sent here would be swept immediately. The funded wallet the Send E2E test broadcasts from lives only in
// qa-tools and is injected at build time — it is never committed.
object E2eCatalog {

    // Junk placeholder — not a real wallet and NOT funded. The real funded seed is injected from qa-tools on CI
    // (see the header); this value only exists so the catalog compiles.
    const val SEED_PHRASE: String = "test test test test test test test test test test test junk"

    // Placeholder; the real wallet id (matching the qa-tools funded wallet) is injected on CI.
    const val WALLET_ID: String = "46255D252ED3808AF7F3F7797D2F2C0E87D8BB80896D0D0366A6EB6E4898CC4B"

    val network: E2eNetwork = E2eNetwork(
        coinId = "polygon-ecosystem-token",
        searchQuery = "POL",
        tokenName = "POL (ex-MATIC)",
        networkName = "Polygon",
        mainScreenToken = "Polygon",
        manageTokensNetworkLabel = "POLYGON",
        sendAmount = "0.1",
        accountAddresses = listOf(
            "0x3A0078b8407bC5d7C72FAd47F03eb9D315CA252e",
            "0x6c9578665c02Cc3FF16639A7C636f756B085Ce49",
        ),
        token = E2eToken(
            coinId = "usd-coin",
            searchQuery = "USDC",
            tokenName = "USDC",
            mainScreenToken = "USDC",
            manageTokensNetworkLabel = "POLYGON",
            contractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
            sendAmount = "0.01",
        ),
    )

    data class E2eNetwork(
        val coinId: String,
        val searchQuery: String,
        val tokenName: String,
        val networkName: String,
        val mainScreenToken: String,
        val manageTokensNetworkLabel: String,
        // Amount the Send E2E test transfers, in the network's native coin (0.1 POL is tiny; 0.1 ETH would not be).
        val sendAmount: String,
        // The wallet's per-account receive addresses for this network, ordered by account index
        // (sender/recipient for the Send E2E test). Mirrors the iOS E2ENetwork.accountAddresses.
        val accountAddresses: List<String>,
        // The token enabled on this network for the token Send E2E test; null when the network has no token test.
        val token: E2eToken? = null,
    )

    // A token (not the native coin) enabled on the network. Gas is paid in the native coin, so the sender
    // account must also hold a native-coin balance. Mirrors the iOS E2EToken.
    data class E2eToken(
        val coinId: String,
        val searchQuery: String,
        val tokenName: String,
        val mainScreenToken: String,
        val manageTokensNetworkLabel: String,
        val contractAddress: String,
        val sendAmount: String,
    )
}