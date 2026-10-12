package com.tangem.features.staking.impl.presentation.state.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class RewardsValidatorStateConverterTest {

    private val jailedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = JAILED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.JAILED,
            name = "Jailed validator",
            preferred = false,
            isStrategicPartner = false,
        ),
    )

    private val listedTarget = StakingTarget.Validator(
        Yield.Validator(
            address = LISTED_ADDRESS,
            status = Yield.Validator.ValidatorStatus.ACTIVE,
            name = "Listed validator",
            preferred = true,
            isStrategicPartner = false,
        ),
    )

    private val integration: StakeKitIntegration = mockk {
        every { targets } returns listOf(listedTarget)
        every { allTargets } returns listOf(listedTarget, jailedTarget)
    }

    @Test
    fun `GIVEN rewards at jailed and unlisted validators WHEN convert THEN both rows keep their own validator`() {
        // Arrange
        val converter = createConverter(
            createRewardItem(validatorAddress = JAILED_ADDRESS),
            createRewardItem(validatorAddress = UNKNOWN_ADDRESS),
        )

        // Act
        val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

        // Assert
        assertThat(actual.rewards.map { it.target?.address }).containsExactly(JAILED_ADDRESS, UNKNOWN_ADDRESS).inOrder()
        assertThat(actual.rewards.first().target).isEqualTo(jailedTarget)
        assertThat(actual.rewards.last().target?.isPreferred).isFalse()
    }

    @Test
    fun `GIVEN claimable reward without validator address WHEN convert THEN row is kept at first listed validator`() {
        // Arrange
        val claimAction = PendingAction(type = StakingActionType.CLAIM_REWARDS, passthrough = "claim", args = null)
        val converter = createConverter(createRewardItem(validatorAddress = null, pendingActions = listOf(claimAction)))

        // Act
        val actual = converter.convert(Unit) as StakingStates.RewardsValidatorsState.Data

        // Assert
        assertThat(actual.rewards.map { it.target }).containsExactly(listedTarget)
        assertThat(actual.rewards.single().pendingActions).containsExactly(claimAction)
    }

    private fun createConverter(vararg items: BalanceItem): RewardsValidatorStateConverter {
        val stakingBalance: StakingBalance.Data.StakeKit = mockk {
            every { balance } returns mockk { every { this@mockk.items } returns items.toList() }
        }
        return RewardsValidatorStateConverter(
            cryptoCurrencyStatus = CryptoCurrencyStatus(
                currency = mockk(relaxed = true),
                value = mockk(relaxed = true) {
                    every { this@mockk.fiatRate } returns null
                    every { this@mockk.stakingBalance } returns stakingBalance
                    every { this@mockk.contributions } returns emptyList()
                },
            ),
            appCurrencyProvider = Provider { AppCurrency.Default },
            integration = integration,
        )
    }

    private fun createRewardItem(
        validatorAddress: String?,
        pendingActions: List<PendingAction> = emptyList(),
    ) = BalanceItem(
        groupId = "group-$validatorAddress",
        token = mockk(relaxed = true),
        type = BalanceType.REWARDS,
        amount = BigDecimal.ONE,
        rawCurrencyId = null,
        validatorAddress = validatorAddress,
        date = null,
        pendingActions = pendingActions,
        pendingActionsConstraints = emptyList(),
        isPending = false,
    )

    private companion object {
        const val LISTED_ADDRESS = "cosmosvaloper1listed"
        const val JAILED_ADDRESS = "cosmosvaloper1jailed"
        const val UNKNOWN_ADDRESS = "cosmosvaloper1unknownwxyz"
    }
}