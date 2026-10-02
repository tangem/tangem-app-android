package com.tangem.datasource.local.preferences.utils

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class AppPreferencesStoreJsonExtTest {

    private val prefs = createAppPreferencesStore(
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )

    private val json = Json
    private val serializer = MapSerializer(String.serializer(), Long.serializer())

    @Test
    fun `GIVEN no value WHEN getJsonSyncOrNull THEN null`() = runTest {
        // Act
        val actual = prefs.getJsonSyncOrNull(KEY, json, serializer)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN value set inside editData WHEN getJsonSyncOrNull THEN the value is restored`() = runTest {
        // Arrange
        val expected = mapOf("a" to 1L, "b" to 2L)
        prefs.editData { preferences ->
            preferences.setJson(key = KEY, json = json, serializer = serializer, value = expected)
        }

        // Act
        val actual = prefs.getJsonSyncOrNull(KEY, json, serializer)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN malformed value WHEN getJsonSyncOrNull THEN null instead of an exception`() = runTest {
        // Arrange
        prefs.editData { preferences -> preferences[KEY] = """["not-a-map"]""" }

        // Act
        val actual = prefs.getJsonSyncOrNull(KEY, json, serializer)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN malformed value WHEN getJsonOrNull inside editData THEN null and the key can be overwritten`() = runTest {
        // Arrange
        prefs.editData { preferences -> preferences[KEY] = "{" }
        val expected = mapOf("a" to 3L)

        // Act
        var current: Map<String, Long>? = null
        prefs.editData { preferences ->
            current = preferences.getJsonOrNull(KEY, json, serializer)
            preferences.setJson(key = KEY, json = json, serializer = serializer, value = current.orEmpty() + expected)
        }

        // Assert
        assertThat(current).isNull()
        assertThat(prefs.getJsonSyncOrNull(KEY, json, serializer)).isEqualTo(expected)
    }

    private companion object {
        val KEY = stringPreferencesKey("json_ext_test")
    }
}