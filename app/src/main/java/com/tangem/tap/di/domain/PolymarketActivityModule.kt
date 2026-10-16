package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.usecase.CreatePolymarketActivitySessionUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetAllPolymarketActivityUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketActivityModule {

    @Provides
    @Singleton
    fun provideGetAllPolymarketActivityUseCase(
        repository: PolymarketActivityRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
    ): GetAllPolymarketActivityUseCase = GetAllPolymarketActivityUseCase(repository, deriveAddresses)

    @Provides
    @Singleton
    fun provideCreatePolymarketActivitySessionUseCase(
        repository: PolymarketActivityRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        dispatchers: CoroutineDispatcherProvider,
    ): CreatePolymarketActivitySessionUseCase = CreatePolymarketActivitySessionUseCase(
        repository = repository,
        deriveAddresses = deriveAddresses,
        dispatchers = dispatchers,
    )
}