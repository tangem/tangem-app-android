package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.model.TangemPayTxHistoryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TangemPayModelModule {

    @Binds
    @IntoMap
    @ClassKey(TangemPayDetailsModel::class)
    fun bindTangemPayDetailsModel(model: TangemPayDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayTxHistoryModel::class)
    fun bindTangemPayTxHistoryModel(model: TangemPayTxHistoryModel): Model
}