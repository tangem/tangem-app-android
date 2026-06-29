package com.tangem.data.pushnotificationpreferences

import arrow.core.Either
import com.tangem.data.pushnotificationpreferences.converters.PushNotificationPreferencesConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.PushNotificationPreferencesBody
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Preferences are cached in-memory (non-persistent); writes are full-replace PUTs and the server echo
 * is cached as the source of truth.
 */
internal class DefaultWalletPushNotificationPreferencesRepository(
    private val tangemTechApi: TangemTechApi,
    private val cache: RuntimeSharedStore<Map<String, WalletPushNotificationPreferences>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletPushNotificationPreferencesRepository {

    private val walletMutexes = ConcurrentHashMap<String, Mutex>()

    override suspend fun preload(userWalletId: UserWalletId) {
        if (isCached(userWalletId)) return
        mutexFor(userWalletId).withLock {
            if (isCached(userWalletId)) return
            val preferences = fetch(userWalletId)
            cache.update(default = emptyMap()) { current ->
                if (current.containsKey(userWalletId.stringValue)) {
                    current
                } else {
                    current + (userWalletId.stringValue to preferences)
                }
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
        mutexFor(userWalletId).withLock {
            val current = currentOrFetch(userWalletId)
            val updated = current.withCategory(category, isEnabled)
            putAndCommit(userWalletId, updated)
        }
    }

    override suspend fun setAllPreferences(
        userWalletId: UserWalletId,
        transactionAlerts: Boolean,
        offersUpdates: Boolean,
        priceAlerts: Boolean,
    ): Either<Throwable, Unit> = Either.catch {
        mutexFor(userWalletId).withLock {
            val current = currentOrFetch(userWalletId)
            val updated = current.copy(
                transactionAlerts = current.transactionAlerts.copy(isEnabled = transactionAlerts),
                offersUpdates = current.offersUpdates.copy(isEnabled = offersUpdates),
                priceAlerts = current.priceAlerts.copy(isEnabled = priceAlerts),
            )
            putAndCommit(userWalletId, updated)
        }
    }

    // Cache, or a freshly fetched server snapshot, so a full-replace PUT never carries fabricated defaults.
    private suspend fun currentOrFetch(userWalletId: UserWalletId): WalletPushNotificationPreferences =
        cache.getSyncOrNull()?.get(userWalletId.stringValue) ?: fetch(userWalletId)

    private suspend fun fetch(userWalletId: UserWalletId): WalletPushNotificationPreferences =
        withContext(dispatchers.io) {
            val response = tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue).getOrThrow()
            PushNotificationPreferencesConverter.convert(response)
        }

    private suspend fun isCached(userWalletId: UserWalletId): Boolean =
        cache.getSyncOrNull()?.containsKey(userWalletId.stringValue) == true

    private fun mutexFor(userWalletId: UserWalletId): Mutex =
        walletMutexes.computeIfAbsent(userWalletId.stringValue) { Mutex() }

    private suspend fun putAndCommit(userWalletId: UserWalletId, updated: WalletPushNotificationPreferences) {
        val applied = withContext(dispatchers.io) {
            val response = tangemTechApi.updatePushNotificationPreferences(
                walletId = userWalletId.stringValue,
                body = PushNotificationPreferencesBody(
                    areTransactionEventsEnabled = updated.transactionAlerts.isEnabled,
                    areOfferUpdatesEnabled = updated.offersUpdates.isEnabled,
                    arePriceAlertsEnabled = updated.priceAlerts.isEnabled,
                ),
            ).getOrThrow()
            PushNotificationPreferencesConverter.convert(response)
        }
        cache.update(default = emptyMap()) { it + (userWalletId.stringValue to applied) }
    }
}