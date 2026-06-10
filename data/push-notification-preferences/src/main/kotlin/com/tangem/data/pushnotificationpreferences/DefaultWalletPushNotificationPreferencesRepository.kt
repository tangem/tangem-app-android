package com.tangem.data.pushnotificationpreferences

import arrow.core.Either
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * In-memory cache implementation of [WalletPushNotificationPreferencesRepository].
 *
 * Mock-mode (current): defaults are computed locally and writes are kept in-memory only.
 * Real-mode (when Variant C BE is ready): replace TODO blocks with [TangemTechApi] calls.
 *
 * Defaults for existing users (until BE migration runs): TX read from
 * [PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] (default true), Offers&Updates = true, Price Alerts = false,
 * isVisible = true for all three.
 */
internal class DefaultWalletPushNotificationPreferencesRepository(
    private val appPreferencesStore: AppPreferencesStore,
    @Suppress("unused") private val tangemTechApi: TangemTechApi,
    private val cache: RuntimeSharedStore<Map<String, WalletPushNotificationPreferences>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletPushNotificationPreferencesRepository {

    override suspend fun preload(userWalletId: UserWalletId) {
        if (cache.getSyncOrNull()?.containsKey(userWalletId.stringValue) == true) return
        val preferences = withContext(dispatchers.io) {
            // TODO: uncomment when api is ready
            //   val response = tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue).getOrThrow()
            //   PushNotificationPreferencesConverter.convert(response)
            loadDefaults(userWalletId)
        }
        cache.update(default = emptyMap()) { current ->
            if (current.containsKey(userWalletId.stringValue)) {
                current
            } else {
                current + (userWalletId.stringValue to preferences)
            }
        }
    }

    override fun observePreferences(userWalletId: UserWalletId): Flow<WalletPushNotificationPreferences> = cache.get()
        .onStart { preload(userWalletId) }
        .map { it[userWalletId.stringValue] }
        .filterNotNull()
        .distinctUntilChanged()

    override suspend fun updatePreference(
        userWalletId: UserWalletId,
        category: PushNotificationCategory,
        isEnabled: Boolean,
    ): Either<Throwable, Unit> = Either.catch {
        val current = cache.getSyncOrNull()?.get(userWalletId.stringValue) ?: loadDefaults(userWalletId)
        val updated = applyCategory(current, category, isEnabled)
        withContext(dispatchers.io) {
            // TODO: uncomment when api is ready
            //   tangemTechApi.updatePushNotificationPreferences(
            //       walletId = userWalletId.stringValue,
            //       body = PushNotificationPreferencesBody(
            //           areTransactionAlertsEnabled = updated.transactionAlerts.isEnabled,
            //           areOffersUpdatesEnabled = updated.offersUpdates.isEnabled,
            //           arePriceAlertsEnabled = updated.priceAlerts.isEnabled,
            //       ),
            //   ).getOrThrow()
        }
        cache.update(default = emptyMap()) { it + (userWalletId.stringValue to updated) }
    }

    private fun applyCategory(
        current: WalletPushNotificationPreferences,
        category: PushNotificationCategory,
        isEnabled: Boolean,
    ): WalletPushNotificationPreferences = when (category) {
        PushNotificationCategory.TransactionAlerts -> current.copy(
            transactionAlerts = current.transactionAlerts.copy(isEnabled = isEnabled),
        )
        PushNotificationCategory.OffersUpdates -> current.copy(
            offersUpdates = current.offersUpdates.copy(isEnabled = isEnabled),
        )
        PushNotificationCategory.PriceAlerts -> current.copy(
            priceAlerts = current.priceAlerts.copy(isEnabled = isEnabled),
        )
    }

    // TODO remove when api is ready, use api methods to load real settings
    private suspend fun loadDefaults(userWalletId: UserWalletId): WalletPushNotificationPreferences {
        val areTransactionAlertsEnabled = appPreferencesStore
            .getObjectMapSync<Boolean>(PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY)[userWalletId.stringValue] !=
            false
        return WalletPushNotificationPreferences(
            transactionAlerts = PushNotificationPreference(isEnabled = areTransactionAlertsEnabled, isVisible = true),
            offersUpdates = PushNotificationPreference(isEnabled = true, isVisible = true),
            priceAlerts = PushNotificationPreference(isEnabled = false, isVisible = true),
        )
    }
}