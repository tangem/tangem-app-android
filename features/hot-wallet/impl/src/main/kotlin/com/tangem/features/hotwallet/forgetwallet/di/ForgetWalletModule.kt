package com.tangem.features.hotwallet.forgetwallet.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.ForgetWalletComponent
import com.tangem.features.hotwallet.forgetwallet.DefaultForgetWalletComponent
import com.tangem.features.hotwallet.forgetwallet.ForgetWalletModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ForgetWalletModule {

    @Binds
    fun bindForgetWalletComponentFactory(impl: DefaultForgetWalletComponent.Factory): ForgetWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(ForgetWalletModel::class)
    fun bindForgetWalletModel(model: ForgetWalletModel): Model
}