package com.tangem.features.hotwallet.manualbackup.phrase.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.manualbackup.phrase.model.ManualBackupPhraseModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ManualBackupPhraseModule {

    @Binds
    @IntoMap
    @ClassKey(ManualBackupPhraseModel::class)
    fun bindManualBackupPhraseModel(model: ManualBackupPhraseModel): Model
}