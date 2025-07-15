package com.tangem.features.hotwallet.createmobilewallet.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.hotwallet.createmobilewallet.CreateMobileWalletModel
import com.tangem.features.hotwallet.createmobilewallet.DefaultCreateMobileWalletComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal object CreateMobileWalletModule

@Module
@InstallIn(ActivityComponent::class)
internal interface CreateMobileWalletModuleActivityBinds {

    @Binds
    @ActivityScoped
    fun bindCreateMobileWalletComponentFactory(
        impl: DefaultCreateMobileWalletComponent.Factory,
    ): CreateMobileWalletComponent.Factory
}

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateMobileWalletModuleBinds {

    @Binds
    @IntoMap
    @ClassKey(CreateMobileWalletModel::class)
    fun bindCreateMobileWalletModel(model: CreateMobileWalletModel): Model
}