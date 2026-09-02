package com.tangem.domain.markets

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.pagination.BatchingContext
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetMarketCategoryListFlowUseCaseTest {

    private val marketsTokenRepository: MarketsTokenRepository = mockk()
    private val batchFlow: MarketCategoryListBatchFlow = mockk()

    private val useCase = GetMarketCategoryListFlowUseCase(marketsTokenRepository = marketsTokenRepository)

    @BeforeEach
    fun setup() {
        clearMocks(marketsTokenRepository)
        every { marketsTokenRepository.getMarketCategoryListFlow(any(), any(), any()) } returns batchFlow
    }

    @Test
    fun `GIVEN a batching context WHEN invoke THEN the repository flow is returned as is`() {
        // Arrange
        val context = batchingContext()

        // Act
        val actual = useCase(batchingContext = context)

        // Assert
        assertThat(actual).isSameInstanceAs(batchFlow)
        verify(exactly = 1) {
            marketsTokenRepository.getMarketCategoryListFlow(
                batchingContext = context,
                firstBatchSize = any(),
                nextBatchSize = any(),
            )
        }
    }

    @Test
    fun `GIVEN a flow type WHEN invoke THEN its batch sizes are forwarded instead of magic numbers`() {
        // Arrange
        val type = GetMarketCategoryListFlowUseCase.BatchFlowType.Main

        // Act
        useCase(batchingContext = batchingContext(), batchFlowType = type)

        // Assert
        verify(exactly = 1) {
            marketsTokenRepository.getMarketCategoryListFlow(
                batchingContext = any(),
                firstBatchSize = type.firstBatchSize,
                nextBatchSize = type.nextBatchSize,
            )
        }
    }

    private fun batchingContext(): MarketCategoryListBatchingContext = BatchingContext(
        actionsFlow = emptyFlow(),
        coroutineScope = TestScope(),
    )
}