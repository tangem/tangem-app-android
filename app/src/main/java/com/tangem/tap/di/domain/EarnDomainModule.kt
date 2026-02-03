package com.tangem.tap.di.domain

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.earn.usecase.*
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
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
    fun provideManageEarnNetworksUseCase(
        earnRepository: EarnRepository,
        userWalletsListRepository: UserWalletsListRepository,
        multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    ): GetEarnNetworksUseCase {
        return GetEarnNetworksUseCase(
            earnRepository = earnRepository,
            userWalletsListRepository = userWalletsListRepository,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
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
}