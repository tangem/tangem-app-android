package com.tangem.features.moneyaccount.main.di

import com.tangem.features.moneyaccount.main.component.DefaultMoneyAccountMainBlockComponent
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
}