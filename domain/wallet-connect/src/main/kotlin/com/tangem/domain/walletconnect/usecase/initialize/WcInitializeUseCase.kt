package com.tangem.domain.walletconnect.usecase.initialize

interface WcInitializeUseCase {

    /**
     * Brings the WalletConnect SDK up unless it is already running, and returns once it is usable.
     *
     * Every entry point that touches the SDK calls this, so the app pays for the SDK only when
     * WalletConnect is actually used. Concurrent calls share a single initialization; a failed one is not
     * remembered, so the next call retries.
     */
    suspend fun ensureInitialized()

    /**
     * Applies the startup policy: with on-demand init enabled the SDK starts only for a user who already
     * has sessions — without it their dApps could not reach the wallet until they opened a WalletConnect
     * screen — and a user with no sessions never pays its ~400 ms. With the toggle off the SDK starts
     * unconditionally, as it did before.
     */
    suspend fun initializeOnAppStart()
}