package com.tangem.data.promo.di

import com.tangem.data.promo.DefaultPromoRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.DevTangemApi
import com.tangem.domain.tokens.repository.PromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PromoDataModule {

    @Provides
    @Singleton
    fun providePromoRepository(
        @DevTangemApi tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): PromoRepository {
        return DefaultPromoRepository(tangemTechApi, dispatchers)
    }
}
