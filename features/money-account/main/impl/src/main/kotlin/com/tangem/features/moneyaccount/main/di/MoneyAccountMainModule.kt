package com.tangem.features.moneyaccount.main.di

import com.tangem.features.moneyaccount.main.component.DefaultMoneyAccountActivationComponent
import com.tangem.features.moneyaccount.main.component.DefaultMoneyAccountMainBlockComponent
import com.tangem.features.moneyaccount.main.component.MoneyAccountActivationComponent
import com.tangem.features.moneyaccount.main.component.MoneyAccountMainBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface MoneyAccountMainModule {
    @Binds
    fun bindMoneyAccountMainBlockComponent(
        factory: DefaultMoneyAccountMainBlockComponent.Factory,
    ): MoneyAccountMainBlockComponent.Factory

    @Binds
    fun bindMoneyAccountActivationComponent(
        factory: DefaultMoneyAccountActivationComponent.Factory,
    ): MoneyAccountActivationComponent.Factory
}