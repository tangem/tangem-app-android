package com.tangem.features.managetokens.di

import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.impl.DefaultManageTokensComponent
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
    fun bindManageTokensComponentFactory(factory: DefaultManageTokensComponent.Factory): ManageTokensComponent.Factory
}
