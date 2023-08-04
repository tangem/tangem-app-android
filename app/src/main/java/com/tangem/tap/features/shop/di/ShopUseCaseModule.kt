package com.tangem.tap.features.shop.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.tap.features.shop.data.DefaultShopRepository
import com.tangem.tap.features.shop.domain.DefaultShopifyOrderingAvailabilityUseCase
import com.tangem.tap.features.shop.domain.GetShopifySalesProductsUseCase
import com.tangem.tap.features.shop.domain.ShopRepository
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
    fun provideShopifyOrderingAvailabilityUseCase(shopRepository: ShopRepository): ShopifyOrderingAvailabilityUseCase {
        return DefaultShopifyOrderingAvailabilityUseCase(
            shopRepository = shopRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetShopifySalesProductsUseCase(shopRepository: ShopRepository): GetShopifySalesProductsUseCase {
        return GetShopifySalesProductsUseCase(
            shopRepository = shopRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideDefaultShopRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): ShopRepository {
        return DefaultShopRepository(tangemTechApi = tangemTechApi, dispatchers = dispatchers)
    }
}