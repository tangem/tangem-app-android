package com.tangem.tap.di

import com.tangem.common.ui.backup.BackupErrorFeatureToggles
import com.tangem.common.ui.backup.DefaultBackupErrorFeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BackupErrorFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideBackupErrorFeatureToggles(featureTogglesManager: FeatureTogglesManager): BackupErrorFeatureToggles {
        return DefaultBackupErrorFeatureToggles(featureTogglesManager)
    }
}