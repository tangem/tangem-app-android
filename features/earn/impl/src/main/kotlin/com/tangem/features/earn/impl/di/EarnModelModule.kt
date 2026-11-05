package com.tangem.features.earn.impl.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.earn.impl.widgets.calculator.model.EarnCalculatorModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnModelModule {

    @Binds
    @IntoMap
    @ClassKey(EarnCalculatorModel::class)
    fun bindEarnCalculatorModel(model: EarnCalculatorModel): Model
}