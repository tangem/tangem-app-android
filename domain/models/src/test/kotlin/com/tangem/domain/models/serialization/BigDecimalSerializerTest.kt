package com.tangem.domain.models.serialization

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BigDecimalSerializerTest {

    private val json = Json

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json value WHEN decode THEN big decimal is parsed`(model: DecodeModel) {
        // Act
        val actual = json.decodeFromString(Holder.serializer(), """{"amount":${model.jsonValue}}""")

        // Assert
        assertThat(actual.amount).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN big decimal WHEN encode THEN it is written as string`() {
        // Act
        val actual = json.encodeToString(Holder.serializer(), Holder(amount = BigDecimal("12.50")))

        // Assert
        assertThat(actual).isEqualTo("""{"amount":"12.50"}""")
    }

    private fun provideTestModels() = listOf(
        DecodeModel(jsonValue = "\"12.50\"", expected = BigDecimal("12.50")),
        DecodeModel(jsonValue = "6", expected = BigDecimal("6")),
        DecodeModel(jsonValue = "12.5", expected = BigDecimal("12.5")),
        DecodeModel(jsonValue = "-0.01", expected = BigDecimal("-0.01")),
        DecodeModel(jsonValue = "1E+3", expected = BigDecimal("1E+3")),
    )

    internal data class DecodeModel(val jsonValue: String, val expected: BigDecimal) {
        override fun toString(): String = jsonValue
    }

    @Serializable
    private data class Holder(@SerialName("amount") val amount: SerializedBigDecimal)
}