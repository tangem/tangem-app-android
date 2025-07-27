package com.tangem.features.hotwallet.accesscode.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.accesscode.confirm.ConfirmAccessCodeModel
import com.tangem.features.hotwallet.accesscode.set.SetAccessCodeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AccessCodeModule {

    @Binds
    @IntoMap
    @ClassKey(SetAccessCodeModel::class)
    fun bindSetAccessCodeModel(model: SetAccessCodeModel): Model

    @Binds
    @IntoMap
    @ClassKey(ConfirmAccessCodeModel::class)
    fun bindConfirmAccessCodeModel(model: ConfirmAccessCodeModel): Model
}