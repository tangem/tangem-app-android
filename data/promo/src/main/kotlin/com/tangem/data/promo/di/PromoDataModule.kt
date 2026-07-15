package com.tangem.data.promo.di

import com.squareup.moshi.Moshi
import com.tangem.data.promo.DefaultPromoRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.promo.PromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PromoDataModule {

    @Provides
    @Singleton
    fun providePromoRepository(
        promotionsSupplier: PromotionsSupplier,
        tangemApi: TangemTechApi,
        @NetworkMoshi moshi: Moshi,
        dispatchers: CoroutineDispatcherProvider,
    ): PromoRepository {
        return DefaultPromoRepository(
            promotionsSupplier = promotionsSupplier,
            tangemApi = tangemApi,
            moshi = moshi,
            dispatchers = dispatchers,
        )
    }
}