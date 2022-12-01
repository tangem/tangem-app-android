package com.tangem.tap.domain.walletStores.di

import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.domain.walletStores.implementation.DummyWalletStoresManager

fun WalletStoresManager.Companion.provideDummyImplementation(): WalletStoresManager {
    return DummyWalletStoresManager()
}