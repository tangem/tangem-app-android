package com.tangem.tap.di.domain

import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.tap.domain.scanCard.DefaultScanCardProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardLegacyDomainModule {

    @Provides
    fun provideScanCardProcessor(scanCardProcessor: DefaultScanCardProcessor): ScanCardProcessor {
        return scanCardProcessor
    }

    @Provides
    @Singleton
    fun providesWalletNameGenerateUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GenerateWalletNameUseCase {
        return GenerateWalletNameUseCase(userWalletsListRepository = userWalletsListRepository)
    }
}