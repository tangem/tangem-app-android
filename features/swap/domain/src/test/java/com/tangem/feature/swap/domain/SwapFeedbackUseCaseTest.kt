package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.ExistingRating
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SwapFeedbackUseCaseTest {

    private val repository: SwapFeedbackRepository = mockk()
    private val useCase = SwapFeedbackUseCase(repository)

    @Test
    fun `getExistingRating returns ExistingRating when rated`() = runTest {
        coEvery { repository.getRating("tx123") } returns ExistingRating(rating = 4).right()

        val result = useCase.getExistingRating("tx123")

        assertThat(result.getOrNull()).isEqualTo(ExistingRating(rating = 4))
    }

    @Test
    fun `getExistingRating returns null when not rated`() = runTest {
        coEvery { repository.getRating("tx123") } returns null.right()

        val result = useCase.getExistingRating("tx123")

        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `getExistingRating returns Left on error`() = runTest {
        coEvery { repository.getRating("tx123") } returns RuntimeException("Network error").left()

        val result = useCase.getExistingRating("tx123")

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `submit delegates to repository`() = runTest {
        val params = SwapFeedbackParams(
            userWalletIdHash = "hash",
            providerName = "ChangeNOW",
            txUrl = "https://example.com/tx/abc",
            txExternalId = "tx123",
            rating = 5,
            feedback = "Great!",
        )
        coEvery { repository.submitFeedback(params) } returns Unit.right()

        useCase.submit(params)

        coVerify(exactly = 1) { repository.submitFeedback(params) }
    }
}