package com.tangem.tap.common.redux

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.tap.features.home.data.HomeRepositoryImpl
import com.tangem.tap.features.home.domain.HomeRepository
import com.tangem.tap.features.wallet.data.WalletRepositoryImpl
import com.tangem.tap.features.wallet.domain.WalletRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class FeatureRepositoryProvider(tangemTechApi: TangemTechApi, dispatchers: CoroutineDispatcherProvider) {

    val walletRepository: WalletRepository = WalletRepositoryImpl(tangemTechApi, dispatchers)

    val homeRepository: HomeRepository = HomeRepositoryImpl(tangemTechApi, dispatchers)
}
