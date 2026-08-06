package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SwapFeedbackUseCaseTest {

    private val repository: SwapFeedbackRepository = mockk()
    private val useCase = SwapFeedbackUseCase(repository)

    @Test
    fun `GIVEN deal data WHEN submit THEN wallet id is hashed and params delegated to repository`() = runTest {
        // Arrange
        val paramsSlot = slot<SwapFeedbackParams>()
        coEvery { repository.submitFeedback(capture(paramsSlot)) } returns Unit.right()

        // Act
        val result = useCase.submit(
            SwapFeedbackUseCase.SubmitParams(
                txExternalId = "tx123",
                providerName = "ChangeNOW",
                txExternalUrl = "https://example.com/tx/abc",
                userWalletId = UserWalletId(USER_WALLET_ID_HEX),
                rating = 5,
                feedback = "Great!",
            ),
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(paramsSlot.captured).isEqualTo(
            SwapFeedbackParams(
                userWalletIdHash = USER_WALLET_ID_HASH,
                providerName = "ChangeNOW",
                txUrl = "https://example.com/tx/abc",
                txExternalId = "tx123",
                rating = 5,
                feedback = "Great!",
            ),
        )
    }

    @Test
    fun `GIVEN repository fails WHEN submit THEN error is returned`() = runTest {
        // Arrange
        coEvery { repository.submitFeedback(any()) } returns RuntimeException("Network error").left()

        // Act
        val result = useCase.submit(
            SwapFeedbackUseCase.SubmitParams(
                txExternalId = "tx123",
                providerName = "ChangeNOW",
                txExternalUrl = "https://example.com/tx/abc",
                userWalletId = UserWalletId(USER_WALLET_ID_HEX),
                rating = 5,
                feedback = "",
            ),
        )

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN tx external id WHEN ensureLoaded THEN delegated to repository`() = runTest {
        // Arrange
        coEvery { repository.fetchRatingIfNeeded("tx123") } returns Unit

        // Act
        useCase.ensureLoaded("tx123")

        // Assert
        coVerify(exactly = 1) { repository.fetchRatingIfNeeded("tx123") }
    }

    private companion object {
        const val USER_WALLET_ID_HEX = "0011223344556677"

        /** SHA-256 of [USER_WALLET_ID_HEX] bytes, hex-encoded — mirrors the legacy hashing in TokenDetailsModel */
        val USER_WALLET_ID_HASH = UserWalletId(USER_WALLET_ID_HEX).value.calculateSha256().toHexString()
    }
}