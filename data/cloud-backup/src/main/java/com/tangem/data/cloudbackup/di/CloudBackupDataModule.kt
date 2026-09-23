package com.tangem.data.cloudbackup.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.data.cloudbackup.crypto.CloudBackupCipher
import com.tangem.data.cloudbackup.repository.CredentialManagerPasswordRepository
import com.tangem.data.cloudbackup.repository.DefaultCloudBackupRepository
import com.tangem.data.cloudbackup.store.CloudBackupStore
import com.tangem.data.cloudbackup.store.DefaultCloudBackupStore
import com.tangem.domain.cloudbackup.repository.BackupPasswordRepository
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
internal object CloudBackupDataModule {

    @Provides
    @Singleton
    fun provideCloudBackupStore(@ApplicationContext context: Context, appScope: AppCoroutineScope): CloudBackupStore {
        val dataStore = DataStoreFactory.create(
            serializer = KotlinxDataStoreSerializer(
                defaultValue = emptySet(),
                serializer = SetSerializer(String.serializer()),
            ),
            produceFile = { context.dataStoreFile(fileName = "cloud_backup_wallet_ids") },
            scope = appScope,
        )
        return DefaultCloudBackupStore(dataStore = dataStore)
    }

    @Provides
    @Singleton
    fun provideCloudBackupRepository(
        api: GoogleDriveApi,
        tokenProvider: GoogleDriveTokenProvider,
        store: CloudBackupStore,
        dispatchers: CoroutineDispatcherProvider,
    ): CloudBackupRepository {
        return DefaultCloudBackupRepository(
            api = api,
            tokenProvider = tokenProvider,
            store = store,
            cipher = CloudBackupCipher(),
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideBackupPasswordRepository(
        @ApplicationContext context: Context,
        foregroundActivityProvider: ForegroundActivityProvider,
        dispatchers: CoroutineDispatcherProvider,
    ): BackupPasswordRepository {
        return CredentialManagerPasswordRepository(
            credentialManager = CredentialManager.create(context),
            foregroundActivityProvider = foregroundActivityProvider,
            dispatchers = dispatchers,
        )
    }
}