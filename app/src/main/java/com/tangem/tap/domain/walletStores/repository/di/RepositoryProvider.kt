package com.tangem.tap.domain.walletStores.repository.di

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.common.Provider
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletStoresRepository
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider

fun WalletStoresRepository.Companion.provideDefaultImplementation(): WalletStoresRepository {
    return DefaultWalletStoresRepository()
}

fun WalletManagersRepository.Companion.provideDefaultImplementation(
    walletManagerFactory: WalletManagerFactory,
    cardTypeResolverProvider: Provider<CardTypeResolver>,
): WalletManagersRepository {
    return DefaultWalletManagersRepository(walletManagerFactory, cardTypeResolverProvider)
}

fun WalletAmountsRepository.Companion.provideDefaultImplementation(
    tangemTechService: TangemTechService,
): WalletAmountsRepository {
// [REDACTED_TODO_COMMENT]
    return DefaultWalletAmountsRepository(
        tangemTechApi = tangemTechService.api,
        dispatchers = AppCoroutineDispatcherProvider(),
    )
}
