package com.tangem.domain.staking

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakeKitRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.tangem.test.mock.staking.MockStaking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetStakingTargetsByAddressUseCaseTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val p2pEthPoolRepository: P2PEthPoolRepository = mockk()

    private val useCase = GetStakingTargetsByAddressUseCase(
        stakeKitRepository = stakeKitRepository,
        p2pEthPoolRepository = p2pEthPoolRepository,
    )

    private val v1 = MockStaking.validator()
    private val v2 = MockStaking.validator(address = "validator-2", name = "Validator 2")
    private val vault = MockStaking.vault()

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, p2pEthPoolRepository)
    }

    @Test
    fun `GIVEN validators and vaults WHEN invoke THEN map keyed by address`() = runTest {
        // Arrange
        every { stakeKitRepository.getPersistedValidatorsFlow() } returns flowOf(listOf(v1, v2))
        every { p2pEthPoolRepository.getPersistedVaultsFlow() } returns flowOf(listOf(vault))

        // Act
        val actual = useCase().first()

        // Assert
        assertThat(actual).isEqualTo(
            mapOf(
                "validator-1" to StakingTarget.Validator(v1),
                "validator-2" to StakingTarget.Validator(v2),
                "0xvault1" to StakingTarget.Vault(vault),
            ),
        )
    }

    @Test
    fun `GIVEN validator and vault with same address WHEN invoke THEN vault wins`() = runTest {
        // Arrange
        every { stakeKitRepository.getPersistedValidatorsFlow() } returns
            flowOf(listOf(MockStaking.validator(address = "0xvault1")))
        every { p2pEthPoolRepository.getPersistedVaultsFlow() } returns flowOf(listOf(vault))

        // Act
        val actual = useCase().first()

        // Assert
        assertThat(actual).isEqualTo(mapOf("0xvault1" to StakingTarget.Vault(vault)))
    }

    @Test
    fun `GIVEN validators updated WHEN collecting THEN new map emitted`() = runTest {
        // Arrange
        val validators = MutableStateFlow(listOf(v1))
        every { stakeKitRepository.getPersistedValidatorsFlow() } returns validators
        every { p2pEthPoolRepository.getPersistedVaultsFlow() } returns flowOf(emptyList())

        // Act & Assert
        useCase().test {
            assertThat(awaitItem()).isEqualTo(mapOf("validator-1" to StakingTarget.Validator(v1)))

            validators.value = listOf(v1, v2)

            assertThat(awaitItem().keys).containsExactly("validator-1", "validator-2").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }
}