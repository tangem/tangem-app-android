package com.tangem.features.hotwallet.createcloudbackup.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.CreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.DefaultCreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.model.CreateCloudBackupModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateCloudBackupModule {

    @Binds
    @Singleton
    fun bindCreateCloudBackupComponentFactory(
        impl: DefaultCreateCloudBackupComponent.Factory,
    ): CreateCloudBackupComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CreateCloudBackupModel::class)
    fun bindCreateCloudBackupModel(model: CreateCloudBackupModel): Model
}