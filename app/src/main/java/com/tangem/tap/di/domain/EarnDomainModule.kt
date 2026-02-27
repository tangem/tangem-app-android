package com.tangem.tap.di.domain

import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.earn.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object EarnDomainModule {

    @Provides
    fun provideFetchEarnNetworksUseCase(repository: EarnRepository): FetchEarnNetworksUseCase {
        return FetchEarnNetworksUseCase(repository)
    }

    @Provides
    fun provideGetEarnNetworksUseCase(
        earnRepository: EarnRepository,
        multiAccountListSupplier: MultiAccountListSupplier,
        userWalletsListRepository: UserWalletsListRepository,
    ): GetEarnNetworksUseCase {
        return GetEarnNetworksUseCase(
            earnRepository = earnRepository,
            multiAccountListSupplier = multiAccountListSupplier,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    fun provideFetchTopEarnTokensUseCase(repository: EarnRepository): FetchTopEarnTokensUseCase {
        return FetchTopEarnTokensUseCase(repository)
    }

    @Provides
    fun provideManageTopEarnTokensUseCase(repository: EarnRepository): GetTopEarnTokensUseCase {
        return GetTopEarnTokensUseCase(repository)
    }

    @Provides
    fun provideGetEarnTokensBatchFlowUseCase(repository: EarnRepository): GetEarnTokensBatchFlowUseCase {
        return GetEarnTokensBatchFlowUseCase(repository)
    }

    @Provides
    fun provideGetEarnFilterUseCase(repository: EarnRepository): GetEarnFilterUseCase {
        return GetEarnFilterUseCase(repository)
    }

    @Provides
    fun provideSetEarnFilterUseCase(repository: EarnRepository): SetEarnFilterUseCase {
        return SetEarnFilterUseCase(repository)
    }
}