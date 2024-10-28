package com.tangem.feature.wallet.presentation.selecttoken.di

import com.tangem.feature.wallet.presentation.selecttoken.DefaultSelectTokenComponent
import com.tangem.feature.wallet.presentation.selecttoken.SelectTokenComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SelectTokenComponentModule {

    @Binds
    @Singleton
    fun bindSelectTokenComponentFactory(factory: DefaultSelectTokenComponent.Factory): SelectTokenComponent.Factory
}