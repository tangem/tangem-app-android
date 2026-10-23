package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.repositories.StakeKitActionRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FetchActionsUseCaseTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val stakeKitActionRepository: StakeKitActionRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = FetchActionsUseCase(
        stakeKitRepository = stakeKitRepository,
        stakeKitActionRepository = stakeKitActionRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    private val userWalletId = UserWalletId(stringValue = "011")
    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val action = MockStaking.stakingAction()

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, stakeKitActionRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    private suspend fun invoke() = useCase(userWalletId, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)

    @Test
    fun `GIVEN repository returns actions WHEN invoke THEN actions stored and Right`() = runTest {
        // Arrange
        coEvery {
            stakeKitRepository.getActions(userWalletId, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)
        } returns listOf(action)
        coEvery { stakeKitActionRepository.store(any(), any(), any()) } just Runs

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { stakeKitActionRepository.store(userWalletId, solana.id, listOf(action)) }
    }

    @Test
    fun `GIVEN getActions throws WHEN invoke THEN resolved Left and nothing stored`() = runTest {
        // Arrange
        coEvery {
            stakeKitRepository.getActions(userWalletId, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)
        } throws IllegalStateException("boom")

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
        coVerify(exactly = 0) { stakeKitActionRepository.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN store throws WHEN invoke THEN resolved Left`() = runTest {
        // Arrange
        coEvery {
            stakeKitRepository.getActions(userWalletId, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)
        } returns listOf(action)
        coEvery { stakeKitActionRepository.store(any(), any(), any()) } throws IllegalStateException("boom")

        // Act
        val actual = invoke()

        // Assert
        assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}