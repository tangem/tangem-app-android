package com.tangem.tap.data

import android.content.Context
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.spend.datasource.visa.storage.VisaAuthTokenStorage
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultVisaAuthTokenStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : VisaAuthTokenStorage {

    private val secureStorage by lazy {
        AndroidSecureStorageV2(
            appContext = applicationContext,
            useStrongBox = false,
            name = "visa_auth_storage",
        )
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun store(cardId: String, tokens: VisaAuthTokens) = withContext(dispatcherProvider.io) {
        val encoded = json.encodeToString(VisaAuthTokens.serializer(), tokens)

        secureStorage.store(
            encoded.encodeToByteArray(throwOnInvalidSequence = true),
            createKey(cardId),
        )
    }

    override suspend fun get(cardId: String): VisaAuthTokens? = withContext(dispatcherProvider.io) {
        secureStorage.get(createKey(cardId))
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let { encoded -> json.decodeFromString(VisaAuthTokens.serializer(), encoded) }
    }

    override fun remove(cardId: String) {
        secureStorage.delete(createKey(cardId))
    }

    private fun createKey(cardId: String): String = "visa_auth_tokens_$cardId"
}