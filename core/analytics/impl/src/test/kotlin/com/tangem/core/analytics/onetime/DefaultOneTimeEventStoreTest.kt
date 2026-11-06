package com.tangem.core.analytics.onetime

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultOneTimeEventStoreTest {

    @Test
    fun `GIVEN no sent events WHEN markSent THEN returns false and stores the event`() = runTest {
        // Arrange
        val dataStore = MockStateDataStore<Preferences>(default = emptyPreferences())
        val store = createStore(dataStore)

        // Act
        val actual = store.markSent(eventId = "first")

        // Assert
        assertThat(actual).isFalse()
        assertThat(dataStore.data.first()[SENT_EVENTS_KEY]).isEqualTo("""["first"]""")
    }

    @Test
    fun `GIVEN event stored in the existing format WHEN markSent THEN returns true and keeps the stored value`() =
        runTest {
            // Arrange
            val dataStore = MockStateDataStore(default = preferencesOf(SENT_EVENTS_KEY to STORED_EVENTS))
            val store = createStore(dataStore)

            // Act
            val actual = store.markSent(eventId = "second")

            // Assert
            assertThat(actual).isTrue()
            assertThat(dataStore.data.first()[SENT_EVENTS_KEY]).isEqualTo(STORED_EVENTS)
        }

    @Test
    fun `GIVEN other events stored WHEN markSent with a new event THEN returns false and appends it`() = runTest {
        // Arrange
        val dataStore = MockStateDataStore(default = preferencesOf(SENT_EVENTS_KEY to STORED_EVENTS))
        val store = createStore(dataStore)

        // Act
        val actual = store.markSent(eventId = "third")

        // Assert
        assertThat(actual).isFalse()
        assertThat(dataStore.data.first()[SENT_EVENTS_KEY]).isEqualTo("""["first","second","third"]""")
    }

    private fun createStore(dataStore: MockStateDataStore<Preferences>) = DefaultOneTimeEventStore(
        appPreferencesStore = createAppPreferencesStore(
            dispatchers = TestingCoroutineDispatcherProvider(),
            preferencesDataStore = dataStore,
        ),
    )

    private companion object {
        val SENT_EVENTS_KEY = stringPreferencesKey(name = "sentOneTimeEvents")
        const val STORED_EVENTS = """["first","second"]"""
    }
}