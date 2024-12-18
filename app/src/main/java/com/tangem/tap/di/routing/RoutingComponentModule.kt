package com.tangem.tap.di.routing

import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.component.impl.DefaultRoutingComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal interface RoutingComponentModule {

    @Binds
    @ActivityScoped
    fun bindRoutingComponentFactory(factory: DefaultRoutingComponent.Factory): RoutingComponent.Factory
}