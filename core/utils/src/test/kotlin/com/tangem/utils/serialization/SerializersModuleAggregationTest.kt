package com.tangem.utils.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.subclass
import org.junit.jupiter.api.Test

internal class SerializersModuleAggregationTest {

    private val json = Json {
        serializersModule = listOf(CircleModuleProvider, SquareModuleProvider).aggregate()
    }

    private val shapesSerializer = ListSerializer(PolymorphicSerializer(Shape::class))

    @Test
    fun `GIVEN providers from different modules WHEN encode THEN every subclass is written with its type`() {
        // Arrange
        val shapes: List<Shape> = listOf(Circle(radius = 2), Square(side = 3))

        // Act
        val actual = json.encodeToString(shapesSerializer, shapes)

        // Assert
        assertThat(actual).isEqualTo("""[{"type":"circle","radius":2},{"type":"square","side":3}]""")
    }

    @Test
    fun `GIVEN providers from different modules WHEN decode THEN every subclass is restored`() {
        // Arrange
        val encoded = """[{"type":"square","side":3},{"type":"circle","radius":2}]"""

        // Act
        val actual = json.decodeFromString(shapesSerializer, encoded)

        // Assert
        assertThat(actual).containsExactly(Square(side = 3), Circle(radius = 2)).inOrder()
    }

    @Test
    fun `GIVEN no providers WHEN aggregate THEN module is empty and subclass is unknown`() {
        // Arrange
        val emptyJson = Json { serializersModule = emptyList<SerializersModuleProvider>().aggregate() }

        // Act
        val error = runCatching { emptyJson.encodeToString(shapesSerializer, listOf(Circle(radius = 1))) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    private interface Shape

    @Serializable
    @SerialName("circle")
    private data class Circle(val radius: Int) : Shape

    @Serializable
    @SerialName("square")
    private data class Square(val side: Int) : Shape

    private object CircleModuleProvider : SerializersModuleProvider {
        override fun provide() = polymorphicSerializersModule<Shape> { subclass(Circle::class) }
    }

    private object SquareModuleProvider : SerializersModuleProvider {
        override fun provide() = polymorphicSerializersModule<Shape> { subclass(Square::class) }
    }
}