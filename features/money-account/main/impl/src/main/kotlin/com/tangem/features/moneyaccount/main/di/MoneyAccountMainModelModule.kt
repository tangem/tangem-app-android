package com.tangem.features.moneyaccount.main.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.moneyaccount.main.model.MoneyAccountActivationModel
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
    @ClassKey(MoneyAccountActivationModel::class)
    fun bindMoneyAccountActivationModel(model: MoneyAccountActivationModel): Model
}