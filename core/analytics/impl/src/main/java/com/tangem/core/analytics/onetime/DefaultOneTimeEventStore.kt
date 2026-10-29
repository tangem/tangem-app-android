package com.tangem.core.analytics.onetime

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.core.analytics.api.OneTimeEventStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import javax.inject.Inject

internal class DefaultOneTimeEventStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
) : OneTimeEventStore {

    override suspend fun markSent(eventId: String): Boolean {
        var isAlreadySent = false

        appPreferencesStore.editData { preferences ->
            val sentEvents = preferences.getObjectList<String>(SENT_EVENTS_KEY).orEmpty()
            isAlreadySent = eventId in sentEvents

            if (!isAlreadySent) preferences.setObjectList(SENT_EVENTS_KEY, sentEvents + eventId)
        }

        return isAlreadySent
    }

    private companion object {
        val SENT_EVENTS_KEY = stringPreferencesKey(name = "sentOneTimeEvents")
    }
}