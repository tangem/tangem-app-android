package com.tangem.features.hotwallet.manualbackup.check.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.manualbackup.check.model.ManualBackupCheckModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ManualBackupCheckModule {

    @Binds
    @IntoMap
    @ClassKey(ManualBackupCheckModel::class)
    fun bindManualBackupCheckModel(model: ManualBackupCheckModel): Model
}