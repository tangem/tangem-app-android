package com.tangem.google.drive.di

import android.content.Context
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.google.auth.GoogleIdentityAuthorizer
import com.tangem.google.drive.AppDataDrive
import com.tangem.google.drive.DefaultGoogleDriveTokenProvider
import com.tangem.google.drive.GoogleDriveApi
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.google.drive.store.DefaultDriveAccountStore
import com.tangem.google.drive.store.DriveAccountStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GoogleDriveModule {

    private const val MIME_TYPE_JSON = "application/json"
    private const val TIMEOUT_SECONDS = 30L

    // `encodeDefaults = false`: Drive v3 expects optional request-body fields (`mimeType`/`parents`/
    // `appProperties`) absent rather than explicitly `null`
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideGoogleDriveApi(): GoogleDriveApi {
        // no logging interceptor on purpose: requests carry the OAuth bearer token and the file contents
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(GoogleDriveApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(MIME_TYPE_JSON.toMediaType()))
            .build()
            .create(GoogleDriveApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleDriveTokenProvider(
        authorizer: GoogleIdentityAuthorizer,
        @ApplicationContext context: Context,
        networkConnectionManager: NetworkConnectionManager,
    ): GoogleDriveTokenProvider {
        return DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            context = context,
            networkConnectionManager = networkConnectionManager,
            scopes = listOf(DefaultGoogleDriveTokenProvider.DRIVE_FILE_SCOPE),
        )
    }

    @Provides
    @Singleton
    @AppDataDrive
    fun provideAppDataDriveTokenProvider(
        authorizer: GoogleIdentityAuthorizer,
        @ApplicationContext context: Context,
        networkConnectionManager: NetworkConnectionManager,
    ): GoogleDriveTokenProvider {
        return DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            context = context,
            networkConnectionManager = networkConnectionManager,
            scopes = listOf(DefaultGoogleDriveTokenProvider.DRIVE_APP_DATA_SCOPE),
        )
    }

    @Provides
    @Singleton
    fun provideDriveAccountStore(dataStoreFactory: AppDataStoreFactory): DriveAccountStore =
        createDriveAccountStore(dataStoreFactory, fileName = "drive_file_wallet_accounts")

    @Provides
    @Singleton
    @AppDataDrive
    fun provideAppDataDriveAccountStore(dataStoreFactory: AppDataStoreFactory): DriveAccountStore =
        createDriveAccountStore(dataStoreFactory, fileName = "drive_appdata_wallet_accounts")

    private fun createDriveAccountStore(dataStoreFactory: AppDataStoreFactory, fileName: String): DriveAccountStore {
        val dataStore = dataStoreFactory.create(defaultValue = emptyMap<String, String>(), fileName = fileName)
        return DefaultDriveAccountStore(dataStore = dataStore)
    }
}