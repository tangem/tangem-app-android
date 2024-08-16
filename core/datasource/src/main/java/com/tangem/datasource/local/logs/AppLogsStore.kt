package com.tangem.datasource.local.logs

import androidx.datastore.preferences.core.MutablePreferences
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import javax.inject.Inject

/**
 * Store for saving app logs
 *
 * @property appPreferencesStore app preferences store
 * @param dispatchers         coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
class AppLogsStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    dispatchers: CoroutineDispatcherProvider,
) {

    private val scope = CoroutineScope(dispatchers.io)
    private val mutex = Mutex()

    /** Save log [message] */
    fun saveLogMessage(message: String) {
        val newLogs = DateTime.now().millis.toString() to message

        appPreferencesStore.editDataWithLock { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            preferences.setObjectMap(key = PreferencesKeys.APP_LOGS_KEY, value = savedLogs + newLogs)
        }
    }

    /** Delete deprecated logs if file size exceeds [maxSize] */
    fun deleteDeprecatedLogs(maxSize: Int) {
        appPreferencesStore.editDataWithLock { preferences ->
            val savedLogs = preferences.getObjectMap<String>(PreferencesKeys.APP_LOGS_KEY)

            var sum = 0
            preferences.setObjectMap(
                key = PreferencesKeys.APP_LOGS_KEY,
                value = savedLogs.entries
                    .sortedBy(Map.Entry<String, String>::key)
                    .takeLastWhile {
                        sum += it.value.length
                        sum < maxSize
                    }
                    .associate { it.key to it.value },
            )
        }
    }

    private fun AppPreferencesStore.editDataWithLock(
        transform: suspend AppPreferencesStore.(MutablePreferences) -> Unit,
    ) {
        scope.launch {
            mutex.withLock {
                editData(transform)
            }
        }
    }
}