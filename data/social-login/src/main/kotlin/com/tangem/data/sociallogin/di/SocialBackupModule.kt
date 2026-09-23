package com.tangem.data.sociallogin.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.data.sociallogin.backup.DefaultSocialBackupRepository
import com.tangem.data.sociallogin.crypto.DefaultRecoveryKeyGenerator
import com.tangem.data.sociallogin.crypto.SocialBackupCipher
import com.tangem.data.sociallogin.store.DefaultSocialBackupStore
import com.tangem.data.sociallogin.store.SocialBackupStore
import com.tangem.domain.sociallogin.crypto.RecoveryKeyGenerator
import com.tangem.domain.sociallogin.repository.SocialBackupRepository
import com.tangem.google.drive.AppDataDrive
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialBackupModule {

    @Binds
    @Singleton
    fun bindRecoveryKeyGenerator(impl: DefaultRecoveryKeyGenerator): RecoveryKeyGenerator

    companion object {

        @Provides
        @Singleton
        fun provideSocialBackupStore(
            @ApplicationContext context: Context,
            appScope: AppCoroutineScope,
        ): SocialBackupStore {
            val dataStore = DataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptySet(),
                    serializer = SetSerializer(String.serializer()),
                ),
                // an undecodable set must read as "no wallet is backed up", not throw out of every
                // backup check — claiming a backup that may not exist is the dangerous direction
                corruptionHandler = ReplaceFileCorruptionHandler { corruptionException ->
                    TangemLogger.e("Social backup ids store corrupted, replaced with an empty set", corruptionException)
                    emptySet()
                },
                produceFile = { context.dataStoreFile(fileName = "social_backup_wallet_ids") },
                scope = appScope,
            )
            return DefaultSocialBackupStore(dataStore = dataStore)
        }

        @Provides
        @Singleton
        fun provideSocialBackupRepository(
            api: GoogleDriveApi,
            @AppDataDrive tokenProvider: GoogleDriveTokenProvider,
            store: SocialBackupStore,
            dispatchers: CoroutineDispatcherProvider,
        ): SocialBackupRepository {
            return DefaultSocialBackupRepository(
                api = api,
                tokenProvider = tokenProvider,
                store = store,
                cipher = SocialBackupCipher(),
                dispatchers = dispatchers,
            )
        }
    }
}