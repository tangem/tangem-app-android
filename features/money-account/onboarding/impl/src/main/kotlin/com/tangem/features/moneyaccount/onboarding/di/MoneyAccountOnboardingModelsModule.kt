package com.tangem.features.moneyaccount.onboarding.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.moneyaccount.onboarding.model.MoneyAccountOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface MoneyAccountOnboardingModelsModule {

    @Binds
    @IntoMap
    @ClassKey(MoneyAccountOnboardingModel::class)
    fun bindMoneyAccountOnboardingModel(model: MoneyAccountOnboardingModel): Model
}