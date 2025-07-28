package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.account.CryptoPortfolioIcon.*
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoPortfolioIconTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfMainAccount {

        @Test
        fun `ofMainAccount with empty exclude`() {
            // Act
            val actual = CryptoPortfolioIcon.ofMainAccount(exclude = emptySet())

            // Assert
            val expectedColor = Color.Azure
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedType = Type.Icon(value = Icon.Star)
            Truth.assertThat(actual.type).isEqualTo(expectedType)
        }

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofMainAccount(model: OfMainAccountModel) {
            // Arrange
            mockkObject(Random.Default)

            val size = (Color.entries.size - model.exclude.size).takeIf { it > 0 } ?: Color.entries.size
            every { Random.nextInt(size) } returns model.randomNextInt

            // Act
            val actual = CryptoPortfolioIcon.ofMainAccount(exclude = model.exclude)

            // Assert
            val expectedColor = model.expectedColor
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedType = Type.Icon(value = Icon.Star)
            Truth.assertThat(actual.type).isEqualTo(expectedType)

            verify(exactly = 1) { Random.nextInt(size) }

            unmockkObject(Random.Default)
        }

        private fun provideTestModels() = listOf(
            // If the default color is already occupied (present in the exclude set), a random color from the
            // remaining available colors will be selected for the main account icon.
            OfMainAccountModel(
                exclude = setOf(Color.Azure),
                randomNextInt = 0,
                expectedColor = Color.entries[1],
            ),
            OfMainAccountModel(
                exclude = setOf(Color.Azure, Color.CaribbeanBlue),
                randomNextInt = 0,
                expectedColor = Color.entries[2],
            ),
            // If all colors are already occupied, a random one will be selected.
            OfMainAccountModel(
                exclude = Color.entries.toSet(),
                randomNextInt = 1,
                expectedColor = Color.entries[1],
            ),
        )
    }

    data class OfMainAccountModel(
        val exclude: Set<Color>,
        val randomNextInt: Int,
        val expectedColor: Color,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfCustomAccountBasedOnName {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofCustomAccount(model: OfCustomAccountModel.BasedOnName) {
            // Arrange
            mockkObject(Random.Default)

            every { Random.nextInt(until = Color.entries.size) } returns model.randomNextInt

            // Act
            val actual = CryptoPortfolioIcon.ofCustomAccount(accountName = model.accountName)

            // Assert
            val expectedColor = model.expectedColor
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedType = Type.Symbol(value = model.accountName.first())
            Truth.assertThat(actual.type).isEqualTo(expectedType)

            verify(exactly = 1) { Random.nextInt(until = Color.entries.size) }

            unmockkObject(Random.Default)
        }

        private fun provideTestModels() = listOf(
            OfCustomAccountModel.BasedOnName(
                accountName = "New account",
                randomNextInt = 0,
                expectedColor = Color.entries[0],
            ),
            OfCustomAccountModel.BasedOnName(
                accountName = "Awesome",
                randomNextInt = Color.entries.lastIndex,
                expectedColor = Color.entries.last(),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OfCustomAccountWithTypeAndColor {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun ofCustomAccount(model: OfCustomAccountModel.WithTypeAndColor) {
            // Act
            val actual = CryptoPortfolioIcon.ofCustomAccount(type = model.type, color = model.color)

            // Assert
            val expectedColor = model.expectedColor
            Truth.assertThat(actual.color).isEqualTo(expectedColor)

            val expectedType = model.expectedType
            Truth.assertThat(actual.type).isEqualTo(expectedType)
        }

        private fun provideTestModels() = listOf(
            OfCustomAccountModel.WithTypeAndColor(
                type = Type.Icon(value = Icon.User),
                color = Color.CaribbeanBlue,
                expectedType = Type.Icon(value = Icon.User),
                expectedColor = Color.CaribbeanBlue,
            ),
            OfCustomAccountModel.WithTypeAndColor(
                type = Type.Symbol(value = 'A'),
                color = Color.DullLavender,
                expectedType = Type.Symbol(value = 'A'),
                expectedColor = Color.DullLavender,
            ),
        )
    }

    sealed interface OfCustomAccountModel {

        data class BasedOnName(
            val accountName: String,
            val randomNextInt: Int,
            val expectedColor: Color,
        ) : OfCustomAccountModel

        data class WithTypeAndColor(
            val type: Type,
            val color: Color,
            val expectedType: Type,
            val expectedColor: Color,
        ) : OfCustomAccountModel
    }
}