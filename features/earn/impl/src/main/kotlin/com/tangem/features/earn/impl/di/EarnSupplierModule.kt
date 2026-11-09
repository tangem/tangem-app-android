package com.tangem.features.earn.impl.di

import com.tangem.features.earn.impl.overview.EarnProductSummaryProducer
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EarnSupplierModule {

    @Provides
    @Singleton
    fun provideEarnProductSummarySupplier(factory: EarnProductSummaryProducer.Factory): EarnProductSummarySupplier {
        return EarnProductSummarySupplier(
            factory = factory,
            keyCreator = { params -> "earn_product_summaries_${params.userWalletId.stringValue}" },
        )
    }
}