package com.tangem.tap.di

import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.tap.proxy.AppStateHolder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppStateHolderModule {

    @Binds
    @Singleton
    fun bindsReduxStateHolder(appStateHolder: AppStateHolder): ReduxStateHolder
}