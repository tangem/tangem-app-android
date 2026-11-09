package com.tangem.features.earn.flow.impl.di

import com.tangem.features.earn.product.api.session.EarnSession
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnSessionsModule {

    @Multibinds
    fun sessionFactories(): Map<String, EarnSession.Factory>
}