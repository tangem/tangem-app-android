package com.tangem.features.virtualaccount.main.di

import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import com.tangem.features.virtualaccount.details.component.VirtualAccountMainComponent
import com.tangem.features.virtualaccount.main.DefaultVirtualAccountMainComponent
import com.tangem.features.virtualaccount.main.addfunds.DefaultVirtualAccountAddFundsBottomSheetComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface VirtualAccountMainComponentModule {

    @Binds
    fun bindVirtualAccountMainComponentFactory(
        factory: DefaultVirtualAccountMainComponent.Factory,
    ): VirtualAccountMainComponent.Factory

    @Binds
    fun bindVirtualAccountAddFundsComponentFactory(
        factory: DefaultVirtualAccountAddFundsBottomSheetComponent.Factory,
    ): VirtualAccountAddFundsBottomSheetComponent.Factory
}