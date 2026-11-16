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
        writeLogFile()

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
        writeLogFile()

        // Act
        val archive = store.getZipFile(infoLogs = INFO_CONTENT)

        // Assert
        assertThat(archive?.parentFile?.listFiles()?.map(File::getName)).containsExactly(LOG_ZIP_FILE_NAME)
    }

    @Test
    fun `GIVEN archive with info WHEN logs-only archive requested THEN archive with info is kept intact`() = runTest {
        // Arrange
        writeLogFile()
        val archiveWithInfo = store.getZipFile(infoLogs = INFO_CONTENT)

        // Act
        val logsOnlyArchive = store.getZipFile()

        // Assert
        assertThat(logsOnlyArchive).isNotEqualTo(archiveWithInfo)
        assertThat(archiveWithInfo?.readEntries()).isEqualTo(
            mapOf(LOG_FILE_NAME to LOG_CONTENT, INFO_LOGS_FILE_NAME to INFO_CONTENT),
        )
        assertThat(archiveWithInfo?.name).isEqualTo(LOG_ZIP_FILE_NAME)
    }

    @Test
    fun `GIVEN log file and no info logs WHEN getZipFile THEN archive contains only log file`() = runTest {
        // Arrange
        writeLogFile()

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

    @Test
    fun `GIVEN log file in legacy location WHEN getZipFile THEN log is moved into logs directory`() = runTest {
        // Arrange
        File(filesDir, LOG_FILE_NAME).writeText(LOG_CONTENT)
        File(filesDir, LOG_ZIP_FILE_NAME).writeText(LOG_CONTENT)

        // Act
        val archive = store.getZipFile()

        // Assert
        assertThat(archive?.readEntries()).isEqualTo(mapOf(LOG_FILE_NAME to LOG_CONTENT))
        assertThat(filesDir.listFiles()?.map(File::getName)).containsExactly(LOGS_DIR_NAME)
    }

    @Test
    fun `GIVEN log files in legacy and logs directories WHEN getZipFile THEN logs directory file is kept`() = runTest {
        // Arrange
        writeLogFile()
        File(filesDir, LOG_FILE_NAME).writeText(LEGACY_LOG_CONTENT)

        // Act
        val archive = store.getZipFile()

        // Assert
        assertThat(archive?.readEntries()).isEqualTo(mapOf(LOG_FILE_NAME to LOG_CONTENT))
        assertThat(File(filesDir, LOG_FILE_NAME).exists()).isFalse()
    }

    @Test
    fun `GIVEN legacy log file that cannot be moved WHEN getZipFile THEN legacy log file is kept`() = runTest {
        // Arrange
        File(filesDir, LOG_FILE_NAME).writeText(LEGACY_LOG_CONTENT)
        File(filesDir, LOGS_DIR_NAME).writeText("")

        // Act
        store.getZipFile()

        // Assert
        assertThat(File(filesDir, LOG_FILE_NAME).readText()).isEqualTo(LEGACY_LOG_CONTENT)
    }

    @Test
    fun `GIVEN log file WHEN getZipFile THEN archive is created inside logs directory`() = runTest {
        // Arrange
        writeLogFile()

        // Act
        val archive = store.getZipFile()

        // Assert
        assertThat(archive?.parentFile).isEqualTo(File(filesDir, LOGS_DIR_NAME))
    }

    private fun writeLogFile() {
        File(filesDir, LOGS_DIR_NAME).mkdirs()
        File(filesDir, "$LOGS_DIR_NAME/$LOG_FILE_NAME").writeText(LOG_CONTENT)
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
        const val LOG_ZIP_FILE_NAME = "log.zip"
        const val LOGS_DIR_NAME = "logs"
        const val INFO_LOGS_FILE_NAME = "infoLogs.txt"
        const val LOG_CONTENT = "12.09 10:00:00.000: Tag scan started"
        const val LEGACY_LOG_CONTENT = "11.09 09:00:00.000: Tag legacy entry"
        const val INFO_CONTENT = "Phone model: Pixel 9"
    }
}