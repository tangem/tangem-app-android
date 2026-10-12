package com.tangem.domain.walletconnect.usecase.initialize

interface WcInitializeUseCase {

    /**
     * Applies the startup policy: with on-demand init enabled the SDK starts only for a user who has live
     * work — a saved session or an approval awaiting settlement — because otherwise their dApps could not
     * reach the wallet until they opened a WalletConnect screen. With the toggle off the SDK starts
     * unconditionally and synchronously, as it did before.
     */
    operator fun invoke()
}