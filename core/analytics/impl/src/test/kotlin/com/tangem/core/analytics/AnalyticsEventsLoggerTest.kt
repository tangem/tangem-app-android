package com.tangem.core.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnalyticsEventsLoggerTest {

    private val writer = RecordingLogWriter()
    private val logger = AnalyticsEventsLogger(name = "Amplitude")

    @BeforeEach
    fun setUp() {
        TangemLogger.setLogWriters(listOf(writer))
    }

    @AfterEach
    fun tearDown() {
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `GIVEN event with params WHEN logEvent THEN writes debug json with client, event and params`() {
        // Act
        logger.logEvent(event = "[Basic] Balance Loaded", params = mapOf("State" to "Full"))

        // Assert
        val record = writer.records.single()
        assertThat(record.severity).isEqualTo(Severity.Debug)
        assertThat(record.throwable).isNull()
        assertThat(record.json.toMap()).isEqualTo(
            mapOf(
                "client" to "Amplitude",
                "event" to "[Basic] Balance Loaded",
                "params" to mapOf("State" to "Full"),
            ),
        )
    }

    @Test
    fun `GIVEN exception with params WHEN logException THEN writes error json with the exception`() {
        // Arrange
        val error = IllegalStateException("boom")

        // Act
        logger.logException(error = error, params = mapOf("Screen" to "Main"))

        // Assert
        val record = writer.records.single()
        assertThat(record.severity).isEqualTo(Severity.Error)
        assertThat(record.throwable).isSameInstanceAs(error)
        assertThat(record.json.toMap()).isEqualTo(
            mapOf(
                "client" to "Amplitude",
                "event" to "error",
                "params" to mapOf("Screen" to "Main"),
            ),
        )
    }

    private fun JSONObject.toMap(): Map<String, Any> = keys().asSequence().associateWith { key ->
        when (val value = get(key)) {
            is JSONObject -> value.toMap()
            else -> value
        }
    }

    private class RecordingLogWriter : TangemLogger.LogWriter {

        val records = mutableListOf<Record>()

        override fun write(
            severity: Severity,
            tag: String,
            message: String,
            throwable: Throwable?,
            shouldSanitize: Boolean,
        ) {
            records += Record(severity = severity, json = JSONObject(message), throwable = throwable)
        }
    }

    private data class Record(val severity: Severity, val json: JSONObject, val throwable: Throwable?)
}