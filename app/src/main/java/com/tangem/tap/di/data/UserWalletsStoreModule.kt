package com.tangem.tap.di.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.tap.data.UserWalletsStoreRepositoryProxy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserWalletsStoreModule {

    @Provides
    @Singleton
    fun provideUserWalletsStore(userWalletsListRepository: UserWalletsListRepository): UserWalletsStore {
        return UserWalletsStoreRepositoryProxy(userWalletsListRepository)
    }
}