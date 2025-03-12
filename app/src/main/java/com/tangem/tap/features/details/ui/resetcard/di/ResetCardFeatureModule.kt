package com.tangem.tap.features.details.ui.resetcard.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.resetcard.DefaultResetCardComponent
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.features.details.ui.resetcard.model.ResetCardModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ResetCardFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultResetCardComponent.Factory): ResetCardComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(ResetCardModel::class)
    fun bindModel(model: ResetCardModel): Model
}