package com.tangem.features.hotwallet.setupfinished.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface MobileWalletSetupFinishedModule {

    @Binds
    @IntoMap
    @ClassKey(MobileWalletSetupFinishedModel::class)
    fun bindMobileWalletSetupFinishedModel(model: MobileWalletSetupFinishedModel): Model
}