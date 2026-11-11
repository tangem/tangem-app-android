package com.tangem.store.features.cardsettings.impl.resetcard.di

import com.tangem.core.decompose.model.Model
import com.tangem.store.features.cardsettings.impl.resetcard.DefaultResetCardComponent
import com.tangem.store.features.cardsettings.impl.resetcard.api.ResetCardComponent
import com.tangem.store.features.cardsettings.impl.resetcard.model.ResetCardModel
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