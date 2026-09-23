package com.tangem.datasource.local.logs

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

internal class DefaultAppLogsStoreTest {

    @TempDir
    lateinit var filesDir: File

    private val store by lazy {
        DefaultAppLogsStore(
            applicationContext = mockk<Context> { every { filesDir } returns this@DefaultAppLogsStoreTest.filesDir },
            dispatchers = TestingCoroutineDispatcherProvider(),
            scope = TestAppCoroutineScope(),
        )
    }

    @Test
    fun `GIVEN log file and info logs WHEN getZipFile THEN archive contains both files`() = runTest {
        // Arrange
        File(filesDir, LOG_FILE_NAME).writeText(LOG_CONTENT)

        // Act
        val archive = store.getZipFile(infoLogs = INFO_CONTENT)

        // Assert
        assertThat(archive?.readEntries()).isEqualTo(
            mapOf(LOG_FILE_NAME to LOG_CONTENT, INFO_LOGS_FILE_NAME to INFO_CONTENT),
        )
    }

    @Test
    fun `GIVEN info logs WHEN getZipFile THEN temporary info file is removed`() = runTest {
        // Arrange
        File(filesDir, LOG_FILE_NAME).writeText(LOG_CONTENT)

        // Act
        store.getZipFile(infoLogs = INFO_CONTENT)

        // Assert
        assertThat(File(filesDir, INFO_LOGS_FILE_NAME).exists()).isFalse()
    }

    @Test
    fun `GIVEN log file and no info logs WHEN getZipFile THEN archive contains only log file`() = runTest {
        // Arrange
        File(filesDir, LOG_FILE_NAME).writeText(LOG_CONTENT)

        // Act
        val archive = store.getZipFile()

        // Assert
        assertThat(archive?.readEntries()).isEqualTo(mapOf(LOG_FILE_NAME to LOG_CONTENT))
    }

    @Test
    fun `GIVEN no log file and info logs WHEN getZipFile THEN archive contains only info logs`() = runTest {
        // Act
        val archive = store.getZipFile(infoLogs = INFO_CONTENT)

        // Assert
        assertThat(archive?.readEntries()).isEqualTo(mapOf(INFO_LOGS_FILE_NAME to INFO_CONTENT))
    }

    @Test
    fun `GIVEN no log file and no info logs WHEN getZipFile THEN returns null`() = runTest {
        assertThat(store.getZipFile()).isNull()
    }

    private fun File.readEntries(): Map<String, String> {
        return ZipFile(this).use { zip ->
            zip.entries().asSequence().associate { entry ->
                entry.name to zip.getInputStream(entry).bufferedReader().use { it.readText() }
            }
        }
    }

    private companion object {
        const val LOG_FILE_NAME = "log.txt"
        const val INFO_LOGS_FILE_NAME = "infoLogs.txt"
        const val LOG_CONTENT = "12.09 10:00:00.000: Tag scan started"
        const val INFO_CONTENT = "Phone model: Pixel 9"
    }
}