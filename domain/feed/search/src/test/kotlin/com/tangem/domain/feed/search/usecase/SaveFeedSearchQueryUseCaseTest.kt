package com.tangem.domain.feed.search.usecase

import com.tangem.domain.feed.search.repository.FeedSearchHistoryRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveFeedSearchQueryUseCaseTest {

    private val repository: FeedSearchHistoryRepository = mockk(relaxed = true)

    private val useCase = SaveFeedSearchQueryUseCase(repository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: SaveModel) = runTest {
        // Act
        useCase(model.query)

        // Assert
        if (model.expected == null) {
            coVerify(exactly = 0) { repository.saveRecentQuery(any()) }
        } else {
            coVerify(exactly = 1) { repository.saveRecentQuery(model.expected) }
        }
    }

    internal data class SaveModel(val query: String, val expected: String?)

    private fun provideTestModels() = listOf(
        SaveModel(query = "bitcoin", expected = "bitcoin"),
        SaveModel(query = "  bitcoin  ", expected = "bitcoin"),
        SaveModel(query = "", expected = null),
        SaveModel(query = "   ", expected = null),
    )
}