package com.tangem.features.moneyaccount.main.di

import com.tangem.features.moneyaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import com.tangem.features.moneyaccount.details.component.MoneyAccountMainComponent
import com.tangem.features.moneyaccount.main.DefaultMoneyAccountMainComponent
import com.tangem.features.moneyaccount.main.addfunds.DefaultVirtualAccountAddFundsBottomSheetComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MoneyAccountMainComponentModule {

    @Binds
    fun bindMoneyAccountMainComponentFactory(
        factory: DefaultMoneyAccountMainComponent.Factory,
    ): MoneyAccountMainComponent.Factory

    @Binds
    fun bindVirtualAccountAddFundsComponentFactory(
        factory: DefaultVirtualAccountAddFundsBottomSheetComponent.Factory,
    ): VirtualAccountAddFundsBottomSheetComponent.Factory
}