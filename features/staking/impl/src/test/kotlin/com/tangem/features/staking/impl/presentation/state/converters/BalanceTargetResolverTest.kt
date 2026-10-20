package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.target
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class BalanceTargetResolverTest {

    @Nested
    inner class StakeKit {

        private val listed = listOf(
            MockStaking.validator(),
            MockStaking.validator(address = "validator-10", name = "Validator 10"),
        )
        private val integration = stakeKitIntegration(
            id = StakingIntegrationID.StakeKit.Coin.Cosmos,
            yield = MockStaking.yield(
                id = MockStaking.COSMOS_ID,
                token = MockStaking.atomToken,
                validators = listed,
                allValidators = listed + MockStaking.validator(
                    address = "jailed-1",
                    name = "Jailed",
                    status = Yield.Validator.ValidatorStatus.JAILED,
                    preferred = false,
                ),
            ),
        )

        @Test
        fun `GIVEN null address WHEN findBalanceTarget THEN null`() {
            // Act & Assert
            assertThat(integration.findBalanceTarget(address = null)).isNull()
        }

        @Test
        fun `GIVEN blank address WHEN findBalanceTarget THEN null`() {
            // Act & Assert
            assertThat(integration.findBalanceTarget(address = "  ")).isNull()
        }

        @Test
        fun `GIVEN listed validator address in other case WHEN findBalanceTarget THEN listed target`() {
            // Act
            val actual = integration.findBalanceTarget(address = "VALIDATOR-1")

            // Assert
            assertThat(actual).isEqualTo(target(MockStaking.validator()))
        }

        @Test
        fun `GIVEN jailed validator address WHEN findBalanceTarget THEN jailed target from all targets`() {
            // Act
            val actual = integration.findBalanceTarget(address = "jailed-1")

            // Assert
            assertThat(actual?.isActive).isFalse()
            assertThat(actual?.name).isEqualTo("Jailed")
        }

        // Pins current behaviour: matching is substring-based, so validator-10 resolves to validator-1.
        @Test
        fun `GIVEN address containing a listed address WHEN findBalanceTarget THEN first listed substring match returned`() {
            // Act
            val actual = integration.findBalanceTarget(address = "validator-10")

            // Assert
            assertThat(actual?.address).isEqualTo(MockStaking.VALIDATOR_ADDRESS)
        }

        @Test
        fun `GIVEN unlisted address with name WHEN findBalanceTarget THEN unlisted non preferred target with given name`() {
            // Act
            val actual = integration.findBalanceTarget(address = UNLISTED_ADDRESS, name = "Unknown")

            // Assert
            val expected = StakingTarget.Validator(
                Yield.Validator(
                    address = UNLISTED_ADDRESS,
                    status = Yield.Validator.ValidatorStatus.UNKNOWN,
                    name = "Unknown",
                    isPreferred = false,
                    isStrategicPartner = false,
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN unlisted address without name WHEN findBalanceTarget THEN brief address used as name`() {
            // Act
            val actual = integration.findBalanceTarget(address = UNLISTED_ADDRESS)

            // Assert
            assertThat(actual?.name).isEqualTo("cosmos...wxyz")
        }
    }

    @Nested
    inner class P2PEthPool {

        private val integration = p2pIntegration()

        @Test
        fun `GIVEN vault address WHEN findBalanceTarget THEN vault target`() {
            // Act
            val actual = integration.findBalanceTarget(address = MockStaking.VAULT_ADDRESS)

            // Assert
            assertThat(actual).isEqualTo(StakingTarget.Vault(MockStaking.vault()))
        }

        @Test
        fun `GIVEN unknown vault address WHEN findBalanceTarget THEN null`() {
            // Act & Assert
            assertThat(integration.findBalanceTarget(address = "0xunknown")).isNull()
        }
    }

    private companion object {
        const val UNLISTED_ADDRESS = "cosmosvaloper1unknownwxyz"
    }
}