package com.tangem.tap.di.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.tap.data.RuntimeUserWalletsStore
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
    fun provideUserWalletsStore(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): UserWalletsStore {
        return if (hotWalletFeatureToggles.isHotWalletEnabled) {
            UserWalletsStoreRepositoryProxy(userWalletsListRepository)
        } else {
            RuntimeUserWalletsStore(userWalletsListManager = userWalletsListManager)
        }
    }
}