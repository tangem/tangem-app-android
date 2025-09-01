package com.tangem.features.hotwallet.addexistingwallet.entry.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.entry.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.entry.AddExistingWalletStepperStateManager
import com.tangem.features.hotwallet.addexistingwallet.entry.DefaultAddExistingWalletComponent
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.stepper.impl.DefaultHotWalletStepperComponent
import com.tangem.features.hotwallet.stepper.impl.HotWalletStepperModel
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
internal interface AddExistingWalletModuleBinds {

    @Binds
    @Singleton
    fun bindAddExistingWalletComponentFactory(
        impl: DefaultAddExistingWalletComponent.Factory,
    ): AddExistingWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletModel::class)
    fun bindAddExistingWalletModel(model: AddExistingWalletModel): Model

    @Binds
    fun bindFactory(impl: DefaultHotWalletStepperComponent.Factory): HotWalletStepperComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(HotWalletStepperModel::class)
    fun bindHotWalletStepperModel(model: HotWalletStepperModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal object AddExistingWalletModule {

    @Provides
    @Singleton
    fun provideAddExistingWalletStepperStateManager(): AddExistingWalletStepperStateManager {
        return AddExistingWalletStepperStateManager()
    }
}