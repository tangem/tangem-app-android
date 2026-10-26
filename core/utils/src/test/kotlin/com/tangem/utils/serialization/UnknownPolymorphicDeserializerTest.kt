package com.tangem.utils.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test

internal class UnknownPolymorphicDeserializerTest {

    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Event::class) { subclass(Created::class) }
            polymorphicUnknownFallback<Event> { type, payload -> UnknownEvent(type = type, payload = payload) }
        }
    }

    private val eventsSerializer = ListSerializer(PolymorphicSerializer(Event::class))

    private val sealedJson = Json {
        serializersModule = SerializersModule {
            polymorphicUnknownFallback<Shape> { type, _ -> Shape.Unknown(type = type) }
        }
    }

    private val shapesSerializer = ListSerializer(Shape.serializer())

    @Test
    fun `GIVEN known and unknown types WHEN decode THEN the unknown one keeps its type and payload`() {
        // Arrange
        val encoded = """[{"type":"created","id":"1"},{"type":"deleted","id":"2","reason":"gone"}]"""

        // Act
        val actual = json.decodeFromString(eventsSerializer, encoded)

        // Assert
        assertThat(actual).containsExactly(
            Created(id = "1"),
            UnknownEvent(type = "deleted", payload = parse("""{"type":"deleted","id":"2","reason":"gone"}""")),
        ).inOrder()
    }

    @Test
    fun `GIVEN open base and registered type in another case WHEN decode THEN the subclass is matched ignoring case`() {
        // Act
        val actual = json.decodeFromString(eventsSerializer, """[{"type":"CREATED","id":"1"}]""")

        // Assert
        assertThat(actual).containsExactly(Created(id = "1"))
    }

    @Test
    fun `GIVEN item without type WHEN decode THEN the fallback receives a null type`() {
        // Arrange
        val encoded = """[{"id":"3"}]"""

        // Act
        val actual = json.decodeFromString(eventsSerializer, encoded)

        // Assert
        assertThat(actual).containsExactly(UnknownEvent(type = null, payload = parse("""{"id":"3"}""")))
    }

    @Test
    fun `GIVEN sealed base with fallback WHEN decode THEN unknown subtype is resolved as well`() {
        // Act
        val actual = sealedJson.decodeFromString(
            shapesSerializer,
            """[{"type":"circle","radius":1},{"type":"hexagon"}]""",
        )

        // Assert
        assertThat(actual).containsExactly(Shape.Circle(radius = 1), Shape.Unknown(type = "hexagon")).inOrder()
    }

    @Test
    fun `GIVEN sealed base and known type in another case WHEN decode THEN the subtype is matched ignoring case`() {
        // Act
        val actual = sealedJson.decodeFromString(shapesSerializer, """[{"type":"CIRCLE","radius":1}]""")

        // Assert
        assertThat(actual).containsExactly(Shape.Circle(radius = 1))
    }

    @Test
    fun `GIVEN known type in another case and payload it cannot decode WHEN decode THEN it falls back to Unknown`() {
        // Act
        val actual = sealedJson.decodeFromString(shapesSerializer, """[{"type":"Circle"}]""")

        // Assert
        assertThat(actual).containsExactly(Shape.Unknown(type = "Circle"))
    }

    private fun parse(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject

    private interface Event

    @Serializable
    @SerialName("created")
    private data class Created(val id: String) : Event

    private data class UnknownEvent(val type: String?, val payload: JsonObject) : Event

    @Serializable
    private sealed interface Shape {

        @Serializable
        @SerialName("circle")
        data class Circle(val radius: Int) : Shape

        @Serializable
        data class Unknown(val type: String?) : Shape
    }
}