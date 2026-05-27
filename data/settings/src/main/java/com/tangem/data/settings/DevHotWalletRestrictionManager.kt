package com.tangem.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Development implementation of [HotWalletRestrictionManager].
 *
 * Reads and writes the restriction state from [AppPreferencesStore],
 * allowing testers to toggle it via the Tester Menu.
 * The preference [Flow] is converted to a [StateFlow] on construction,
 * so [isCreationEnabledSync] can be called from non-suspending contexts.
 * Defaults to `true` (restriction enabled) when no value is stored.
 */
internal class DevHotWalletRestrictionManager(
    private val appPreferencesStore: AppPreferencesStore,
    dispatchers: CoroutineDispatcherProvider,
) : HotWalletRestrictionManager {

    private val isCreationEnabledState: StateFlow<Boolean> =
        appPreferencesStore
            .get(key = IS_HOT_WALLET_CREATION_RESTRICTION_ENABLED_KEY, default = true)
            .stateIn(
                scope = CoroutineScope(dispatchers.io + SupervisorJob()),
                started = SharingStarted.Eagerly,
                initialValue = true,
            )

    override fun isCreationEnabled(): StateFlow<Boolean> = isCreationEnabledState

    override fun isCreationEnabledSync(): Boolean = isCreationEnabledState.value

    override suspend fun toggleCreationEnabled() {
        appPreferencesStore.editData { preferences ->
            val isEnabled = preferences.getOrDefault(
                key = IS_HOT_WALLET_CREATION_RESTRICTION_ENABLED_KEY,
                default = true,
            )
            preferences[IS_HOT_WALLET_CREATION_RESTRICTION_ENABLED_KEY] = !isEnabled
        }
    }

    private companion object {
        val IS_HOT_WALLET_CREATION_RESTRICTION_ENABLED_KEY =
            booleanPreferencesKey(name = "isHotWalletCreationRestrictionEnabled")
    }
}