package com.tangem.domain.models.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class RawJsonSerializerTest {

    private val json = Json

    @Test
    fun `GIVEN nested json with an integer WHEN decoded THEN the text is kept with the literal intact`() {
        // Arrange
        val raw = """{"payload":$STRUCTURED_DATA,"hash":"0x1"}"""

        // Act
        val actual = json.decodeFromString(Holder.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(Holder(payload = STRUCTURED_DATA, hash = "0x1"))
    }

    @Test
    fun `GIVEN json with whitespace WHEN decoded THEN key order and values are kept in compact form`() {
        // Arrange
        val raw = """{ "payload": { "b" : 1 , "a" : [ 2 , "x" , null , true ] } , "hash": "0x1" }"""

        // Act
        val actual = json.decodeFromString(Holder.serializer(), raw)

        // Assert
        assertThat(actual.payload).isEqualTo("""{"b":1,"a":[2,"x",null,true]}""")
    }

    @Test
    fun `GIVEN a holder WHEN encoded THEN the payload is written as json, not as a string`() {
        // Arrange
        val holder = Holder(payload = STRUCTURED_DATA, hash = "0x1")

        // Act
        val encoded = json.encodeToString(Holder.serializer(), holder)

        // Assert
        assertThat(encoded).isEqualTo("""{"payload":$STRUCTURED_DATA,"hash":"0x1"}""")
    }

    @Test
    fun `GIVEN a holder WHEN encoded and decoded THEN the payload survives the round trip`() {
        // Arrange
        val holder = Holder(payload = STRUCTURED_DATA, hash = "0x1")

        // Act
        val restored = json.decodeFromString(Holder.serializer(), json.encodeToString(Holder.serializer(), holder))

        // Assert
        assertThat(restored).isEqualTo(holder)
    }

    @Serializable
    private data class Holder(
        @SerialName("payload") val payload: SerializedRawJson,
        @SerialName("hash") val hash: String,
    )

    private companion object {
        const val STRUCTURED_DATA = """{"domain":{"name":"Rain","chainId":137},"message":{"amount":"10000"}}"""
    }
}