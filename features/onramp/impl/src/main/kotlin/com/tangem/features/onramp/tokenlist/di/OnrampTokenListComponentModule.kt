package com.tangem.features.onramp.tokenlist.di

import com.tangem.features.onramp.tokenlist.DefaultOnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampTokenListComponentModule {

    @Binds
    @Singleton
    fun bindOnrampTokenListComponentFactory(
        factory: DefaultOnrampTokenListComponent.Factory,
    ): OnrampTokenListComponent.Factory
}