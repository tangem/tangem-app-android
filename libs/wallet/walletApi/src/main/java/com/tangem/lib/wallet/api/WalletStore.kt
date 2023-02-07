package com.tangem.lib.wallet.api

interface WalletStore {

    suspend fun createWallet(): WalletManager
}