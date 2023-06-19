package com.tangem.tap.features.shop.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.tap.features.shop.data.DefaultShopRepository
import com.tangem.tap.features.shop.domain.DefaultShopifyOrderingAvailabilityUseCase
import com.tangem.tap.features.shop.domain.ShopifyOrderingAvailabilityUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object ShopUseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideShopifyOrderingAvailabilityUseCase(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): ShopifyOrderingAvailabilityUseCase {
        return DefaultShopifyOrderingAvailabilityUseCase(
            shopRepository = DefaultShopRepository(tangemTechApi, dispatchers),
        )
    }
}