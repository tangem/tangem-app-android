package com.tangem.domain.models.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test

internal class IsoDateTimeSerializerTest {

    private val json = Json

    @Test
    fun `GIVEN iso string WHEN decode THEN date time is parsed`() {
        // Act
        val actual = json.decodeFromString(Holder.serializer(), """{"date":"2026-01-02T03:04:05.006Z"}""")

        // Assert
        assertThat(actual.date.millis).isEqualTo(DateTime(2026, 1, 2, 3, 4, 5, 6, DateTimeZone.UTC).millis)
    }

    @Test
    fun `GIVEN date time WHEN encode THEN iso string is written`() {
        // Arrange
        val date = DateTime(2026, 1, 2, 3, 4, 5, 6, DateTimeZone.UTC)

        // Act
        val actual = json.encodeToString(Holder.serializer(), Holder(date = date))

        // Assert
        assertThat(actual).isEqualTo("""{"date":"2026-01-02T03:04:05.006Z"}""")
    }

    @Serializable
    private data class Holder(@SerialName("date") val date: SerializedIsoDateTime)
}