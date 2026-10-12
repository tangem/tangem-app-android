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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCollectibleGachaMachineUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = GetCollectibleGachaMachineUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN repository returns a machine WHEN invoke THEN returns it on the right`() = runTest {
        // Arrange
        val machine = CollectiblesModelFactory.createMachine()
        coEvery { repository.getMachine("pokemon_50") } returns machine

        // Act
        val actual = useCase(externalId = "pokemon_50")

        // Assert
        assertThat(actual).isEqualTo(machine.right())
        coVerify(exactly = 1) { repository.getMachine("pokemon_50") }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns an Api error carrying the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("no such machine")
        coEvery { repository.getMachine(any()) } throws cause

        // Act
        val actual = useCase(externalId = "pokemon_50")

        // Assert
        assertThat(actual).isEqualTo(CollectiblesError.Api(cause).left())
    }
}