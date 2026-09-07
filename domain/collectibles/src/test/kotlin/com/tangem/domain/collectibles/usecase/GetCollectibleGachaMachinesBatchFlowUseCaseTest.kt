package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.pagination.BatchingContext
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCollectibleGachaMachinesBatchFlowUseCaseTest {

    private val repository: CollectiblesRepository = mockk()
    private val batchFlow: CollectibleGachaMachinesBatchFlow = mockk()

    private val useCase = GetCollectibleGachaMachinesBatchFlowUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
        every { repository.getMachinesBatchFlow(any(), any()) } returns batchFlow
    }

    @Test
    fun `GIVEN no batch size WHEN invoke THEN the repository is asked for the default page of twenty`() = runTest {
        // Arrange
        val context = batchingContext()

        // Act
        val actual = useCase(context = context)

        // Assert
        assertThat(actual).isSameInstanceAs(batchFlow)
        verify(exactly = 1) { repository.getMachinesBatchFlow(context = context, batchSize = 20) }
    }

    @Test
    fun `GIVEN an explicit batch size WHEN invoke THEN it overrides the default`() = runTest {
        // Arrange
        val context = batchingContext()

        // Act
        useCase(context = context, batchSize = 7)

        // Assert
        verify(exactly = 1) { repository.getMachinesBatchFlow(context = context, batchSize = 7) }
    }

    private fun TestScope.batchingContext() = BatchingContext(
        actionsFlow = MutableSharedFlow<CollectibleGachaMachinesBatchAction>(),
        coroutineScope = backgroundScope,
    )
}