package com.tangem.features.earn.lending.impl.provider.aave.di

import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.provider.aave.AaveAssetResolver
import com.tangem.features.earn.lending.impl.provider.aave.AaveLendingProvider
import com.tangem.features.earn.lending.impl.provider.aave.DefaultAaveAssetResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AaveLendingProviderModule {

    @Binds
    fun bindLendingProvider(impl: AaveLendingProvider): LendingProvider

    @Binds
    fun bindAssetResolver(impl: DefaultAaveAssetResolver): AaveAssetResolver
}