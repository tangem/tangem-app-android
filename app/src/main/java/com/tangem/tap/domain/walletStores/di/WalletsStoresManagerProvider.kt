package com.tangem.tap.domain.walletStores.di

import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.domain.walletStores.implementation.DummyWalletStoresManager
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletStores.implementation.DefaultWalletStoresManager
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository

fun WalletStoresManager.Companion.provideDummyImplementation(): WalletStoresManager {
    return DummyWalletStoresManager()
}

fun WalletStoresManager.Companion.provideDefaultImplementation(
    userTokensRepository: UserTokensRepository,
    walletStoresRepository: WalletStoresRepository,
    walletAmountsRepository: WalletAmountsRepository,
    walletManagersRepository: WalletManagersRepository,
    appCurrencyProvider: () -> FiatCurrency,
): WalletStoresManager {
    return DefaultWalletStoresManager(
        userTokensRepository = userTokensRepository,
        walletStoresRepository = walletStoresRepository,
        walletAmountsRepository = walletAmountsRepository,
        walletManagersRepository = walletManagersRepository,
        appCurrencyProvider = appCurrencyProvider,
    )
}
