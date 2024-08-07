package com.tangem.feature.walletsettings.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.walletsettings.model.WalletSettingsModel
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
    @ClassKey(WalletSettingsModel::class)
    fun provideWalletSettingsModel(model: WalletSettingsModel): Model
}