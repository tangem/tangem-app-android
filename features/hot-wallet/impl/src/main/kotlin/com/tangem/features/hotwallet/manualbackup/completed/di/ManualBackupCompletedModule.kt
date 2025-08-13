package com.tangem.features.hotwallet.manualbackup.completed.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.manualbackup.completed.ManualBackupCompletedModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ManualBackupCompletedModule {

    @Binds
    @IntoMap
    @ClassKey(ManualBackupCompletedModel::class)
    fun bindManualBackupCompletedModel(model: ManualBackupCompletedModel): Model
}