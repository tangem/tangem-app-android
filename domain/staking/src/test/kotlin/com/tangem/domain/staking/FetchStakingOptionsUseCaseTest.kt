package com.tangem.domain.staking

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FetchStakingOptionsUseCaseTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val p2pEthPoolRepository: P2PEthPoolRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = FetchStakingOptionsUseCase(
        stakeKitRepository = stakeKitRepository,
        p2pEthPoolRepository = p2pEthPoolRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, p2pEthPoolRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns StakingError.DomainError("resolved")
        coEvery { stakeKitRepository.fetchYields() } just Runs
        coEvery { p2pEthPoolRepository.fetchVaults(any()) } just Runs
        coEvery { p2pEthPoolRepository.fetchVaultLimits() } just Runs
    }

    @Test
    fun `GIVEN all fetches succeed WHEN invoke THEN Right and all three fetches called`() = runTest {
        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { stakeKitRepository.fetchYields() }
        coVerify(exactly = 1) { p2pEthPoolRepository.fetchVaults(P2PEthPoolNetwork.MAINNET) }
        coVerify(exactly = 1) { p2pEthPoolRepository.fetchVaultLimits() }
    }

    @Test
    fun `GIVEN all fetches suspend WHEN invoke THEN returns only after all three completed`() = runTest {
        // Arrange
        val completedFetches = mutableListOf<String>()
        coEvery { stakeKitRepository.fetchYields() } coAnswers {
            delay(SLOW_FETCH_MILLIS)
            completedFetches += "yields"
        }
        coEvery { p2pEthPoolRepository.fetchVaults(any()) } coAnswers {
            delay(SLOW_FETCH_MILLIS)
            completedFetches += "vaults"
        }
        coEvery { p2pEthPoolRepository.fetchVaultLimits() } coAnswers {
            delay(SLOW_FETCH_MILLIS)
            completedFetches += "limits"
        }

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(completedFetches).containsExactly("yields", "vaults", "limits")
    }

    // Pins current behaviour: Raise.catch returns the resolved error as a value, it is never raised
    @Test
    fun `GIVEN fetchVaults throws WHEN invoke THEN sibling fetches cancelled and error resolved`() = runTest {
        // Arrange
        val completedFetches = mutableListOf<String>()
        coEvery { stakeKitRepository.fetchYields() } coAnswers {
            delay(SLOW_FETCH_MILLIS)
            completedFetches += "yields"
        }
        coEvery { p2pEthPoolRepository.fetchVaults(any()) } throws IllegalStateException("vaults")
        coEvery { p2pEthPoolRepository.fetchVaultLimits() } coAnswers {
            delay(SLOW_FETCH_MILLIS)
            completedFetches += "limits"
        }

        // Act
        val actual = useCase()
        advanceUntilIdle()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(completedFetches).isEmpty()
        coVerify(exactly = 1) { stakeKitRepository.fetchYields() }
        verify(exactly = 1) {
            stakingErrorResolver.resolve(match { it is IllegalStateException && it.message == "vaults" })
        }
    }

    private companion object {
        const val SLOW_FETCH_MILLIS = 1_000L
    }
}