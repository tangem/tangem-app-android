package com.tangem.features.txhistory.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.txhistory.model.TxHistoryDetailsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TxHistoryDetailsModelModule {
    @Binds
    @IntoMap
    @ClassKey(TxHistoryDetailsModel::class)
    fun bindModel(model: TxHistoryDetailsModel): Model
}