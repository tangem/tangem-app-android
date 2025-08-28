package com.tangem.features.hotwallet.updateaccesscode.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.UpdateAccessCodeComponent
import com.tangem.features.hotwallet.updateaccesscode.DefaultUpdateAccessCodeComponent
import com.tangem.features.hotwallet.updateaccesscode.UpdateAccessCodeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface UpdateAccessCodeModule {

    @Binds
    fun bindUpdateAccessCodeComponentFactory(
        impl: DefaultUpdateAccessCodeComponent.Factory,
    ): UpdateAccessCodeComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(UpdateAccessCodeModel::class)
    fun bindUpdateAccessCodeModel(model: UpdateAccessCodeModel): Model
}