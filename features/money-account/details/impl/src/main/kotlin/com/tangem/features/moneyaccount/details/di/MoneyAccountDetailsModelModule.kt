package com.tangem.features.moneyaccount.details.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.moneyaccount.details.MoneyAccountDetailsModel
import com.tangem.features.moneyaccount.details.addfunds.MoneyAccountAddFundsModel
import com.tangem.features.moneyaccount.details.addfunds.options.MoneyAccountAddFundsOptionsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface MoneyAccountDetailsModelModule {

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountDetailsModel::class)
    fun bindMoneyAccountDetailsModel(model: MoneyAccountDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountAddFundsModel::class)
    fun bindMoneyAccountAddFundsModel(model: MoneyAccountAddFundsModel): Model

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountAddFundsOptionsModel::class)
    fun bindMoneyAccountAddFundsOptionsModel(model: MoneyAccountAddFundsOptionsModel): Model
}