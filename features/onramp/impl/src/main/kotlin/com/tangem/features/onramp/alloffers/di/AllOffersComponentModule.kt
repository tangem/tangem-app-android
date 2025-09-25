package com.tangem.features.onramp.alloffers.di

import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.alloffers.DefaultAllOffersComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AllOffersComponentModule {

    @Binds
    @Singleton
    fun bindAllOffersComponentFactory(factory: DefaultAllOffersComponent.Factory): AllOffersComponent.Factory
}