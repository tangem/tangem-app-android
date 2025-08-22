package com.tangem.feature.tester.di

import com.tangem.feature.tester.presentation.navigation.DefaultTesterRouter
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal interface TesterRouterModule {

    @Binds
    @ActivityScoped
    fun bindTesterRouter(defaultTesterRouter: DefaultTesterRouter): InnerTesterRouter
}