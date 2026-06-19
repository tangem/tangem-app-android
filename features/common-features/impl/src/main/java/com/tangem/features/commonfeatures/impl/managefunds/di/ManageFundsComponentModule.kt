package com.tangem.features.commonfeatures.impl.managefunds.di

import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.commonfeatures.impl.managefunds.DefaultManageFundsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ManageFundsComponentModule {

    @Binds
    fun bindManageFundsComponentFactory(factory: DefaultManageFundsComponent.Factory): ManageFundsComponent.Factory
}