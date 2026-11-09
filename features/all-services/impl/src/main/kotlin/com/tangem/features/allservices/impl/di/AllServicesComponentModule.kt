package com.tangem.features.allservices.impl.di

import com.tangem.features.allservices.api.AllServicesComponent
import com.tangem.features.allservices.impl.DefaultAllServicesComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AllServicesComponentModule {

    @Binds
    fun bindAllServicesComponentFactory(impl: DefaultAllServicesComponent.Factory): AllServicesComponent.Factory
}