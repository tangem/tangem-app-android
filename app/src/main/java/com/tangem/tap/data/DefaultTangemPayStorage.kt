package com.tangem.tap.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.encodeToByteArray

private const val DEFAULT_KEY = "tangem_pay_default_key"
private const val DEFAULT_CUSTOMER_WALLET_ADDRESS_KEY = "tangem_pay_default_customer_wallet_address_key"

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
    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val tokensAdapter by lazy { moshi.adapter(VisaAuthTokens::class.java) }

    override suspend fun storeAuthTokens(customerWalletAddress: String, tokens: VisaAuthTokens) =
        withContext(dispatcherProvider.io) {
            val json = tokensAdapter.toJson(tokens)

            secureStorage.store(
                json.encodeToByteArray(throwOnInvalidSequence = true),
                createKey(customerWalletAddress),
            )
        }

    override suspend fun getAuthTokens(customerWalletAddress: String): VisaAuthTokens? =
        withContext(dispatcherProvider.io) {
            secureStorage.get(createKey(customerWalletAddress))
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(tokensAdapter::fromJson)
        }

    /**
     * Store the only customer wallet address, since for the f&f user can issue only one card tied to one address
     */
    override suspend fun storeCustomerWalletAddress(customerWalletAddress: String) =
        withContext(dispatcherProvider.io) {
            secureStorage.store(
                customerWalletAddress.encodeToByteArray(throwOnInvalidSequence = true),
                DEFAULT_CUSTOMER_WALLET_ADDRESS_KEY,
            )
        }

    override suspend fun getCustomerWalletAddress(): String? = withContext(dispatcherProvider.io) {
        secureStorage.get(DEFAULT_CUSTOMER_WALLET_ADDRESS_KEY)?.decodeToString(throwOnInvalidSequence = true)
    }

    override suspend fun clear(customerWalletAddress: String) = withContext(dispatcherProvider.io) {
        secureStorage.delete(createKey(customerWalletAddress))
    }

    private fun createKey(cardId: String): String = "${DEFAULT_KEY}_$cardId"
}