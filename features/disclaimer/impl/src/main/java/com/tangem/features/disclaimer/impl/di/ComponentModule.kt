package com.tangem.features.disclaimer.impl.di

import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.disclaimer.impl.component.impl.DefaultDisclaimerComponent
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
    fun bindDisclaimerComponentFactory(factory: DefaultDisclaimerComponent.Factory): DisclaimerComponent.Factory
}
