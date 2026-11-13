package com.tangem.features.earn.lending.impl.di

import com.tangem.features.earn.lending.impl.product.LendingEarnProduct
import com.tangem.features.earn.lending.impl.session.LendingSession
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductKey
import com.tangem.features.earn.product.api.session.EarnSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface LendingModule {

    @Binds
    @IntoSet
    fun bindProduct(impl: LendingEarnProduct): EarnProduct

    @Binds
    @IntoMap
    @EarnProductKey(LendingEarnProduct.ID_VALUE)
    fun bindSessionFactory(impl: LendingSession.Factory): EarnSession.Factory
}