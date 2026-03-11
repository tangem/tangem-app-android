package com.tangem.tap.di.domain

import com.tangem.domain.hotwallet.CheckHotWalletUpgradeBannerUseCase
import com.tangem.domain.hotwallet.CloseHotWalletUpgradeBannerUseCase
import com.tangem.domain.hotwallet.GetAccessCodeSkippedUseCase
import com.tangem.domain.hotwallet.GetUpgradeBannerClosureTimestampUseCase
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.hotwallet.IsAccessCodeSimpleUseCase
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.hotwallet.ShouldShowUpgradeHotWalletBannerUseCase
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HotWalletDomainModule {

    @Provides
    @Singleton
    fun provideGetAccessCodeSkippedUseCase(hotWalletRepository: HotWalletRepository): GetAccessCodeSkippedUseCase {
        return GetAccessCodeSkippedUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideSetAccessCodeSkippedUseCase(hotWalletRepository: HotWalletRepository): SetAccessCodeSkippedUseCase {
        return SetAccessCodeSkippedUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideIsAccessCodeSimpleUseCase(): IsAccessCodeSimpleUseCase {
        return IsAccessCodeSimpleUseCase()
    }

    @Provides
    @Singleton
    fun provideIsWalletCreationSupportedUseCase(
        hotWalletRepository: HotWalletRepository,
    ): IsHotWalletCreationSupported {
        return IsHotWalletCreationSupported(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideCheckHotWalletUpgradeBannerUseCase(
        hotWalletRepository: HotWalletRepository,
    ): CheckHotWalletUpgradeBannerUseCase {
        return CheckHotWalletUpgradeBannerUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideCloseHotWalletUpgradeBannerUseCase(
        hotWalletRepository: HotWalletRepository,
    ): CloseHotWalletUpgradeBannerUseCase {
        return CloseHotWalletUpgradeBannerUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowUpgradeHotWalletBannerUseCase(
        hotWalletRepository: HotWalletRepository,
    ): ShouldShowUpgradeHotWalletBannerUseCase {
        return ShouldShowUpgradeHotWalletBannerUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideGetUpgradeBannerClosureTimestampUseCase(
        hotWalletRepository: HotWalletRepository,
    ): GetUpgradeBannerClosureTimestampUseCase {
        return GetUpgradeBannerClosureTimestampUseCase(hotWalletRepository)
    }
}