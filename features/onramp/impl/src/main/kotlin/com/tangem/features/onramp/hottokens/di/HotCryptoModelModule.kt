package com.tangem.features.onramp.hottokens.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.hottokens.model.HotCryptoModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface HotCryptoModelModule {

    @Binds
    @IntoMap
    @ClassKey(HotCryptoModel::class)
    fun bindHotCryptoModel(model: HotCryptoModel): Model
}