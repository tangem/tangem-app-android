package com.tangem.core.local.datastore

import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class DeleteObsoleteFileMigrationTest {

    @TempDir
    lateinit var tempDir: File

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `GIVEN obsolete file exists WHEN store is first read THEN the file is deleted and default is returned`() =
        runBlocking {
            // Arrange
            val obsolete = File(tempDir, "statuses").apply { writeText("""{"legacy":"blob"}""") }
            val store = createStore(obsolete)

            // Act
            val actual = store.data.first()

            // Assert
            assertThat(actual).isEmpty()
            assertThat(obsolete.exists()).isFalse()
        }

    @Test
    fun `GIVEN obsolete file exists WHEN store is written first THEN the written value survives`() = runBlocking {
        // Arrange
        val obsolete = File(tempDir, "statuses").apply { writeText("{}") }
        val store = createStore(obsolete)

        // Act
        store.updateData { mapOf("wallet" to "status") }
        val actual = store.data.first()

        // Assert
        assertThat(actual).containsExactly("wallet", "status")
        assertThat(obsolete.exists()).isFalse()
    }

    @Test
    fun `GIVEN no obsolete file WHEN store is read THEN nothing fails`() = runBlocking {
        // Arrange
        val obsolete = File(tempDir, "statuses")
        val store = createStore(obsolete)

        // Act
        val actual = store.data.first()

        // Assert
        assertThat(actual).isEmpty()
        assertThat(obsolete.exists()).isFalse()
    }

    private fun createStore(obsolete: File) = DataStoreFactory.create(
        serializer = KotlinxDataStoreSerializer(
            defaultValue = emptyMap(),
            serializer = MapSerializer(String.serializer(), String.serializer()),
        ),
        migrations = listOf(DeleteObsoleteFileMigration { obsolete }),
        scope = scope,
        produceFile = { File(tempDir, "statuses_v2") },
    )
}