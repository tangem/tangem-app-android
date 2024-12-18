package com.tangem.tap.features.welcome.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.welcome.model.WelcomeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(WelcomeModel::class)
    fun bindWelcomeModel(model: WelcomeModel): Model
}