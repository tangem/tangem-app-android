package com.tangem.tap.domain.walletCurrencies.di

import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.common.Provider
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletCurrencies.implementation.DefaultWalletCurrenciesManager
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository

@Suppress("LongParameterList")
fun WalletCurrenciesManager.Companion.provideDefaultImplementation(
    userTokensRepository: UserTokensRepository,
    walletStoresRepository: WalletStoresRepository,
    walletAmountsRepository: WalletAmountsRepository,
    walletManagersRepository: WalletManagersRepository,
    appCurrencyProvider: () -> FiatCurrency,
    cardTypeResolverProvider: Provider<CardTypeResolver>,
): WalletCurrenciesManager {
    return DefaultWalletCurrenciesManager(
        userTokensRepository = userTokensRepository,
        walletStoresRepository = walletStoresRepository,
        walletAmountsRepository = walletAmountsRepository,
        walletManagersRepository = walletManagersRepository,
        appCurrencyProvider = appCurrencyProvider,
        cardTypeResolverProvider = cardTypeResolverProvider,
    )
}
