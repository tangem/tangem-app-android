package com.tangem.data.earn.di

import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.earn.DefaultEarnErrorResolver
import com.tangem.data.earn.repository.DefaultEarnRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.data.earn.datastore.EarnNetworksStore
import com.tangem.data.earn.datastore.EarnTopTokensStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EarnDataModule {

    @Provides
    fun provideEarnErrorResolver(): EarnErrorResolver = DefaultEarnErrorResolver()

    @Provides
    @Singleton
    fun provideEarnRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
        userWalletsListRepository: UserWalletsListRepository,
        cryptoCurrencyFactory: CryptoCurrencyFactory,
        earnNetworksStore: EarnNetworksStore,
        earnTopTokensStore: EarnTopTokensStore,
        earnErrorResolver: EarnErrorResolver,
    ): EarnRepository {
        return DefaultEarnRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
            userWalletsListRepository = userWalletsListRepository,
            cryptoCurrencyFactory = cryptoCurrencyFactory,
            earnNetworksStore = earnNetworksStore,
            earnTopTokensStore = earnTopTokensStore,
            earnErrorResolver = earnErrorResolver,
        )
    }
}