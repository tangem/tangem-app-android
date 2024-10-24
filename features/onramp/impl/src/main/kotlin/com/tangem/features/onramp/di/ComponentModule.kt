package com.tangem.features.onramp.di

import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.component.ResidenceComponent
import com.tangem.features.onramp.component.impl.DefaultOnrampComponent
import com.tangem.features.onramp.component.impl.DefaultResidenceComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindOnrampComponentFactory(factory: DefaultOnrampComponent.Factory): OnrampComponent.Factory

    @Binds
    @Singleton
    fun bindResidenceComponentFactory(factory: DefaultResidenceComponent.Factory): ResidenceComponent.Factory
}
