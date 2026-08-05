package com.tangem.data.pay.store

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.data.pay.entity.WithdrawStoreData
import com.tangem.data.pay.util.WithdrawStateConverter
import com.tangem.data.pay.util.WithdrawStoreDataConverter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH_TOKENS_DEFAULT_KEY = "tangem_pay_default_key"
private const val WITHDRAW_ORDER_ID_KEY = "tangem_pay_withdraw_order_id_key"

@Suppress("TooManyFunctions")
@Singleton
internal class DefaultTangemPayStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @NetworkMoshi moshi: Moshi,
    @SdkMoshi private val sdkMoshi: Moshi,
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
    private val withdrawStoreDataConverter by lazy { WithdrawStoreDataConverter() }
    private val withdrawStateConverter by lazy { WithdrawStateConverter() }

    private val listType by lazy {
        Types.newParameterizedType(List::class.java, WithdrawStoreData::class.java)
    }
    private val mapType by lazy {
        Types.newParameterizedType(Map::class.java, String::class.java, listType)
    }
    private val adapter: JsonAdapter<Map<String, List<WithdrawStoreData>>> by lazy {
        sdkMoshi.adapter(mapType)
    }

    override suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String) {
        appPreferencesStore
            .store(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId), customerWalletAddress)
    }

    override suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String? {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId),
        )
            .takeIf { !it.isNullOrEmpty() }
    }

    override suspend fun storeAuthTokens(customerWalletAddress: String, tokens: TangemPayAuthTokens) =
        withContext(dispatcherProvider.io) {
            val json = tokensAdapter.toJson(tokens)

            secureStorage.store(
                json.encodeToByteArray(throwOnInvalidSequence = true),
                createAuthTokensKey(customerWalletAddress),
            )
        }

    override suspend fun getAuthTokens(customerWalletAddress: String): TangemPayAuthTokens? {
        return withContext(dispatcherProvider.io) {
            val authTokens = secureStorage.get(createAuthTokensKey(customerWalletAddress))
                ?.decodeToString(throwOnInvalidSequence = true)
                ?.let(tokensAdapter::fromJson)

            authTokens?.let { tokens ->
                if (tokens.idempotencyKey == null) {
                    val newAuthTokens = tokens.copy(idempotencyKey = UUID.randomUUID().toString())
                    storeAuthTokens(customerWalletAddress, newAuthTokens)
                    newAuthTokens
                } else {
                    tokens
                }
            }
        }
    }

    override suspend fun storeOrderId(customerWalletAddress: String, orderId: String) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), orderId)
    }

    override suspend fun getOrderId(customerWalletAddress: String): String? {
        return appPreferencesStore.getSyncOrNull(key = PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress))
            .takeIf { !it.isNullOrEmpty() }
    }

    override suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress),
        ) == true
    }

    override suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress), isDone)
    }

    override suspend fun getCashbackDeactivationDismissed(customerWalletAddress: String): Boolean {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayCashbackDeactivationDismissedKey(customerWalletAddress),
        ) == true
    }

    override suspend fun storeCashbackDeactivationDismissed(customerWalletAddress: String, isDismissed: Boolean) {
        appPreferencesStore.store(
            key = PreferencesKeys.getTangemPayCashbackDeactivationDismissedKey(customerWalletAddress),
            value = isDismissed,
        )
    }

    override suspend fun clearOrderId(customerWalletAddress: String) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), "")
    }

    override suspend fun storeVirtualAccountOrderId(customerWalletAddress: String, vaOrderId: String) {
        appPreferencesStore.store(
            key = PreferencesKeys.getTangemPayVirtualAccountOrderIdKey(customerWalletAddress),
            value = vaOrderId,
        )
    }

    override suspend fun getVirtualAccountOrderId(customerWalletAddress: String): String? {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayVirtualAccountOrderIdKey(customerWalletAddress),
        ).takeIf { !it.isNullOrEmpty() }
    }

    override suspend fun clearVirtualAccountOrderId(customerWalletAddress: String) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayVirtualAccountOrderIdKey(customerWalletAddress), "")
    }

    override suspend fun storeCheckCustomerWalletResult(userWalletId: UserWalletId, isPaeraCustomer: Boolean) {
        appPreferencesStore.store(
            PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId),
            isPaeraCustomer,
        )
    }

    override suspend fun checkCustomerWalletResult(userWalletId: UserWalletId): Boolean? {
        return appPreferencesStore.getSyncOrNull(PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId))
    }

    override suspend fun storeActiveWithdrawOrderId(userWalletId: UserWalletId, orderId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val orders = mutablePreferences.getObjectMap<String>(
                PreferencesKeys.TANGEM_PAY_ACTIVE_WITHDRAW_ORDERS_KEY,
            )
                .plus(createWithdrawOrderIdKey(userWalletId) to orderId)
            mutablePreferences.setObjectMap(
                key = PreferencesKeys.TANGEM_PAY_ACTIVE_WITHDRAW_ORDERS_KEY,
                value = orders,
            )
        }
    }

    override suspend fun storeWithdrawOrder(userWalletId: UserWalletId, data: TangemPayWithdrawState) {
        appPreferencesStore.editData { prefs ->
            val walletKey = createWithdrawOrderIdKey(userWalletId)
            val currentMap = prefs[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY]
                ?.let(adapter::fromJson)
                .orEmpty()
            val newItem = withdrawStoreDataConverter.convert(data)
            val currentList = currentMap[walletKey].orEmpty()
            val updatedList = buildList(currentList.size + 1) {
                for (item in currentList) { if (item.orderId != newItem.orderId) add(item) }
                add(newItem)
            }
            prefs[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY] =
                adapter.toJson(currentMap + (walletKey to updatedList))
        }
    }

    override suspend fun getActiveWithdrawOrderId(userWalletId: UserWalletId): String? {
        val orders = appPreferencesStore.getObjectMapSync<String>(
            PreferencesKeys.TANGEM_PAY_ACTIVE_WITHDRAW_ORDERS_KEY,
        )
        return orders[createWithdrawOrderIdKey(userWalletId)]
    }

    override suspend fun getWithdrawOrders(userWalletId: UserWalletId): List<TangemPayWithdrawState> {
        val map = appPreferencesStore.data.firstOrNull()
            ?.get(PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY)?.let(adapter::fromJson).orEmpty()
        return map[createWithdrawOrderIdKey(userWalletId)].orEmpty().map(withdrawStateConverter::convert)
    }

    override suspend fun deleteActiveWithdrawOrder(userWalletId: UserWalletId) {
        appPreferencesStore.editData { it.deleteActiveWithdrawOrderInternal(userWalletId) }
    }

    private fun MutablePreferences.deleteActiveWithdrawOrderInternal(userWalletId: UserWalletId) {
        val mutablePreferences = this

        // make as context parameter to avoid shadowing the outer `this` reference
        with(appPreferencesStore) {
            val orders = mutablePreferences.getObjectMap<String>(
                PreferencesKeys.TANGEM_PAY_ACTIVE_WITHDRAW_ORDERS_KEY,
            )
                .minus(createWithdrawOrderIdKey(userWalletId))

            mutablePreferences.setObjectMap(
                key = PreferencesKeys.TANGEM_PAY_ACTIVE_WITHDRAW_ORDERS_KEY,
                value = orders,
            )
        }
    }

    private fun MutablePreferences.deleteWithdrawOrdersInternal(userWalletId: UserWalletId) {
        val walletKey = createWithdrawOrderIdKey(userWalletId)
        val currentMap = this[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY]?.let(adapter::fromJson).orEmpty()

        this[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY] = adapter.toJson(currentMap - walletKey)
    }

    override suspend fun deleteWithdrawOrder(userWalletId: UserWalletId, orderId: String) {
        appPreferencesStore.editData { prefs ->
            val walletKey = createWithdrawOrderIdKey(userWalletId)
            val currentMap = prefs[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY]?.let(adapter::fromJson)
                .orEmpty()
            val currentList = currentMap[walletKey].orEmpty()
            val updatedList = buildList(currentList.size) {
                for (item in currentList) {
                    if (item.orderId != orderId) add(item)
                }
            }
            val updatedMap = if (updatedList.isEmpty()) {
                currentMap - walletKey
            } else {
                currentMap + (walletKey to updatedList)
            }
            prefs[PreferencesKeys.TANGEM_PAY_WITHDRAW_ORDERS_KEY] = adapter.toJson(updatedMap)
        }
    }

    override suspend fun storeHideOnboardingBanner(userWalletId: UserWalletId, hide: Boolean) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId), hide)
    }

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean {
        return appPreferencesStore.getSyncOrNull(
            key = PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId),
        ) == true
    }

    override suspend fun storeTangemPayEligibility(eligibility: Set<String>) {
        withContext(dispatcherProvider.io) {
            appPreferencesStore.store(
                key = PreferencesKeys.TANGEM_PAY_ELIGIBILITY_KEY,
                value = eligibility,
            )
        }
    }

    override suspend fun getTangemPayEligibility(): Set<String> {
        return withContext(dispatcherProvider.io) {
            appPreferencesStore.getSyncOrDefault(
                key = PreferencesKeys.TANGEM_PAY_ELIGIBILITY_KEY,
                default = emptySet(),
            )
        }
    }

    override suspend fun storeIsTangemPayDeactivated(userWalletId: UserWalletId) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayDeactivatedKey(userWalletId), true)
    }

    override suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean {
        val key = PreferencesKeys.getTangemPayDeactivatedKey(userWalletId)
        return appPreferencesStore.getSyncOrNull(key) == true
    }

    override suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String?) {
        if (customerWalletAddress != null) {
            withContext(dispatcherProvider.io) {
                secureStorage.delete(createAuthTokensKey(customerWalletAddress))
            }
        }

        // Keys are removed, not overwritten with empty values: `checkCustomerWalletResult` is a nullable
        // tri-state where `false` is a cached "this wallet has no Tangem Pay" that short-circuits the
        // network check, so writing it would keep the block hidden instead of forcing a re-check. Doing it
        // in a single transaction also keeps a crash from leaving half of the wallet's data behind.
        appPreferencesStore.editData { preferences ->
            if (customerWalletAddress != null) {
                preferences.remove(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress))
                preferences.remove(PreferencesKeys.getTangemPayVirtualAccountOrderIdKey(customerWalletAddress))
                preferences.remove(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress))
                preferences.remove(PreferencesKeys.getTangemPayCashbackDeactivationDismissedKey(customerWalletAddress))
            }
            preferences.remove(PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId))
            preferences.remove(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId))
            preferences.remove(PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId))
            // Both withdraw stores live inside a single preference holding a map, so the wallet's entry is
            // removed from it rather than the key itself.
            preferences.deleteActiveWithdrawOrderInternal(userWalletId)
            preferences.deleteWithdrawOrdersInternal(userWalletId)
        }
    }

    override suspend fun clearIsTangemPayDeactivated(userWalletId: UserWalletId) {
        appPreferencesStore.editData { it.remove(PreferencesKeys.getTangemPayDeactivatedKey(userWalletId)) }
    }

    private fun createAuthTokensKey(address: String): String = "${AUTH_TOKENS_DEFAULT_KEY}_$address"

    private fun createWithdrawOrderIdKey(userWalletId: UserWalletId): String = "${WITHDRAW_ORDER_ID_KEY}_$userWalletId"
}