package com.tangem.features.hotwallet.addexistingwallet.start.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AddExistingWalletStartModule {

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletStartModel::class)
    fun bindAddExistingWalletStartModel(model: AddExistingWalletStartModel): Model
}