package com.tangem.tap.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorage
import com.tangem.sdk.storage.createEncryptedSharedPreferences
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultVisaAuthTokenStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : VisaAuthTokenStorage {

    private val secureStorage = AndroidSecureStorage(
        preferences = SecureStorage.createEncryptedSharedPreferences(
            context = applicationContext,
            storageName = "visa_auth_storage",
        ),
    )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val tokensAdapter = moshi.adapter(VisaAuthTokens::class.java)

    override suspend fun store(tokens: VisaAuthTokens) = withContext(dispatcherProvider.io) {
        val json = tokensAdapter.toJson(tokens)

        secureStorage.store(
            json.encodeToByteArray(throwOnInvalidSequence = true),
            VISA_AUTH_TOKENS_KEY,
        )
    }

    override suspend fun get(): VisaAuthTokens? = withContext(dispatcherProvider.io) {
        secureStorage.get(VISA_AUTH_TOKENS_KEY)
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let(tokensAdapter::fromJson)
    }

    private companion object {
        const val VISA_AUTH_TOKENS_KEY = "visa_auth_tokens"
    }
}