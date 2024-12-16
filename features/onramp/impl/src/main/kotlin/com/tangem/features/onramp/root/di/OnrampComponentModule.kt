package com.tangem.features.onramp.root.di

import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.root.DefaultOnrampComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampComponentModule {

    @Binds
    @Singleton
    fun bindOnrampComponentFactory(factory: DefaultOnrampComponent.Factory): OnrampComponent.Factory
}