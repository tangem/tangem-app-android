package com.tangem.features.hotwallet.manualbackup.start.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.manualbackup.start.ManualBackupStartModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ManualBackupStartModule {

    @Binds
    @IntoMap
    @ClassKey(ManualBackupStartModel::class)
    fun bindManualBackupStartModel(model: ManualBackupStartModel): Model
}