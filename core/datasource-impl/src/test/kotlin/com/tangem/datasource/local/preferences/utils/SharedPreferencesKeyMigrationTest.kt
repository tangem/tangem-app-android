package com.tangem.datasource.local.preferences.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class SharedPreferencesKeyMigrationTest {

    @TempDir
    lateinit var appDataDir: File

    private val reported = mutableListOf<ExceptionAnalyticsEvent>()
    private val analyticsExceptionHandler = object : AnalyticsExceptionHandler {
        override fun sendException(event: ExceptionAnalyticsEvent) {
            reported += event
        }
    }

    private val editor: SharedPreferences.Editor = mockk()
    private val legacyPrefs: SharedPreferences = mockk()
    private val context: Context = mockk()

    private val migration = SharedPreferencesKeyMigration(
        context = context,
        legacyPrefsName = LEGACY_PREFS_NAME,
        legacyKeyName = LEGACY_KEY_NAME,
        keyName = "new_key",
        analyticsExceptionHandler = analyticsExceptionHandler,
    )

    @BeforeEach
    fun setUp() {
        reported.clear()

        every { context.applicationInfo } returns mockk<ApplicationInfo>().apply { dataDir = appDataDir.path }
        every { context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE) } returns legacyPrefs
        every { legacyPrefs.edit() } returns editor
        every { editor.remove(LEGACY_KEY_NAME) } returns editor
    }

    @Test
    fun `GIVEN commit fails WHEN cleanUp THEN reports non-fatal and keeps legacy prefs file`() = runBlocking {
        // Arrange
        val prefsFile = createLegacyPrefsFile()
        every { editor.commit() } returns false
        every { legacyPrefs.all } returns mapOf(LEGACY_KEY_NAME to "value")

        // Act
        migration.cleanUp()

        // Assert
        val event = reported.single()
        assertThat(event.exception).isInstanceOf(LegacyPreferencesCleanUpException::class.java)
        assertThat(event.params.getValue("usable_space").toLong()).isGreaterThan(0L)
        assertThat(event.params - "usable_space").containsExactlyEntriesIn(
            mapOf(
                "prefs" to LEGACY_PREFS_NAME,
                "key" to LEGACY_KEY_NAME,
                "keys_left" to "1",
            ),
        )
        assertThat(prefsFile.exists()).isTrue()
    }

    @Test
    fun `GIVEN commit succeeds and no keys left WHEN cleanUp THEN deletes legacy prefs file without reporting`() =
        runBlocking {
            // Arrange
            val prefsFile = createLegacyPrefsFile()
            every { editor.commit() } returns true
            every { legacyPrefs.all } returns emptyMap()

            // Act
            migration.cleanUp()

            // Assert
            assertThat(reported).isEmpty()
            assertThat(prefsFile.exists()).isFalse()
        }

    private fun createLegacyPrefsFile(): File {
        val prefsDir = File(appDataDir, "shared_prefs").apply { mkdirs() }
        return File(prefsDir, "$LEGACY_PREFS_NAME.xml").apply { writeText("<map/>") }
    }

    private companion object {
        const val LEGACY_PREFS_NAME = "app_theme"
        const val LEGACY_KEY_NAME = "key"
    }
}