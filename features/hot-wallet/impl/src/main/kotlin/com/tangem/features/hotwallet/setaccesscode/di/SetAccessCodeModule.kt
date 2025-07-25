package com.tangem.features.hotwallet.setaccesscode.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.setaccesscode.SetAccessCodeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SetAccessCodeModule {

    @Binds
    @IntoMap
    @ClassKey(SetAccessCodeModel::class)
    fun bindSetAccessCodeModel(model: SetAccessCodeModel): Model
}