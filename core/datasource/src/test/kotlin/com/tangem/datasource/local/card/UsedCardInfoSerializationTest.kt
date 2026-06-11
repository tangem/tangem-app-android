package com.tangem.datasource.local.card

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Tests Moshi serialization/deserialization of [UsedCardInfo].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UsedCardInfoSerializationTest {

    private val adapter = Moshi.Builder().build().adapter(UsedCardInfo::class.java)

    @Test
    fun `GIVEN full model WHEN toJson THEN all fields serialized in declaration order`() {
        // Arrange
        val model = UsedCardInfo(
            cardId = "card-1",
            isScanned = true,
            isActivationStarted = true,
            isActivationFinished = false,
            hasBackupError = true,
        )

        // Act
        val json = adapter.toJson(model)

        // Assert
        assertThat(json).isEqualTo(
            """{"cardId":"card-1","isScanned":true,"isActivationStarted":true,""" +
                """"isActivationFinished":false,"hasBackupError":true}""",
        )
    }

    @Test
    fun `GIVEN full json WHEN fromJson THEN model fully populated`() {
        // Arrange
        val json = """{"cardId":"card-2","isScanned":false,"isActivationStarted":true,""" +
            """"isActivationFinished":true,"hasBackupError":false}"""

        // Act
        val result = adapter.fromJson(json)

        // Assert
        assertThat(result).isEqualTo(
            UsedCardInfo(
                cardId = "card-2",
                isScanned = false,
                isActivationStarted = true,
                isActivationFinished = true,
                hasBackupError = false,
            ),
        )
    }

    @Test
    fun `GIVEN json with only cardId WHEN fromJson THEN boolean fields fall back to defaults`() {
        // Arrange
        val json = """{"cardId":"card-3"}"""

        // Act
        val result = adapter.fromJson(json)

        // Assert
        assertThat(result).isEqualTo(UsedCardInfo(cardId = "card-3"))
    }

    @Test
    fun `GIVEN json without cardId WHEN fromJson THEN throws`() {
        // Arrange
        val json = """{"isScanned":true}"""

        // Act
        val error = runCatching { adapter.fromJson(json) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(JsonDataException::class.java)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun roundTrip(model: UsedCardInfo) {
        // Act
        val restored = adapter.fromJson(adapter.toJson(model))

        // Assert
        assertThat(restored).isEqualTo(model)
    }

    private fun provideTestModels() = listOf(
        UsedCardInfo(cardId = "default-only"),
        UsedCardInfo(
            cardId = "all-true",
            isScanned = true,
            isActivationStarted = true,
            isActivationFinished = true,
            hasBackupError = true,
        ),
        UsedCardInfo(cardId = "activation-in-progress", isScanned = true, isActivationStarted = true),
        UsedCardInfo(cardId = "backup-error", hasBackupError = true),
    )
}