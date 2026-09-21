package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchAction
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
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

internal class GetCollectibleGachaMachineNftsBatchFlowUseCaseTest {

    private val repository: CollectiblesRepository = mockk()
    private val batchFlow: CollectibleGachaMachineNftsBatchFlow = mockk()

    private val useCase = GetCollectibleGachaMachineNftsBatchFlowUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
        every { repository.getMachineNftsBatchFlow(any(), any(), any()) } returns batchFlow
    }

    @Test
    fun `GIVEN no batch size WHEN invoke THEN the repository is asked for the default page of fifty`() = runTest {
        // Arrange
        val context = batchingContext()

        // Act
        val actual = useCase(context = context, userWalletId = mockk())

        // Assert
        assertThat(actual).isSameInstanceAs(batchFlow)
        verify(exactly = 1) {
            repository.getMachineNftsBatchFlow(
                context = context,
                batchSize = 50,
                userWalletId = any()
            )
        }
    }

    @Test
    fun `GIVEN an explicit batch size WHEN invoke THEN it overrides the default`() = runTest {
        // Arrange
        val context = batchingContext()

        // Act
        useCase(context = context, batchSize = 7, userWalletId = mockk())

        // Assert
        verify(exactly = 1) {
            repository.getMachineNftsBatchFlow(
                context = context,
                batchSize = 7,
                userWalletId = any()
            )
        }
    }

    private fun TestScope.batchingContext() = BatchingContext(
        actionsFlow = MutableSharedFlow<CollectibleGachaMachineNftsBatchAction>(),
        coroutineScope = backgroundScope,
    )
}