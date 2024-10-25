package com.tangem.features.onramp.di

import com.tangem.features.onramp.component.ConfirmResidencyComponent
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.component.impl.DefaultConfirmResidencyComponent
import com.tangem.features.onramp.component.impl.DefaultOnrampComponent
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

    @Binds
    @Singleton
    fun bindConfirmResidencyComponentFactory(
        factory: DefaultConfirmResidencyComponent.Factory,
    ): ConfirmResidencyComponent.Factory
}
