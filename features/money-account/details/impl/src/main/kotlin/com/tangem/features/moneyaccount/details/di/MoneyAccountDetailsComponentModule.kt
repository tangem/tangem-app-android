package com.tangem.features.moneyaccount.details.di

import com.tangem.features.moneyaccount.details.component.MoneyAccountAddFundsBottomSheetComponent
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.moneyaccount.details.DefaultMoneyAccountDetailsComponent
import com.tangem.features.moneyaccount.details.addfunds.DefaultMoneyAccountAddFundsBottomSheetComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MoneyAccountDetailsComponentModule {

    @Binds
    fun bindMoneyAccountDetailsComponentFactory(
        factory: DefaultMoneyAccountDetailsComponent.Factory,
    ): MoneyAccountDetailsComponent.Factory

    @Binds
    fun bindMoneyAccountAddFundsComponentFactory(
        factory: DefaultMoneyAccountAddFundsBottomSheetComponent.Factory,
    ): MoneyAccountAddFundsBottomSheetComponent.Factory
}