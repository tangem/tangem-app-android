package com.tangem.data.card

import com.google.common.truth.Truth.assertThat
import com.tangem.TangemSdk
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.Config
import com.tangem.data.card.sdk.CardSdkProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class DefaultCardSdkConfigRepositoryTest {

    private val cardSdkProvider: CardSdkProvider = mockk()
    private val sdk: TangemSdk = mockk()

    private val repository = DefaultCardSdkConfigRepository(
        cardSdkProvider = cardSdkProvider,
        transactionSignerFactory = mockk(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(cardSdkProvider, sdk)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardIdDisplayFormatProperty {

        @Test
        fun `GIVEN sdk config format WHEN cardIdDisplayFormat THEN returns it`() {
            // Arrange
            val expected = CardIdDisplayFormat.LastLuhn(numbers = 4)
            every { cardSdkProvider.sdk } returns sdk
            every { sdk.config } returns Config(cardIdDisplayFormat = expected)

            // Act
            val actual = repository.cardIdDisplayFormat

            // Assert
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RestoreCardIdDisplayFormat {

        @Test
        fun `GIVEN registered sdk WHEN restoreCardIdDisplayFormat THEN sets format to config`() {
            // Arrange
            val config = Config(cardIdDisplayFormat = CardIdDisplayFormat.Full)
            val expected = CardIdDisplayFormat.LastLuhn(numbers = 4)
            every { cardSdkProvider.sdkOrNull } returns sdk
            every { sdk.config } returns config

            // Act
            repository.restoreCardIdDisplayFormat(expected)

            // Assert
            assertThat(config.cardIdDisplayFormat).isEqualTo(expected)
        }

        @Test
        fun `GIVEN no registered sdk WHEN restoreCardIdDisplayFormat THEN does not request sdk`() {
            // Arrange
            every { cardSdkProvider.sdkOrNull } returns null

            // Act
            repository.restoreCardIdDisplayFormat(CardIdDisplayFormat.Full)

            // Assert
            verify(exactly = 0) { cardSdkProvider.sdk }
        }
    }
}