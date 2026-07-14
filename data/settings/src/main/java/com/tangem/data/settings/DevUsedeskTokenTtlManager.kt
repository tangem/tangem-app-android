package com.tangem.data.settings

import androidx.datastore.preferences.core.longPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.settings.UsedeskTokenTtlManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Development implementation of [UsedeskTokenTtlManager].
 *
 * Reads and writes the Usedesk chat token TTL from [AppPreferencesStore],
 * allowing testers to override it via the Tester Menu.
 * The preference [kotlinx.coroutines.flow.Flow] is converted to a [StateFlow] on construction,
 * so [getTokenTtlMillisSync] can be called from non-suspending contexts.
 * Defaults to [UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS] when no value is stored.
 */
internal class DevUsedeskTokenTtlManager(
    private val appPreferencesStore: AppPreferencesStore,
    dispatchers: CoroutineDispatcherProvider,
) : UsedeskTokenTtlManager {

    private val ttlMillisState: StateFlow<Long> =
        appPreferencesStore
            .get(key = USEDESK_TOKEN_TTL_MILLIS_KEY, default = UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS)
            .stateIn(
                scope = CoroutineScope(dispatchers.io + SupervisorJob()),
                started = SharingStarted.Eagerly,
                initialValue = UsedeskTokenTtlManager.DEFAULT_TTL_MILLIS,
            )

    override fun getTokenTtlMillis(): StateFlow<Long> = ttlMillisState

    override fun getTokenTtlMillisSync(): Long = ttlMillisState.value

    override suspend fun setTokenTtlMillis(millis: Long) {
        appPreferencesStore.editData { preferences ->
            preferences[USEDESK_TOKEN_TTL_MILLIS_KEY] = millis
        }
    }

    private companion object {
        val USEDESK_TOKEN_TTL_MILLIS_KEY = longPreferencesKey(name = "usedeskTokenTtlMillis")
    }
}