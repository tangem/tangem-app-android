package com.tangem.tap.di.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.domain.card.DefaultDerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDataModule {

    @Singleton
    @Provides
    fun providesDerivationsRepository(
        tangemSdkManager: TangemSdkManager,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): DerivationsRepository {
        return DefaultDerivationsRepository(tangemSdkManager, userWalletsStore, dispatchers)
    }
}