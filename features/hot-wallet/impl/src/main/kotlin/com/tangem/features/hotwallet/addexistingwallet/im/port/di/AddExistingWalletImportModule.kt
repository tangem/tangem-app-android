package com.tangem.features.hotwallet.addexistingwallet.im.port.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.AddExistingWalletImportModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AddExistingWalletImportModule {

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletImportModel::class)
    fun bindAddExistingWalletImportModel(model: AddExistingWalletImportModel): Model
}