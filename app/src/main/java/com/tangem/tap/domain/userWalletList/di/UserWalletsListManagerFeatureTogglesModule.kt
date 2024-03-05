package com.tangem.tap.domain.userWalletList.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.domain.wallets.legacy.UserWalletsListManagerFeatureToggles
import com.tangem.tap.domain.userWalletList.DefaultUserWalletsListManagerFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserWalletsListManagerFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideUserWalletsListManagerFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): UserWalletsListManagerFeatureToggles {
        return DefaultUserWalletsListManagerFeatureToggles(featureTogglesManager)
    }
}