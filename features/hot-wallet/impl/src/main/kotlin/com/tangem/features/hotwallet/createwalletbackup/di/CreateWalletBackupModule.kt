package com.tangem.features.hotwallet.createwalletbackup.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.CreateWalletBackupComponent
import com.tangem.features.hotwallet.createwalletbackup.CreateWalletBackupModel
import com.tangem.features.hotwallet.createwalletbackup.CreateWalletBackupStepperStateManager
import com.tangem.features.hotwallet.createwalletbackup.DefaultCreateWalletBackupComponent
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
internal interface CreateWalletBackupModuleBinds {

    @Binds
    @Singleton
    fun bindCreateWalletBackupComponentFactory(
        impl: DefaultCreateWalletBackupComponent.Factory,
    ): CreateWalletBackupComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CreateWalletBackupModel::class)
    fun bindCreateWalletBackupModel(model: CreateWalletBackupModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal object CreateWalletBackupModule {

    @Provides
    @Singleton
    fun provideCreateWalletBackupStepperStateManager(): CreateWalletBackupStepperStateManager {
        return CreateWalletBackupStepperStateManager()
    }
}