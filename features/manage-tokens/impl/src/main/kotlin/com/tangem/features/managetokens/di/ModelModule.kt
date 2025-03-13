package com.tangem.features.managetokens.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.managetokens.model.CustomTokenFormModel
import com.tangem.features.managetokens.model.CustomTokenSelectorModel
import com.tangem.features.managetokens.model.ManageTokensModel
import com.tangem.features.managetokens.model.OnboardingManageTokensModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(ManageTokensModel::class)
    fun provideManageTokensModel(model: ManageTokensModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingManageTokensModel::class)
    fun provideOnboardingManageTokensModel(model: OnboardingManageTokensModel): Model

    @Binds
    @IntoMap
    @ClassKey(CustomTokenFormModel::class)
    fun provideCustomTokenFormModel(model: CustomTokenFormModel): Model

    @Binds
    @IntoMap
    @ClassKey(CustomTokenSelectorModel::class)
    fun provideCustomTokenSelectorModel(model: CustomTokenSelectorModel): Model
}