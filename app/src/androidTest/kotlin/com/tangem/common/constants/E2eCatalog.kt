package com.tangem.common.constants

// Non-funded placeholder for local compilation and the add-network tests. CI regenerates it from the shared
// qa-tools /api/e2e/test-data catalog before the build (mirrors the iOS E2EConfig).
// IMPORTANT: never send funds to this wallet. The seed is committed to a repo whose history syncs publicly, so
// anything sent here would be swept immediately. The funded wallet the Send E2E test broadcasts from lives only in
// qa-tools and is injected at build time — it is never committed.
object E2eCatalog {

    // Junk placeholder — not a real wallet and NOT funded. The real funded seed is injected from qa-tools on CI
    // (see the header); this value only exists so the catalog compiles.
    const val SEED_PHRASE: String = "get it from qa-tools"

    // Placeholder; the real wallet id (matching the qa-tools funded wallet) is injected on CI.
    const val WALLET_ID: String = "F833DECD4F3547C13D5CEADAE8C05452B7B171B460F807D3B040A7BA054FA337"

    val network: E2eNetwork = E2eNetwork(
        coinId = "katana-ethereum",
        searchQuery = "Katana",
        tokenName = "Katana",
        networkName = "Katana",
        mainScreenToken = "Katana (ETH)",
        manageTokensNetworkLabel = "KATANA",
        sendAmount = "0.0001",
        accountAddresses = listOf(
            "0x88c1f90210F67a5Db3812cD0481bA2A7F5C0A9d5",
            "0x9D4df7e2Caa6AEA0cFb1348797d856d511dFf5D2",
        ),
        token = E2eToken(
            coinId = "katana-network-token",
            searchQuery = "Kat",
            tokenName = "Katana",
            mainScreenToken = "Katana",
            manageTokensNetworkLabel = "KATANA",
            contractAddress = "0x7f1f4b4b29f5058fa32cc7a97141b8d7e5abdc2d",
            sendAmount = "1",
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