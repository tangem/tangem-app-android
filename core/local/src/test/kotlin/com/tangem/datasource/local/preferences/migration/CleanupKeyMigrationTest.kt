package com.tangem.datasource.local.preferences.migration

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CleanupKeyMigrationTest {

    private val key = stringSetPreferencesKey(name = "key")
    private val anotherKey = stringSetPreferencesKey(name = "anotherKey")

    private val migration = CleanupKeyMigration(key = key)

    @Test
    fun `GIVEN prefs contain key WHEN shouldMigrate THEN returns true`() = runTest {
        // GIVEN
        val prefs = mutablePreferencesOf(key to setOf("value"))

        // WHEN
        val actual = migration.shouldMigrate(prefs)

        // THEN
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN prefs contain key with empty value WHEN shouldMigrate THEN returns true`() = runTest {
        // GIVEN
        val prefs = mutablePreferencesOf(key to emptySet())

        // WHEN
        val actual = migration.shouldMigrate(prefs)

        // THEN
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN prefs do not contain key WHEN shouldMigrate THEN returns false`() = runTest {
        // GIVEN
        val prefs = mutablePreferencesOf(anotherKey to setOf("value"))

        // WHEN
        val actual = migration.shouldMigrate(prefs)

        // THEN
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN prefs contain key WHEN migrate THEN key is removed`() = runTest {
        // GIVEN
        val prefs = mutablePreferencesOf(key to setOf("value"))

        // WHEN
        val actual = migration.migrate(prefs)

        // THEN
        assertThat(actual.contains(key)).isFalse()
        assertThat(migration.shouldMigrate(actual)).isFalse()
    }

    @Test
    fun `GIVEN prefs contain other keys WHEN migrate THEN they are preserved`() = runTest {
        // GIVEN
        val actualKey = anotherKey
        val actualValue = setOf("value-1")
        val prefs = mutablePreferencesOf(key to setOf("another value"), actualKey to actualValue)

        // WHEN
        val actual = migration.migrate(prefs)

        // THEN
        assertThat(actual.asMap()).containsExactly(actualKey, actualValue)
    }

    @Test
    fun `GIVEN prefs do not contain key WHEN migrate THEN prefs are unchanged`() = runTest {
        // GIVEN
        val actualKey = anotherKey
        val actualValue = setOf("value")
        val prefs = mutablePreferencesOf(actualKey to actualValue)

        // WHEN
        val actual = migration.migrate(prefs)

        // THEN
        assertThat(actual.asMap()).containsExactly(actualKey, actualValue)
    }
}