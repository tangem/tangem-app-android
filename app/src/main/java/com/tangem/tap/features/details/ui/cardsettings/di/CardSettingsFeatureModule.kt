package com.tangem.tap.features.details.ui.cardsettings.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.DefaultCardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.model.CardSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSettingsFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultCardSettingsComponent.Factory): CardSettingsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CardSettingsModel::class)
    fun bindModel(model: CardSettingsModel): Model
}