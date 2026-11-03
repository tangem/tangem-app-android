package com.tangem.features.earn.impl.di

import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.impl.DefaultEarnEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnComponentModule {

    @Binds
    @Singleton
    fun bindEntryComponentFactory(impl: DefaultEarnEntryComponent.Factory): EarnEntryComponent.Factory
}