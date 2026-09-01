package com.tangem.features.hotwallet.restorecloudbackup.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.restorecloudbackup.model.RestoreCloudBackupModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface RestoreCloudBackupModule {

    @Binds
    @IntoMap
    @ClassKey(RestoreCloudBackupModel::class)
    fun bindRestoreCloudBackupModel(model: RestoreCloudBackupModel): Model
}