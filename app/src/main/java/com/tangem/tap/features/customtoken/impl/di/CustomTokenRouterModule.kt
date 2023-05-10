package com.tangem.tap.features.customtoken.impl.di

import com.tangem.tap.features.customtoken.impl.presentation.routers.CustomTokenRouter
import com.tangem.tap.features.customtoken.impl.presentation.routers.DefaultCustomTokenRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
* [REDACTED_AUTHOR]
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object CustomTokenRouterModule {

    @Provides
    @ViewModelScoped
    fun provideAddCustomTokenRouter(): CustomTokenRouter = DefaultCustomTokenRouter()
}
