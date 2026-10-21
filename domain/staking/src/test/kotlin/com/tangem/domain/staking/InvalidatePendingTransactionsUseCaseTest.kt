package com.tangem.domain.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InvalidatePendingTransactionsUseCaseTest {

    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = InvalidatePendingTransactionsUseCase(stakingErrorResolver = stakingErrorResolver)

    private val throwingActions = object : AbstractList<StakingAction>() {
        override val size: Int get() = throw IllegalStateException("boom")
        override fun get(index: Int): StakingAction = throw IllegalStateException("boom")
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    private fun balance(
        type: BalanceType,
        amount: String,
        token: YieldToken,
        validator: String? = MockStaking.VALIDATOR_ADDRESS,
        isPending: Boolean = false,
        groupId: String = "group-1",
    ) = MockStaking.balanceItem(
        type = type,
        amount = BigDecimal(amount),
        validatorAddress = validator,
        token = token,
        isPending = isPending,
        groupId = groupId,
    )

    private fun stub(amount: String, token: YieldToken, validator: String = MockStaking.VALIDATOR_ADDRESS) =
        MockStaking.balanceItem(
            type = BalanceType.STAKED,
            amount = BigDecimal(amount),
            validatorAddress = validator,
            token = token,
            isPending = true,
        )

    private fun action(
        type: StakingActionType,
        amount: String,
        status: StakingActionStatus = StakingActionStatus.PROCESSING,
        validatorAddress: String? = MockStaking.VALIDATOR_ADDRESS,
        validatorAddresses: List<String>? = null,
        id: String = "action-1",
    ) = MockStaking.stakingAction(
        type = type,
        status = status,
        amount = BigDecimal(amount),
        validatorAddress = validatorAddress,
        validatorAddresses = validatorAddresses,
        id = id,
    )

    private fun List<BalanceItem>.ignoringGroupId() = map { it.copy(groupId = "") }

    private fun assertBalances(actual: Either<StakingError, List<BalanceItem>>, vararg expected: BalanceItem) {
        assertThat(actual.getOrNull()!!.ignoringGroupId())
            .containsExactlyElementsIn(expected.toList().ignoringGroupId())
            .inOrder()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Common {

        private val token = MockStaking.solToken

        @Test
        fun `GIVEN no balances and no actions WHEN invoke THEN empty list returned`() {
            // Act
            val actual = useCase(balanceItems = emptyList(), stakingActions = emptyList(), token = token)

            // Assert
            assertThat(actual).isEqualTo(emptyList<BalanceItem>().right())
        }

        @ParameterizedTest
        @EnumSource(StakingActionStatus::class, names = ["PROCESSING"], mode = EnumSource.Mode.EXCLUDE)
        fun `GIVEN non processing STAKE action WHEN invoke THEN balances unchanged`(status: StakingActionStatus) {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "5", token))
            val actions = listOf(action(StakingActionType.STAKE, "2", status = status))

            // Act
            val actual = useCase(balanceItems = balances, stakingActions = actions, token = token)

            // Assert
            assertThat(actual).isEqualTo(balances.right())
        }

        @ParameterizedTest
        @EnumSource(
            StakingActionType::class,
            names = ["STAKE", "VOTE", "VOTE_LOCKED", "WITHDRAW", "UNLOCK_LOCKED", "RESTAKE", "UNSTAKE"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `GIVEN processing action of ignored type WHEN invoke THEN balances unchanged`(type: StakingActionType) {
            // Arrange
            val balances = listOf(
                balance(BalanceType.STAKED, "2", token),
                balance(BalanceType.UNSTAKED, "2", token),
                balance(BalanceType.LOCKED, "2", token),
            )
            val actions = listOf(action(type, "2"))

            // Act
            val actual = useCase(balanceItems = balances, stakingActions = actions, token = token)

            // Assert
            assertThat(actual).isEqualTo(balances.right())
        }

        @Test
        fun `GIVEN action list throws WHEN invoke THEN resolved error returned`() {
            // Act
            val actual = useCase(balanceItems = emptyList(), stakingActions = throwingActions, token = token)

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verify(exactly = 1) {
                stakingErrorResolver.resolve(match { it is IllegalStateException && it.message == "boom" })
            }
        }

        @Test
        fun `GIVEN two STAKE actions WHEN invoke THEN stubs have distinct uuid group ids`() {
            // Arrange
            val actions = listOf(
                action(StakingActionType.STAKE, "1", id = "action-1"),
                action(StakingActionType.STAKE, "2", id = "action-2"),
            )

            // Act
            val actual = useCase(balanceItems = emptyList(), stakingActions = actions, token = token).getOrNull()!!

            // Assert
            val groupIds = actual.map { it.groupId }
            groupIds.forEach { groupId ->
                assertThat(runCatching { UUID.fromString(groupId) }.exceptionOrNull()).isNull()
            }
            assertThat(groupIds.distinct()).hasSize(2)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Generic {

        private val token = MockStaking.solToken

        @Test
        fun `GIVEN STAKE action WHEN invoke THEN pending staked stub appended`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "5", token))
            val actions = listOf(action(StakingActionType.STAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "5", token), stub("2", token))
        }

        @Test
        fun `GIVEN STAKE action without validator address WHEN invoke THEN stub takes first of validator addresses`() {
            // Arrange
            val actions = listOf(
                action(
                    type = StakingActionType.STAKE,
                    amount = "2",
                    validatorAddress = null,
                    validatorAddresses = listOf("validator-2", "validator-3"),
                ),
            )

            // Act
            val actual = useCase(emptyList(), actions, token)

            // Assert
            assertBalances(actual, stub("2", token, "validator-2"))
        }

        @Test
        fun `GIVEN STAKE action without any validator WHEN invoke THEN stub validator address is empty`() {
            // Arrange
            val actions = listOf(action(StakingActionType.STAKE, "2", validatorAddress = null))

            // Act
            val actual = useCase(emptyList(), actions, token)

            // Assert
            assertBalances(actual, stub("2", token, ""))
        }

        @Test
        fun `GIVEN RESTAKE action WHEN invoke THEN matching staked balance marked pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "2", token))
            val actions = listOf(action(StakingActionType.RESTAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "2", token, isPending = true))
        }

        @Test
        fun `GIVEN balance amount with other scale WHEN RESTAKE invoked THEN still matched`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "2.0", token))
            val actions = listOf(action(StakingActionType.RESTAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "2.0", token, isPending = true))
        }

        @Test
        fun `GIVEN RESTAKE with no equal amount WHEN invoke THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "3", token))
            val actions = listOf(action(StakingActionType.RESTAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "3", token))
        }

        @Test
        fun `GIVEN already pending balance WHEN RESTAKE invoked THEN next non pending balance marked`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.STAKED, "2", token, isPending = true),
                balance(BalanceType.STAKED, "2", token, groupId = "group-2"),
            )
            val actions = listOf(action(StakingActionType.RESTAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.STAKED, "2", token, isPending = true),
                balance(BalanceType.STAKED, "2", token, isPending = true, groupId = "group-2"),
            )
        }

        @Test
        fun `GIVEN two RESTAKE actions WHEN invoke THEN two equal balances marked one by one`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.STAKED, "2", token),
                balance(BalanceType.STAKED, "2", token, groupId = "group-2"),
            )
            val actions = listOf(
                action(StakingActionType.RESTAKE, "2", id = "action-1"),
                action(StakingActionType.RESTAKE, "2", id = "action-2"),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.STAKED, "2", token, isPending = true),
                balance(BalanceType.STAKED, "2", token, isPending = true, groupId = "group-2"),
            )
        }

        @Test
        fun `GIVEN other type balance with equal amount listed first WHEN RESTAKE invoked THEN only staked balance marked pending`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.UNSTAKED, "2", token),
                balance(BalanceType.STAKED, "2", token),
            )
            val actions = listOf(action(StakingActionType.RESTAKE, "2"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.UNSTAKED, "2", token),
                balance(BalanceType.STAKED, "2", token, isPending = true),
            )
        }

        @Test
        fun `GIVEN WITHDRAW action WHEN invoke THEN only unstaked balance marked pending`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.UNSTAKED, "1", token),
                balance(BalanceType.PREPARING, "1", token),
            )
            val actions = listOf(action(StakingActionType.WITHDRAW, "1"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.UNSTAKED, "1", token, isPending = true),
                balance(BalanceType.PREPARING, "1", token),
            )
        }

        @Test
        fun `GIVEN UNLOCK_LOCKED action WHEN invoke THEN matching locked balance marked pending`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.LOCKED, "4", token),
                balance(BalanceType.STAKED, "4", token),
            )
            val actions = listOf(action(StakingActionType.UNLOCK_LOCKED, "4"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.LOCKED, "4", token, isPending = true),
                balance(BalanceType.STAKED, "4", token),
            )
        }

        @Test
        fun `GIVEN full UNSTAKE action WHEN invoke THEN matching staked balance marked pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "3", token))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "3", token, isPending = true))
        }

        @Test
        fun `GIVEN processing and non processing actions WHEN invoke THEN only processing applied`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "2", token))
            val actions = listOf(
                action(StakingActionType.RESTAKE, "2", status = StakingActionStatus.SUCCESS, id = "action-1"),
                action(StakingActionType.STAKE, "1", id = "action-2"),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "2", token), stub("1", token))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tron {

        private val token = MockStaking.trxToken
        private val validators = listOf("validator-1", "validator-2")

        @Test
        fun `GIVEN tron VOTE action WHEN invoke THEN stub staked on first voted validator`() {
            // Arrange
            val actions = listOf(
                action(StakingActionType.VOTE, "10", validatorAddress = null, validatorAddresses = validators),
            )

            // Act
            val actual = useCase(emptyList(), actions, token)

            // Assert
            assertBalances(actual, stub("10", token, "validator-1"))
        }

        @Test
        fun `GIVEN tron VOTE_LOCKED action WHEN invoke THEN locked balance removed and staked stub added`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.LOCKED, "3", token),
                balance(BalanceType.STAKED, "5", token),
            )
            val actions = listOf(
                action(StakingActionType.VOTE_LOCKED, "3", validatorAddress = null, validatorAddresses = validators),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "5", token), stub("3", token))
        }

        @Test
        fun `GIVEN tron VOTE_LOCKED without equal locked amount WHEN invoke THEN only stub added`() {
            // Arrange
            val balances = listOf(balance(BalanceType.LOCKED, "4", token))
            val actions = listOf(
                action(StakingActionType.VOTE_LOCKED, "3", validatorAddress = null, validatorAddresses = validators),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.LOCKED, "4", token), stub("3", token))
        }

        @Test
        fun `GIVEN two equal locked balances WHEN VOTE_LOCKED invoked THEN only first removed`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.LOCKED, "3", token, groupId = "group-1"),
                balance(BalanceType.LOCKED, "3", token, groupId = "group-2"),
            )
            val actions = listOf(
                action(StakingActionType.VOTE_LOCKED, "3", validatorAddress = null, validatorAddresses = validators),
            )

            // Act
            val actual = useCase(balances, actions, token).getOrNull()!!

            // Assert
            assertThat(actual).hasSize(2)
            assertThat(actual[0]).isEqualTo(balance(BalanceType.LOCKED, "3", token, groupId = "group-2"))
            assertThat(actual[1].copy(groupId = "")).isEqualTo(stub("3", token).copy(groupId = ""))
        }

        // Pins current behaviour: partial unstake compares only action.validatorAddress, Tron actions carry validatorAddresses
        @Test
        fun `GIVEN tron partial UNSTAKE with validator list only WHEN invoke THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "10", token))
            val actions = listOf(
                action(StakingActionType.UNSTAKE, "3", validatorAddress = null, validatorAddresses = validators),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "10", token))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Ton {

        private val token = MockStaking.tonToken

        @Test
        fun `GIVEN ton WITHDRAW without validator WHEN invoke THEN unstaked and preparing marked pending`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.UNSTAKED, "1", token),
                balance(BalanceType.PREPARING, "1", token),
                balance(BalanceType.STAKED, "1", token),
            )
            val actions = listOf(
                action(StakingActionType.WITHDRAW, "1", validatorAddress = null, validatorAddresses = null),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.UNSTAKED, "1", token, isPending = true),
                balance(BalanceType.PREPARING, "1", token, isPending = true),
                balance(BalanceType.STAKED, "1", token),
            )
        }

        // Pins current behaviour: TON marks every balance of the action validator pending, not only the matched one
        @Test
        fun `GIVEN ton STAKE on validator WHEN invoke THEN all balances of that validator and stub pending`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.STAKED, "5", token, validator = "validator-1"),
                balance(BalanceType.REWARDS, "1", token, validator = "validator-1"),
                balance(BalanceType.STAKED, "5", token, validator = "validator-2"),
            )
            val actions = listOf(action(StakingActionType.STAKE, "2", validatorAddress = "validator-1"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.STAKED, "5", token, validator = "validator-1", isPending = true),
                balance(BalanceType.REWARDS, "1", token, validator = "validator-1", isPending = true),
                balance(BalanceType.STAKED, "5", token, validator = "validator-2"),
                stub("2", token),
            )
        }

        @Test
        fun `GIVEN ton action with validator list only WHEN invoke THEN first listed validator balances pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "5", token, validator = "validator-2"))
            val actions = listOf(
                action(
                    type = StakingActionType.RESTAKE,
                    amount = "7",
                    validatorAddress = null,
                    validatorAddresses = listOf("validator-2"),
                ),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "5", token, validator = "validator-2", isPending = true))
        }

        @Test
        fun `GIVEN ton action and balance both without validator WHEN invoke THEN balance not marked pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "5", token, validator = null))
            val actions = listOf(
                action(StakingActionType.CLAIM_REWARDS, "1", validatorAddress = null, validatorAddresses = null),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "5", token, validator = null))
        }

        @Test
        fun `GIVEN ton CLAIM_REWARDS action WHEN invoke THEN validator balances still marked pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.REWARDS, "1", token, validator = "validator-1"))
            val actions = listOf(action(StakingActionType.CLAIM_REWARDS, "1", validatorAddress = "validator-1"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.REWARDS, "1", token, validator = "validator-1", isPending = true))
        }

        @Test
        fun `GIVEN ton action without any validator WHEN invoke THEN no post processing`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "5", token, validator = "validator-1"))
            val actions = listOf(
                action(StakingActionType.CLAIM_REWARDS, "1", validatorAddress = null, validatorAddresses = null),
            )

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "5", token, validator = "validator-1"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PartialUnstake {

        private val token = MockStaking.solToken

        // Pins current behaviour: the pending part reuses the groupId of the real balance
        @Test
        fun `GIVEN staked 10 and unstake 3 on same validator WHEN invoke THEN split into 7 real and 3 pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "10", token, validator = "validator-1"))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3", validatorAddress = "validator-1"))

            // Act
            val actual = useCase(balances, actions, token).getOrNull()!!

            // Assert
            assertThat(actual)
                .containsExactly(
                    balance(BalanceType.STAKED, "7", token, validator = "validator-1", groupId = "group-1"),
                    balance(
                        BalanceType.STAKED,
                        "3",
                        token,
                        validator = "validator-1",
                        groupId = "group-1",
                        isPending = true,
                    ),
                )
                .inOrder()
        }

        // Pins current behaviour: partial unstake matches the validator
        @Test
        fun `GIVEN partial unstake on another validator WHEN invoke THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "10", token, validator = "validator-2"))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3", validatorAddress = "validator-1"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "10", token, validator = "validator-2"))
        }

        // Pins current behaviour: full unstake ignores the validator
        @Test
        fun `GIVEN full unstake on another validator WHEN invoke THEN that balance marked pending`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "3", token, validator = "validator-2"))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3", validatorAddress = "validator-1"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.STAKED, "3", token, validator = "validator-2", isPending = true),
            )
        }

        @Test
        fun `GIVEN unstake amount above balance WHEN invoke THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "2", token))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "2", token))
        }

        @Test
        fun `GIVEN larger non staked balance on same validator WHEN partial unstake invoked THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.UNSTAKED, "10", token))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.UNSTAKED, "10", token))
        }

        @Test
        fun `GIVEN only pending staked balance WHEN partial unstake invoked THEN balances unchanged`() {
            // Arrange
            val balances = listOf(balance(BalanceType.STAKED, "10", token, isPending = true))
            val actions = listOf(action(StakingActionType.UNSTAKE, "3"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(actual, balance(BalanceType.STAKED, "10", token, isPending = true))
        }

        @Test
        fun `GIVEN equal and larger staked balances WHEN unstake invoked THEN equal one marked as full unstake`() {
            // Arrange
            val balances = listOf(
                balance(BalanceType.STAKED, "10", token, groupId = "group-1"),
                balance(BalanceType.STAKED, "3", token, groupId = "group-2"),
            )
            val actions = listOf(action(StakingActionType.UNSTAKE, "3"))

            // Act
            val actual = useCase(balances, actions, token)

            // Assert
            assertBalances(
                actual,
                balance(BalanceType.STAKED, "10", token, groupId = "group-1"),
                balance(BalanceType.STAKED, "3", token, groupId = "group-2", isPending = true),
            )
        }
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}