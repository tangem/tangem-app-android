package com.tangem.features.hotwallet.viewphrase.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.ViewPhraseComponent
import com.tangem.features.hotwallet.viewphrase.DefaultViewPhraseComponent
import com.tangem.features.hotwallet.viewphrase.model.ViewPhraseModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ViewPhraseModule {

    @Binds
    @IntoMap
    @ClassKey(ViewPhraseModel::class)
    fun bindViewPhraseModel(model: ViewPhraseModel): Model

    @Binds
    fun bindViewPhraseComponentFactory(factory: DefaultViewPhraseComponent.Factory): ViewPhraseComponent.Factory
}