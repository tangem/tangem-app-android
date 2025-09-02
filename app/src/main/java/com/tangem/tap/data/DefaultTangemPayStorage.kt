package com.tangem.tap.data

import android.content.Context
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_KEY = "tangem_pay_default_key"

@Singleton
internal class DefaultTangemPayStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : TangemPayStorage {

    private val secureStorage by lazy {
        AndroidSecureStorageV2(
            appContext = applicationContext,
            useStrongBox = false,
            name = "tangem_pay_storage",
        )
    }

    override suspend fun store(authHeader: String) = withContext(dispatcherProvider.io) {
        secureStorage.store(authHeader.encodeToByteArray(throwOnInvalidSequence = true), DEFAULT_KEY)
    }

    override suspend fun get(): String? = withContext(dispatcherProvider.io) {
        secureStorage.get(DEFAULT_KEY)?.decodeToString(throwOnInvalidSequence = true)
    }

    override suspend fun clear() = withContext(dispatcherProvider.io) {
        secureStorage.delete(DEFAULT_KEY)
    }
}