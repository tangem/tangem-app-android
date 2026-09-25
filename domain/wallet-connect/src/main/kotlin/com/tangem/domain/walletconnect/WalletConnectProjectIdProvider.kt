package com.tangem.domain.walletconnect

/** Supplies the project id the WalletConnect relay is addressed with; the app reads it from its environment. */
fun interface WalletConnectProjectIdProvider {

    fun provideProjectId(): String
}