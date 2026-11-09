package com.tangem.features.earn.staking.impl.di

import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.staking.impl.PooledStakingEarnProduct
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface PooledStakingEarnModule {

    @Binds
    @IntoSet
    fun bindPooledStakingEarnProduct(product: PooledStakingEarnProduct): EarnProduct
}