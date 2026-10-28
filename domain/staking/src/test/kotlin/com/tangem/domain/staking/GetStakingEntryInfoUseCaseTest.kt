package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetStakingEntryInfoUseCaseTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = GetStakingEntryInfoUseCase(
        stakeKitRepository = stakeKitRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val stakeKitOption = StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield)

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
        coEvery { stakeKitRepository.getEntryInfo(solana.id, "SOL") } returns StakingEntryInfo("SOL")
    }

    @Test
    fun `GIVEN StakeKit option WHEN invoke THEN repository entry info returned`() = runTest {
        // Act
        val actual = useCase(solana.id, "SOL", stakeKitOption)

        // Assert
        assertThat(actual).isEqualTo(StakingEntryInfo("SOL").right())
        coVerify(exactly = 1) { stakeKitRepository.getEntryInfo(solana.id, "SOL") }
    }

    @Test
    fun `GIVEN StakeKit option and repository throws WHEN invoke THEN resolved error returned`() = runTest {
        // Arrange
        coEvery { stakeKitRepository.getEntryInfo(any(), any()) } throws IllegalStateException("boom")

        // Act
        val actual = useCase(solana.id, "SOL", stakeKitOption)

        // Assert
        assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
        verify(exactly = 1) {
            stakingErrorResolver.resolve(match { it is IllegalStateException && it.message == "boom" })
        }
    }

    @Test
    fun `GIVEN P2PEthPool option WHEN invoke THEN ETH entry info without repository call`() = runTest {
        // Arrange
        val option = StakingOption.P2PEthPool(listOf(MockStaking.vault()))

        // Act
        val actual = useCase(solana.id, "XYZ", option)

        // Assert
        assertThat(actual).isEqualTo(StakingEntryInfo("ETH").right())
        coVerify(exactly = 0) { stakeKitRepository.getEntryInfo(any(), any()) }
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}