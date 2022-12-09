package com.tangem.tap.domain.walletStores.repository.di

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.tap.domain.walletStores.repository.WalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.WalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.WalletStoresRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletAmountsRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletManagersRepository
import com.tangem.tap.domain.walletStores.repository.implementation.DefaultWalletStoresRepository

fun WalletStoresRepository.Companion.provideDefaultImplementation(): WalletStoresRepository {
    return DefaultWalletStoresRepository()
}

fun WalletManagersRepository.Companion.provideDefaultImplementation(
    walletManagerFactory: WalletManagerFactory,
): WalletManagersRepository {
    return DefaultWalletManagersRepository(walletManagerFactory)
}

fun WalletAmountsRepository.Companion.provideDefaultImplementation(
    tangemTechService: TangemTechService,
): WalletAmountsRepository {
    return DefaultWalletAmountsRepository(tangemTechService)
}
