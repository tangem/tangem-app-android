package com.tangem.data.cloudbackup.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.tangem.data.cloudbackup.CloudBackupJson
import com.tangem.data.cloudbackup.crypto.CloudBackupCipher
import com.tangem.data.cloudbackup.datasource.DefaultGoogleDriveTokenProvider
import com.tangem.data.cloudbackup.datasource.GoogleDriveApi
import com.tangem.data.cloudbackup.datasource.GoogleDriveAuthorizer
import com.tangem.data.cloudbackup.datasource.GoogleDriveTokenProvider
import com.tangem.data.cloudbackup.repository.DefaultCloudBackupRepository
import com.tangem.data.cloudbackup.store.CloudBackupStore
import com.tangem.data.cloudbackup.store.DefaultCloudBackupStore
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CloudBackupDataModule {

    private const val MIME_TYPE_JSON = "application/json"
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideGoogleDriveApi(): GoogleDriveApi {
        // no logging interceptor on purpose: requests carry the OAuth bearer token and the encrypted backup
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(GoogleDriveApi.BASE_URL)
            .client(client)
            .addConverterFactory(CloudBackupJson.asConverterFactory(MIME_TYPE_JSON.toMediaType()))
            .build()
            .create(GoogleDriveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveTokenProvider(
        authorizer: GoogleDriveAuthorizer,
        api: GoogleDriveApi,
        @ApplicationContext context: Context,
    ): GoogleDriveTokenProvider {
        return DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            api = api,
            context = context,
        )
    }

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
}