package com.tangem.tap.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH_TOKENS_DEFAULT_KEY = "tangem_pay_default_key"
private const val WITHDRAW_ORDER_ID_KEY = "tangem_pay_withdraw_order_id_key"

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

    private val tokensAdapter by lazy { moshi.adapter(TangemPayAuthTokens::class.java) }

    override suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String) {
        withContext(dispatcherProvider.io) {
            appPreferencesStore
                .store(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId), customerWalletAddress)
        }
    }

    override suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String? {
        return withContext(dispatcherProvider.io) {
            appPreferencesStore.getSyncOrNull(key = PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId))
                .takeIf { !it.isNullOrEmpty() }
        }
    }

    override suspend fun storeAuthTokens(customerWalletAddress: String, tokens: TangemPayAuthTokens) =
        withContext(dispatcherProvider.io) {
            val json = tokensAdapter.toJson(tokens)

            secureStorage.store(
                json.encodeToByteArray(throwOnInvalidSequence = true),
                createAuthTokensKey(customerWalletAddress),
            )
        }

    override suspend fun getAuthTokens(customerWalletAddress: String): TangemPayAuthTokens? =
        withContext(dispatcherProvider.io) {
            secureStorage.get(createAuthTokensKey(customerWalletAddress))
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(tokensAdapter::fromJson)
        }

    override suspend fun storeOrderId(customerWalletAddress: String, orderId: String) {
        withContext(dispatcherProvider.io) {
            appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), orderId)
        }
    }

    override suspend fun getOrderId(customerWalletAddress: String): String? {
        return withContext(dispatcherProvider.io) {
            appPreferencesStore.getSyncOrNull(key = PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress))
                .takeIf { !it.isNullOrEmpty() }
        }
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
        appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), "")
    }

    override suspend fun storeCheckCustomerWalletResult(userWalletId: UserWalletId, isPaeraCustomer: Boolean) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId), isPaeraCustomer)
    }

    override suspend fun checkCustomerWalletResult(userWalletId: UserWalletId): Boolean? {
        return appPreferencesStore.getSyncOrNull(PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId))
    }

    override suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String) =
        withContext(dispatcherProvider.io) {
            secureStorage.delete(createAuthTokensKey(customerWalletAddress))
            appPreferencesStore.store(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId), "")
            appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), "")
            appPreferencesStore.store(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress), false)
            appPreferencesStore.store(PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId), false)
        }

    override suspend fun storeWithdrawOrder(userWalletId: UserWalletId, orderId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val orders = mutablePreferences.getObjectMap<String>(PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY)
                .plus(createWithdrawOrderIdKey(userWalletId) to orderId)
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY,
                value = orders,
            )
        }
    }

    override suspend fun getWithdrawOrderId(userWalletId: UserWalletId): String? {
        val orders = appPreferencesStore.getObjectMapSync<String>(PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY)
        return orders[createWithdrawOrderIdKey(userWalletId)]
    }

    override suspend fun deleteWithdrawOrder(userWalletId: UserWalletId) {
        appPreferencesStore.editData { mutablePreferences ->
            val orders = mutablePreferences.getObjectMap<String>(PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY)
                .minus(createWithdrawOrderIdKey(userWalletId))
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY,
                value = orders,
            )
        }
    }

    override suspend fun storeHideOnboardingBanner(userWalletId: UserWalletId, hide: Boolean) {
        withContext(dispatcherProvider.io) {
            appPreferencesStore.store(PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId), hide)
        }
    }

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean {
        return withContext(dispatcherProvider.io) {
            appPreferencesStore.getSyncOrNull(
                key = PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId),
            ) == true
        }
    }

    private fun createAuthTokensKey(address: String): String = "${AUTH_TOKENS_DEFAULT_KEY}_$address"

    private fun createWithdrawOrderIdKey(userWalletId: UserWalletId): String = "${WITHDRAW_ORDER_ID_KEY}_$userWalletId"
}