package com.tangem.features.forceupdate.impl.di

import com.tangem.features.forceupdate.ForceUpdateComponent
import com.tangem.features.forceupdate.ForceUpdateContinuation
import com.tangem.features.forceupdate.impl.DefaultForceUpdateContinuation
import com.tangem.features.forceupdate.impl.component.DefaultForceUpdateComponent
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
    fun bindForceUpdateComponentFactory(factory: DefaultForceUpdateComponent.Factory): ForceUpdateComponent.Factory

    @Binds
    @Singleton
    fun bindForceUpdateContinuation(impl: DefaultForceUpdateContinuation): ForceUpdateContinuation
}