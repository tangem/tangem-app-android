package com.tangem.features.onramp.redirect.di

import com.tangem.features.onramp.redirect.DefaultOnrampRedirectComponent
import com.tangem.features.onramp.redirect.OnrampRedirectComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampRedirectComponentModule {

    @Binds
    @Singleton
    fun bindOnrampRedirectComponentFactory(
        factory: DefaultOnrampRedirectComponent.Factory,
    ): OnrampRedirectComponent.Factory
}