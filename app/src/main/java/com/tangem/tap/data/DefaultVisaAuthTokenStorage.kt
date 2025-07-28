package com.tangem.tap.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
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

    private val secureStorage by lazy {
        AndroidSecureStorageV2(
            appContext = applicationContext,
            useStrongBox = false,
            name = "visa_auth_storage",
        )
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val tokensAdapter by lazy { moshi.adapter(VisaAuthTokens::class.java) }

    override suspend fun store(cardId: String, tokens: VisaAuthTokens) = withContext(dispatcherProvider.io) {
        val json = tokensAdapter.toJson(tokens)

        secureStorage.store(
            json.encodeToByteArray(throwOnInvalidSequence = true),
            createKey(cardId),
        )
    }

    override suspend fun get(cardId: String): VisaAuthTokens? = withContext(dispatcherProvider.io) {
        secureStorage.get(createKey(cardId))
            ?.decodeToString(throwOnInvalidSequence = true)
            ?.let(tokensAdapter::fromJson)
    }

    override fun remove(cardId: String) {
        secureStorage.delete(createKey(cardId))
    }

    private fun createKey(cardId: String): String = "visa_auth_tokens_$cardId"
}