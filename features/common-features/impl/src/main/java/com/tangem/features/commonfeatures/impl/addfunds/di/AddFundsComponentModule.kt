package com.tangem.features.commonfeatures.impl.addfunds.di

import com.tangem.features.commonfeatures.api.addfunds.AddFundsComponent
import com.tangem.features.commonfeatures.impl.addfunds.DefaultAddFundsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AddFundsComponentModule {

    @Binds
    fun bindAddFundsComponentFactory(factory: DefaultAddFundsComponent.Factory): AddFundsComponent.Factory
}