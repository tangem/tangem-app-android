package com.tangem.store.features.cardsettings.impl.main.di

import com.tangem.core.decompose.model.Model
import com.tangem.store.features.cardsettings.impl.main.model.CardSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSettingsMainFeatureModule {

    @Binds
    @IntoMap
    @ClassKey(CardSettingsModel::class)
    fun bindModel(model: CardSettingsModel): Model
}