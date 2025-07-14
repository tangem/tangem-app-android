package com.tangem.features.swap.v2.impl.sendviaswap.confirm.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.SendWithSwapConfirmModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface SendWithSwapConfirmModule {

    @Binds
    @IntoMap
    @ClassKey(SendWithSwapConfirmModel::class)
    fun bindsSendWithSwapConfirmModel(impl: SendWithSwapConfirmModel): Model
}