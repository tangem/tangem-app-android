package com.tangem.tap.di.domain

import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineCategoriesUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachinesBatchFlowUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CollectiblesDomainModule {

    @Provides
    @Singleton
    fun provideGetCollectibleGachaMachineCategoriesUseCase(
        repository: CollectiblesRepository,
    ): GetCollectibleGachaMachineCategoriesUseCase {
        return GetCollectibleGachaMachineCategoriesUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun provideGetCollectibleGachaMachinesBatchFlowUseCase(
        repository: CollectiblesRepository,
    ): GetCollectibleGachaMachinesBatchFlowUseCase {
        return GetCollectibleGachaMachinesBatchFlowUseCase(repository = repository)
    }
}