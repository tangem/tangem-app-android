package com.tangem.datasource.di

import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.promotion.DefaultPromotionsSupplier
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PromotionModule {

    @Provides
    @Singleton
    fun providePromotionsSupplier(
        tangemApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): PromotionsSupplier {
        return DefaultPromotionsSupplier(
            tangemApi = tangemApi,
            store = RuntimeSharedStore<Map<UserWalletId, PromotionsResponse>>(),
            dispatchers = dispatchers,
        )
    }
}