package com.tangem.feature.swap

import androidx.datastore.core.DataStore
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class DefaultSwapFeedbackRepositoryTest {

    private val remoteSource: SwapFeedbackRemoteSource = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val dataStore = MockStateDataStore(default = SwapRatingsDTO())

    private var nowMillis = 1_000L

    private val repository = DefaultSwapFeedbackRepository(
        remoteSource = remoteSource,
        store = dataStore,
        dispatchers = dispatchers,
        currentTimeMillis = { nowMillis },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(remoteSource)
        nowMillis = 1_000L
    }

    @Test
    fun `GIVEN nothing stored WHEN fetchRatingIfNeeded THEN rating read once and stored`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_ID) } returns 4.right()

        // Act
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        assertThat(repository.observeRating(TX_ID).first()).isEqualTo(SwapRating.Rated(rating = 4))
        coVerify(exactly = 1) { remoteSource.getRating(TX_ID) }
    }

    @Test
    fun `GIVEN rating already stored WHEN fetchRatingIfNeeded THEN remote is not called`() = runTest {
        // Arrange
        storeRatings(TX_ID to StoredSwapRating(rating = 5, savedAt = 1L))

        // Act
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        coVerify(exactly = 0) { remoteSource.getRating(any()) }
    }

    @Test
    fun `GIVEN swap is not rated WHEN fetchRatingIfNeeded twice THEN remote is called once`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_ID) } returns null.right()

        // Act
        repository.fetchRatingIfNeeded(TX_ID)
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        assertThat(repository.observeRating(TX_ID).first()).isEqualTo(SwapRating.NotRated)
        coVerify(exactly = 1) { remoteSource.getRating(TX_ID) }
    }

    @Test
    fun `GIVEN read fails WHEN fetchRatingIfNeeded twice THEN remote is called once and nothing stored`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_ID) } returns RuntimeException("HTTP 429").left()

        // Act
        repository.fetchRatingIfNeeded(TX_ID)
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        assertThat(repository.observeRating(TX_ID).first()).isNull()
        assertThat(readRatings()).isEmpty()
        coVerify(exactly = 1) { remoteSource.getRating(TX_ID) }
    }

    @Test
    fun `GIVEN store is full WHEN a rating is stored THEN the oldest entry is evicted`() = runTest {
        // Arrange
        val existing = (1..MAX_STORED_RATINGS).associate { index ->
            "tx-$index" to StoredSwapRating(rating = null, savedAt = index.toLong())
        }
        storeRatings(*existing.toList().toTypedArray())
        nowMillis = Long.MAX_VALUE
        coEvery { remoteSource.getRating(TX_ID) } returns 3.right()

        // Act
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        val stored = readRatings()
        assertThat(stored).hasSize(MAX_STORED_RATINGS)
        assertThat(stored.keys).contains(TX_ID)
        assertThat(stored.keys).doesNotContain("tx-1")
    }

    @Test
    fun `GIVEN submit succeeds WHEN submitFeedback THEN rating is stored and params are delegated`() = runTest {
        // Arrange
        val params = submitParams(rating = 5)
        coEvery { remoteSource.submitFeedback(params) } returns Unit.right()

        // Act
        val result = repository.submitFeedback(params)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(repository.observeRating(TX_ID).first()).isEqualTo(SwapRating.Rated(rating = 5))
        coVerify(exactly = 1) { remoteSource.submitFeedback(params) }
    }

    @Test
    fun `GIVEN submit fails WHEN submitFeedback THEN nothing is stored`() = runTest {
        // Arrange
        coEvery { remoteSource.submitFeedback(any()) } returns RuntimeException("HTTP 429").left()

        // Act
        val result = repository.submitFeedback(submitParams(rating = 5))

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(repository.observeRating(TX_ID).first()).isNull()
        assertThat(readRatings()).isEmpty()
    }

    @Test
    fun `GIVEN swap read as not rated WHEN submit fails THEN the stored not-rated entry survives`() = runTest {
        // Arrange
        coEvery { remoteSource.getRating(TX_ID) } returns null.right()
        coEvery { remoteSource.submitFeedback(any()) } returns RuntimeException("HTTP 429").left()
        repository.fetchRatingIfNeeded(TX_ID)

        // Act
        repository.submitFeedback(submitParams(rating = 5))

        // Assert
        assertThat(repository.observeRating(TX_ID).first()).isEqualTo(SwapRating.NotRated)
        coVerify(exactly = 1) { remoteSource.getRating(TX_ID) }
    }

    @Test
    fun `GIVEN rating submitted WHEN fetchRatingIfNeeded THEN remote read is skipped`() = runTest {
        // Arrange
        coEvery { remoteSource.submitFeedback(any()) } returns Unit.right()
        repository.submitFeedback(submitParams(rating = 5))

        // Act
        repository.fetchRatingIfNeeded(TX_ID)

        // Assert
        coVerify(exactly = 0) { remoteSource.getRating(any()) }
    }

    @Test
    fun `GIVEN no survey config WHEN fetch and submit THEN unrated first and rating accepted silently`() = runTest {
        // Arrange
        val repository = DefaultSwapFeedbackRepository(
            remoteSource = NoOpSwapFeedbackRemoteSource(),
            store = MockStateDataStore(default = SwapRatingsDTO()),
            dispatchers = dispatchers,
            currentTimeMillis = { nowMillis },
        )

        // Act
        val emitted = getEmittedValues(repository.observeRating(TX_ID))
        repository.fetchRatingIfNeeded(TX_ID)
        val result = repository.submitFeedback(submitParams(rating = 3))

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(emitted)
            .containsExactly(null, SwapRating.NotRated, SwapRating.Rated(rating = 3))
            .inOrder()
    }

    @Test
    fun `GIVEN the store cannot be read WHEN fetchRatingIfNeeded THEN the vendor is asked and nothing throws`() =
        runTest {
            // Arrange
            val flakyStore = FlakyDataStore(MockStateDataStore(default = SwapRatingsDTO()))
            val repository = createRepository(flakyStore)
            flakyStore.failReads = true
            coEvery { remoteSource.getRating(TX_ID) } returns 4.right()

            // Act
            repository.fetchRatingIfNeeded(TX_ID)

            // Assert
            coVerify(exactly = 1) { remoteSource.getRating(TX_ID) }
        }

    @Test
    fun `GIVEN the store cannot be written WHEN fetchRatingIfNeeded THEN nothing throws and nothing is stored`() =
        runTest {
            // Arrange
            val flakyStore = FlakyDataStore(MockStateDataStore(default = SwapRatingsDTO()))
            val repository = createRepository(flakyStore)
            flakyStore.failWrites = true
            coEvery { remoteSource.getRating(TX_ID) } returns 4.right()

            // Act
            repository.fetchRatingIfNeeded(TX_ID)

            // Assert
            assertThat(repository.observeRating(TX_ID).first()).isNull()
        }

    @Test
    fun `GIVEN a read fails WHEN observeRating THEN null is emitted and later values still arrive`() = runTest {
        // Arrange
        val flakyStore = FlakyDataStore(MockStateDataStore(default = SwapRatingsDTO()))
        val repository = createRepository(flakyStore)
        coEvery { remoteSource.submitFeedback(any()) } returns Unit.right()
        flakyStore.failReads = true

        // Act
        val emitted = getEmittedValues(repository.observeRating(TX_ID))
        runCurrent()
        flakyStore.failReads = false
        repository.submitFeedback(submitParams(rating = 5))
        advanceTimeBy(5_000)
        runCurrent()

        // Assert
        assertThat(emitted).containsExactly(null, SwapRating.Rated(rating = 5)).inOrder()
    }

    private fun createRepository(store: DataStore<SwapRatingsDTO>) = DefaultSwapFeedbackRepository(
        remoteSource = remoteSource,
        store = store,
        dispatchers = dispatchers,
        currentTimeMillis = { nowMillis },
    )

    /** Store whose reads and writes can be made to fail on demand */
    private class FlakyDataStore(
        private val delegate: DataStore<SwapRatingsDTO>,
    ) : DataStore<SwapRatingsDTO> {

        var failReads = false
        var failWrites = false

        override val data: Flow<SwapRatingsDTO>
            get() = flow {
                if (failReads) throw IOException("read failed")
                emitAll(delegate.data)
            }

        override suspend fun updateData(transform: suspend (t: SwapRatingsDTO) -> SwapRatingsDTO): SwapRatingsDTO {
            if (failWrites) throw IOException("write failed")
            return delegate.updateData(transform)
        }
    }

    private suspend fun storeRatings(vararg entries: Pair<String, StoredSwapRating>) {
        dataStore.updateData { SwapRatingsDTO(ratings = entries.toMap()) }
    }

    private suspend fun readRatings(): Map<String, StoredSwapRating> = dataStore.data.first().ratings

    private fun submitParams(rating: Int) = SwapFeedbackParams(
        userWalletIdHash = USER_WALLET_ID_HASH,
        providerName = PROVIDER_NAME,
        txUrl = TX_EXTERNAL_URL,
        txExternalId = TX_ID,
        rating = rating,
        feedback = "feedback",
    )

    private companion object {
        const val TX_ID = "tx-external-id"
        const val PROVIDER_NAME = "ChangeNow"
        const val TX_EXTERNAL_URL = "https://provider.example/tx"
        const val USER_WALLET_ID_HASH = "wallet-id-hash"
        const val MAX_STORED_RATINGS = DefaultSwapFeedbackRepository.MAX_STORED_RATINGS
    }
}