package com.tangem.tap.di.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.data.RuntimeUserWalletsStore
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
    fun provideUserWalletsStore(userWalletsListManager: UserWalletsListManager): UserWalletsStore {
        return RuntimeUserWalletsStore(userWalletsListManager = userWalletsListManager)
    }
}