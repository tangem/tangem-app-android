package com.tangem.features.earn.impl.di

import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRateSource
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnProductsModule {

    @Multibinds
    fun products(): Set<EarnProduct>

    @Multibinds
    fun rateSources(): Set<EarnRateSource>
}