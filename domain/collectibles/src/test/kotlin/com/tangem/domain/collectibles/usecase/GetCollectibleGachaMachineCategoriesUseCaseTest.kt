package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesModelFactory
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCollectibleGachaMachineCategoriesUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = GetCollectibleGachaMachineCategoriesUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN repository returns categories WHEN invoke THEN returns them on the right`() = runTest {
        // Arrange
        val categories = CollectiblesModelFactory.createCategories()
        coEvery { repository.getCategories() } returns categories

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(categories.right())
        coVerify(exactly = 1) { repository.getCategories() }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns an Api error carrying the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("backend is down")
        coEvery { repository.getCategories() } throws cause

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(CollectiblesError.Api(cause).left())
    }

    /**
     * Cancellation is asserted once, here: all four Either-returning use cases route failures through the same
     * [com.tangem.utils.coroutines.runSuspendCatching], which rethrows [CancellationException] instead of folding
     * it into a Left — otherwise a destroyed caller would render an error state for its own teardown.
     */
    @Test
    fun `GIVEN repository is cancelled WHEN invoke THEN the cancellation propagates instead of becoming a Left`() =
        runTest {
            // Arrange
            coEvery { repository.getCategories() } throws CancellationException("scope destroyed")

            // Act
            val thrown = runCatching { useCase() }.exceptionOrNull()

            // Assert
            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }
}