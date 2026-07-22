package com.tangem.feature.swap

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultSwapFeedbackRepositoryTest {

    private val remoteSource: SwapFeedbackRemoteSource = mockk()

    private val repository = DefaultSwapFeedbackRepository(remoteSource)

    @BeforeEach
    fun resetMocks() {
        clearMocks(remoteSource)
    }

    private fun submitParams(rating: Int = 5) = SwapFeedbackParams(
        userWalletIdHash = USER_WALLET_ID_HASH,
        providerName = PROVIDER_NAME,
        txUrl = TX_EXTERNAL_URL,
        txExternalId = TX_EXTERNAL_ID,
        rating = rating,
        feedback = "feedback",
    )

    @Test
    fun `GIVEN cache miss WHEN fetchRatingIfNeeded THEN rating is fetched and cached`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_EXTERNAL_ID) } returns 4.right()

        // Act
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)

        // Assert
        val emitted = getEmittedValues(repository.observeRating(TX_EXTERNAL_ID))
        assertThat(emitted).containsExactly(SwapRating.Rated(rating = 4))
    }

    @Test
    fun `GIVEN remote has no rating WHEN fetchRatingIfNeeded THEN NotRated is cached`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_EXTERNAL_ID) } returns null.right()

        // Act
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)

        // Assert
        val emitted = getEmittedValues(repository.observeRating(TX_EXTERNAL_ID))
        assertThat(emitted).containsExactly(SwapRating.NotRated)
    }

    @Test
    fun `GIVEN rating already cached WHEN fetchRatingIfNeeded again THEN no second remote call`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_EXTERNAL_ID) } returns 4.right()

        // Act
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)

        // Assert
        coVerify(exactly = 1) { remoteSource.getRating(TX_EXTERNAL_ID) }
    }

    @Test
    fun `GIVEN load failed WHEN fetchRatingIfNeeded again THEN remote call is retried`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_EXTERNAL_ID) } returns RuntimeException("error").left()

        // Act
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)

        // Assert
        coVerify(exactly = 2) { remoteSource.getRating(TX_EXTERNAL_ID) }
    }

    @Test
    fun `GIVEN unrated tx WHEN submitFeedback THEN optimistic entry cached and params delegated`() = runTest {
        // Arrange
        val params = submitParams(rating = 5)
        coEvery { remoteSource.submitFeedback(params) } returns Unit.right()

        // Act
        val result = repository.submitFeedback(params)

        // Assert
        assertThat(result.isRight()).isTrue()
        val emitted = getEmittedValues(repository.observeRating(TX_EXTERNAL_ID))
        assertThat(emitted).containsExactly(SwapRating.Rated(rating = 5))
        coVerify(exactly = 1) { remoteSource.submitFeedback(params) }
    }

    @Test
    fun `GIVEN remote submit fails WHEN submitFeedback THEN entry is rolled back and error returned`() = runTest {
        // Arrange
        coEvery { remoteSource.submitFeedback(any()) } returns RuntimeException("error").left()

        // Act
        val result = repository.submitFeedback(submitParams(rating = 5))

        // Assert
        assertThat(result.isLeft()).isTrue()
        val emitted = getEmittedValues(repository.observeRating(TX_EXTERNAL_ID))
        assertThat(emitted).containsExactly(null)
    }

    @Test
    fun `GIVEN NoOp remote WHEN fetch and submit THEN unrated first and rating accepted silently`() = runTest {
        // Arrange
        val repository = DefaultSwapFeedbackRepository(NoOpSwapFeedbackRemoteSource())

        // Act
        val emitted = getEmittedValues(repository.observeRating(TX_EXTERNAL_ID))
        repository.fetchRatingIfNeeded(TX_EXTERNAL_ID)
        val result = repository.submitFeedback(submitParams(rating = 3))

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(emitted)
            .containsExactly(null, SwapRating.NotRated, SwapRating.Rated(rating = 3))
            .inOrder()
    }

    private companion object {
        const val TX_EXTERNAL_ID = "tx-external-id"
        const val PROVIDER_NAME = "ChangeNow"
        const val TX_EXTERNAL_URL = "https://provider.example/tx"
        const val USER_WALLET_ID_HASH = "wallet-id-hash"
    }
}