package com.tangem.features.hotwallet.walletactivation.entry.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.WalletActivationComponent
import com.tangem.features.hotwallet.walletactivation.entry.DefaultWalletActivationComponent
import com.tangem.features.hotwallet.walletactivation.entry.WalletActivationModel
import com.tangem.features.hotwallet.walletactivation.entry.WalletActivationStepperStateManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletActivationModuleBinds {

    @Binds
    @Singleton
    fun bindWalletActivationComponentFactory(
        impl: DefaultWalletActivationComponent.Factory,
    ): WalletActivationComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(WalletActivationModel::class)
    fun bindWalletActivationModel(model: WalletActivationModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal object WalletActivationModule {

    @Provides
    @Singleton
    fun provideWalletActivationStepperStateManager(): WalletActivationStepperStateManager {
        return WalletActivationStepperStateManager()
    }
}