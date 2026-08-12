package com.tangem.datasource.utils

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@JsonClass(generateAdapter = true)
data class SampleDto(val id: String)

@OptIn(ExperimentalStdlibApi::class)
internal class AppDataStoreFactoryTest {

    @TempDir
    lateinit var tempDir: File

    private val moshi = Moshi.Builder().build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val reported = mutableListOf<ExceptionAnalyticsEvent>()
    private val analyticsExceptionHandler = object : AnalyticsExceptionHandler {
        override fun sendException(event: ExceptionAnalyticsEvent) {
            reported += event
        }
    }
    private val factory = AppDataStoreFactory(analyticsExceptionHandler)

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `GIVEN corrupted file WHEN store is read THEN returns default and reports corruption`() = runBlocking {
        // Arrange
        val file = File(tempDir, "corrupted_list").apply { writeText("}{ definitely not json") }
        val store = factory.create(
            serializer = MoshiDataStoreSerializer(moshi, listTypes<String>(), defaultValue = listOf("default")),
            scope = scope,
            produceFile = { file },
        )

        // Act
        val value = store.data.first()

        // Assert
        assertThat(value).isEqualTo(listOf("default"))
        assertThat(reported).hasSize(1)
        val event = reported.single()
        assertThat(event.exception).isInstanceOf(DataStoreCorruptionException::class.java)
        assertThat(event.params["file"]).isEqualTo("corrupted_list")
        assertThat(event.params["kind"]).isEqualTo("non_json")
    }

    @Test
    fun `GIVEN corrupted file with null default WHEN store is read THEN returns null without crashing`() = runBlocking {
        // Arrange — mirrors AccountsResponseStore, whose default value is null
        val file = File(tempDir, "corrupted_nullable").apply { writeText("  garbage bytes") }
        val store = factory.create(
            serializer = MoshiDataStoreSerializer(defaultValue = null, adapter = moshi.adapter<SampleDto?>()),
            scope = scope,
            produceFile = { file },
        )

        // Act
        val value = store.data.first()

        // Assert
        assertThat(value).isNull()
        assertThat(reported).hasSize(1)
    }

    @Test
    fun `GIVEN valid file WHEN store is read THEN returns parsed value and does not report`() = runBlocking {
        // Arrange
        val file = File(tempDir, "valid_list").apply { writeText("""["a","b"]""") }
        val store = factory.create(
            serializer = MoshiDataStoreSerializer(moshi, listTypes<String>(), defaultValue = emptyList<String>()),
            scope = scope,
            produceFile = { file },
        )

        // Act
        val value = store.data.first()

        // Assert
        assertThat(value).containsExactly("a", "b").inOrder()
        assertThat(reported).isEmpty()
    }
}