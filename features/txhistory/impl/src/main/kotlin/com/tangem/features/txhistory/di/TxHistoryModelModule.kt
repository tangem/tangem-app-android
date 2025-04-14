package com.tangem.features.txhistory.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.txhistory.model.TxHistoryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TxHistoryModelModule {
    @Binds
    @IntoMap
    @ClassKey(TxHistoryModel::class)
    fun bindModel(model: TxHistoryModel): Model
}