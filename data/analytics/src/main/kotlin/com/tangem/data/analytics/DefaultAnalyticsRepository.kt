package com.tangem.data.analytics

import com.tangem.core.analytics.repository.AnalyticsRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync

internal class DefaultAnalyticsRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : AnalyticsRepository {

    override suspend fun checkIsEventSent(eventId: String): Boolean {
        val sentEvents = appPreferencesStore
            .getObjectListSync<String>(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY)

        return eventId in sentEvents
    }

    override suspend fun setIsEventSent(eventId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val sentEvents = mutablePreferences.getObject<List<String>>(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY)
            val updatedSentEvents = sentEvents.orEmpty() + eventId

            mutablePreferences.setObject(PreferencesKeys.SENT_ONE_TIME_EVENTS_KEY, updatedSentEvents)
        }
    }
}
