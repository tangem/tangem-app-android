package com.tangem.core.remote.json

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test

internal class NetworkJsonFactoryTest {

    private val json = NetworkJsonFactory.create(
        serializersModule = SerializersModule {
            polymorphic(Event::class) { subclass(Created::class) }
        },
    )

    @Test
    fun `GIVEN nullable field is null WHEN encode THEN key is omitted and defaults are written`() {
        // Act
        val actual = json.encodeToString(Request.serializer(), Request(name = "x", note = null))

        // Assert
        assertThat(actual).isEqualTo("""{"name":"x","kind":"default"}""")
    }

    @Test
    fun `GIVEN unknown key and missing nullable field WHEN decode THEN both are tolerated`() {
        // Act
        val actual = json.decodeFromString(Request.serializer(), """{"name":"x","kind":"k","extra":1}""")

        // Assert
        assertThat(actual).isEqualTo(Request(name = "x", note = null, kind = "k"))
    }

    @Test
    fun `GIVEN number for a string field WHEN decode THEN the literal is read as string`() {
        // Act
        val actual = json.decodeFromString(Request.serializer(), """{"name":5.50,"kind":12}""")

        // Assert
        assertThat(actual).isEqualTo(Request(name = "5.50", note = null, kind = "12"))
    }

    @Test
    fun `GIVEN quoted number for a numeric field WHEN decode THEN the number is parsed`() {
        // Act
        val actual = json.decodeFromString(Numbers.serializer(), """{"count":"7","chain_id":"137"}""")

        // Assert
        assertThat(actual).isEqualTo(Numbers(count = 7, chainId = 137L))
    }

    @Test
    fun `GIVEN null for a non-null string field WHEN decode THEN it fails`() {
        // Act
        val error = runCatching { json.decodeFromString(Request.serializer(), """{"name":null}""") }
            .exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    @Test
    fun `GIVEN unknown enum value for a field without default WHEN decode THEN it fails`() {
        // Act
        val error = runCatching { json.decodeFromString(WithStatus.serializer(), """{"status":"BOGUS"}""") }
            .exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    @Test
    fun `GIVEN unknown enum value for a field with default WHEN decode THEN the default is used`() {
        // Act
        val actual = json.decodeFromString(WithFallbackStatus.serializer(), """{"status":"BOGUS"}""")

        // Assert
        assertThat(actual).isEqualTo(WithFallbackStatus(status = FallbackStatus.UNKNOWN))
    }

    @Test
    fun `GIVEN known enum value in another case WHEN decode THEN the entry is matched`() {
        // Act
        val actual = json.decodeFromString(WithFallbackStatus.serializer(), """{"status":"new"}""")

        // Assert
        assertThat(actual).isEqualTo(WithFallbackStatus(status = FallbackStatus.NEW))
    }

    @Test
    fun `GIVEN null for a non-null field with default WHEN decode THEN the default is used`() {
        // Act
        val actual = json.decodeFromString(Request.serializer(), """{"name":"x","kind":null}""")

        // Assert
        assertThat(actual).isEqualTo(Request(name = "x", note = null, kind = "default"))
    }

    @Test
    fun `GIVEN registered module WHEN decode polymorphic interface THEN subclass is resolved by type`() {
        // Act
        val actual = json.decodeFromString(PolymorphicSerializer(Event::class), """{"type":"created","id":"1"}""")

        // Assert
        assertThat(actual).isEqualTo(Created(id = "1"))
    }

    @Serializable
    private data class Request(
        @SerialName("name") val name: String,
        @SerialName("note") val note: String?,
        @SerialName("kind") val kind: String = "default",
    )

    @Serializable
    private data class Numbers(
        @SerialName("count") val count: Int,
        @SerialName("chain_id") val chainId: Long,
    )

    @Serializable
    private data class WithStatus(@SerialName("status") val status: Status)

    @Serializable
    private enum class Status {
        @SerialName("NEW")
        NEW,
    }

    @Serializable
    private data class WithFallbackStatus(
        @SerialName("status") val status: FallbackStatus = FallbackStatus.UNKNOWN,
    )

    @Serializable
    private enum class FallbackStatus {
        @SerialName("NEW")
        NEW,

        UNKNOWN,
    }

    private interface Event

    @Serializable
    @SerialName("created")
    private data class Created(@SerialName("id") val id: String) : Event
}