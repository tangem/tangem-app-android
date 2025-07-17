package com.tangem.features.swap.v2.impl.sendviaswap.success.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.impl.sendviaswap.success.model.SendWithSwapSuccessModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface SendWithSwapSuccessModule {

    @Binds
    @IntoMap
    @ClassKey(SendWithSwapSuccessModel::class)
    fun bindSendWithSwapSuccessModel(impl: SendWithSwapSuccessModel): Model
}