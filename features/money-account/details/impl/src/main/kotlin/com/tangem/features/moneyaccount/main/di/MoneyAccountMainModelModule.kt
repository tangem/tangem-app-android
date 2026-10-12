package com.tangem.features.moneyaccount.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.moneyaccount.main.MoneyAccountMainModel
import com.tangem.features.moneyaccount.main.addfunds.MoneyAccountAddFundsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface MoneyAccountMainModelModule {

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountMainModel::class)
    fun bindMoneyAccountMainModel(model: MoneyAccountMainModel): Model

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountAddFundsModel::class)
    fun bindMoneyAccountAddFundsModel(model: MoneyAccountAddFundsModel): Model
}