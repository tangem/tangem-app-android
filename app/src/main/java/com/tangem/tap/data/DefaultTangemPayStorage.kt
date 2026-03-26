package com.tangem.tap.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.data.pay.entity.WithdrawStoreData
import com.tangem.data.pay.util.WithdrawStateConverter
import com.tangem.data.pay.util.WithdrawStoreDataConverter
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val WITHDRAW_ORDER_ID_KEY = "tangem_pay_withdraw_order_id_key"

@Suppress("TooManyFunctions")
@Singleton
internal class DefaultTangemPayStorage @Inject constructor(
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
) : TangemPayStorage {

    private val withdrawStoreDataConverter by lazy { WithdrawStoreDataConverter() }
    private val withdrawStateConverter by lazy { WithdrawStateConverter() }

    private val listType by lazy {
        Types.newParameterizedType(List::class.java, WithdrawStoreData::class.java)
    }
    private val mapType by lazy {
        Types.newParameterizedType(Map::class.java, String::class.java, listType)
    }
    private val adapter: JsonAdapter<Map<String, List<WithdrawStoreData>>> by lazy {
        appPreferencesStore.moshi.adapter(mapType)
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

    override suspend fun clearOrderId(customerWalletAddress: String) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), "")
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
        appPreferencesStore.editData { mutablePreferences ->
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

    override suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String) {
        appPreferencesStore.store(PreferencesKeys.getTangemPayCheckCustomerByWalletId(userWalletId), false)
        appPreferencesStore.store(PreferencesKeys.getTangemPayCustomerWalletAddressKey(userWalletId), "")
        appPreferencesStore.store(PreferencesKeys.getTangemPayOrderIdKey(customerWalletAddress), "")
        appPreferencesStore.store(PreferencesKeys.getTangemPayAddToWalletKey(customerWalletAddress), false)
        appPreferencesStore.store(PreferencesKeys.getTangemPayHideOnboardingKey(userWalletId), false)
    }

    private fun createWithdrawOrderIdKey(userWalletId: UserWalletId): String = "${WITHDRAW_ORDER_ID_KEY}_$userWalletId"
}