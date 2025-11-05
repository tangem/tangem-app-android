package com.tangem.tap.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_KEY = "tangem_pay_default_key"
private const val ORDER_ID_KEY = "tangem_pay_order_id_key"

@Singleton
internal class DefaultTangemPayStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @NetworkMoshi moshi: Moshi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
) : TangemPayStorage {

    private val secureStorage by lazy {
        AndroidSecureStorageV2(
            appContext = applicationContext,
            useStrongBox = false,
            name = "tangem_pay_storage",
        )
    }

    private val tokensAdapter by lazy { moshi.adapter(VisaAuthTokens::class.java) }

    override suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String) {
        withContext(dispatcherProvider.io) {
            secureStorage.store(key = createCustomerAddressKey(userWalletId), value = customerWalletAddress)
        }
    }

    override suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String? {
        return withContext(dispatcherProvider.io) {
            secureStorage.getAsString(createCustomerAddressKey(userWalletId))
        }
    }

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

    override suspend fun storeOrderId(customerWalletAddress: String, orderId: String) {
        withContext(dispatcherProvider.io) {
            secureStorage.store(
                orderId.encodeToByteArray(throwOnInvalidSequence = true),
                createOrderIdKey(customerWalletAddress),
            )
        }
    }

    override suspend fun getOrderId(customerWalletAddress: String): String? = withContext(dispatcherProvider.io) {
        secureStorage.get(createOrderIdKey(customerWalletAddress))?.decodeToString(throwOnInvalidSequence = true)
    }

    override suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean {
        return withContext(dispatcherProvider.io) {
            appPreferencesStore.getSyncOrNull(
                key = PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress),
            ) == true
        }
    }

    override suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean) {
        withContext(dispatcherProvider.io) {
            appPreferencesStore.store(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress), isDone)
        }
    }

    override suspend fun clearOrderId(customerWalletAddress: String) = withContext(dispatcherProvider.io) {
        secureStorage.delete(createOrderIdKey(customerWalletAddress))
    }

    override suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String) =
        withContext(dispatcherProvider.io) {
            secureStorage.delete(createCustomerAddressKey(userWalletId))
            secureStorage.delete(createKey(customerWalletAddress))
            secureStorage.delete(createOrderIdKey(customerWalletAddress))
            appPreferencesStore.store(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress), false)
        }

    private fun createCustomerAddressKey(userWalletId: UserWalletId): String = userWalletId.stringValue

    private fun createKey(address: String): String = "${DEFAULT_KEY}_$address"

    private fun createOrderIdKey(address: String): String = "${ORDER_ID_KEY}_$address"
}