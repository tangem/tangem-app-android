package com.tangem.features.staking.impl.presentation.state.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class StakingPendingActionUtilsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsSingleAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN network and pending actions WHEN isSingleAction THEN expected`(model: SingleActionModel) {
            // Act
            val actual = isSingleAction(model.blockchain.toNetworkId(), actions(*model.types.toTypedArray()))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SingleActionModel(Blockchain.Cosmos, emptyList(), expected = true),
            SingleActionModel(Blockchain.Cosmos, listOf(StakingActionType.CLAIM_REWARDS), expected = true),
            SingleActionModel(
                Blockchain.Cosmos,
                listOf(StakingActionType.CLAIM_REWARDS, StakingActionType.WITHDRAW),
                expected = false,
            ),
            SingleActionModel(Blockchain.Cosmos, listOf(StakingActionType.RESTAKE), expected = false),
            SingleActionModel(Blockchain.Cosmos, listOf(StakingActionType.STAKE), expected = false),
            SingleActionModel(
                Blockchain.Cosmos,
                listOf(StakingActionType.WITHDRAW, StakingActionType.UNSTAKE),
                expected = false,
            ),
            SingleActionModel(
                Blockchain.Solana,
                listOf(StakingActionType.WITHDRAW, StakingActionType.UNSTAKE),
                expected = true,
            ),
            SingleActionModel(
                Blockchain.Solana,
                listOf(StakingActionType.RESTAKE, StakingActionType.WITHDRAW),
                expected = true,
            ),
            SingleActionModel(
                Blockchain.Solana,
                listOf(StakingActionType.CLAIM_REWARDS, StakingActionType.UNSTAKE),
                expected = false,
            ),
            SingleActionModel(Blockchain.Tron, listOf(StakingActionType.REVOTE), expected = true),
            SingleActionModel(
                Blockchain.Tron,
                listOf(StakingActionType.VOTE_LOCKED, StakingActionType.UNLOCK_LOCKED),
                expected = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithStubUnstakeAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stub network and staked balance WHEN withStubUnstakeAction THEN stub unstake appended to passed actions`(
            blockchain: Blockchain,
        ) {
            // Arrange
            val activeStake = balanceState(
                type = BalanceType.STAKED,
                pendingActions = actions(StakingActionType.WITHDRAW),
            )

            // Act
            val actual = withStubUnstakeAction(
                networkId = blockchain.toNetworkId(),
                pendingActions = actions(StakingActionType.CLAIM_REWARDS),
                activeStake = activeStake,
            )

            // Assert
            assertThat(actual)
                .containsExactly(actions(StakingActionType.CLAIM_REWARDS).single(), STUB_UNSTAKE)
                .inOrder()
        }

        private fun provideTestModels() = listOf(Blockchain.BSC, Blockchain.Cardano, Blockchain.Cosmos)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithStubUnstakeActionPassedList {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no stub unstake needed WHEN withStubUnstakeAction THEN passed actions returned`(
            model: PassedListModel,
        ) {
            // Arrange
            val activeStake = balanceState(
                type = model.balanceType,
                pendingActions = actions(StakingActionType.CLAIM_REWARDS, StakingActionType.RESTAKE),
            )

            // Act
            val actual = withStubUnstakeAction(
                networkId = model.blockchain.toNetworkId(),
                pendingActions = actions(StakingActionType.CLAIM_REWARDS),
                activeStake = activeStake,
            )

            // Assert
            assertThat(actual).containsExactly(actions(StakingActionType.CLAIM_REWARDS).single())
        }

        private fun provideTestModels() = listOf(
            PassedListModel(Blockchain.Cosmos, BalanceType.REWARDS),
            PassedListModel(Blockchain.Cardano, BalanceType.REWARDS),
            PassedListModel(Blockchain.BSC, BalanceType.REWARDS),
            PassedListModel(Blockchain.Solana, BalanceType.STAKED),
            PassedListModel(Blockchain.Polygon, BalanceType.STAKED),
            PassedListModel(Blockchain.Polygon, BalanceType.REWARDS),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsTronStakedBalance {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN network and pending action WHEN isTronStakedBalance THEN expected`(model: TronStakedModel) {
            // Act
            val actual = isTronStakedBalance(
                networkId = model.blockchain.toNetworkId(),
                pendingAction = model.type?.let { MockStaking.pendingAction(type = it) },
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TronStakedModel(Blockchain.Tron, StakingActionType.REVOTE, expected = true),
            TronStakedModel(Blockchain.Tron, StakingActionType.VOTE, expected = false),
            TronStakedModel(Blockchain.Tron, null, expected = false),
            TronStakedModel(Blockchain.Solana, StakingActionType.REVOTE, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsCompositePendingActions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN network and pending actions WHEN isCompositePendingActions THEN expected`(model: CompositeModel) {
            // Act
            val actual = isCompositePendingActions(
                networkId = model.blockchain.toNetworkId(),
                pendingActions = model.types?.let { actions(*it.toTypedArray()).toPersistentList() },
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CompositeModel(Blockchain.Solana, listOf(StakingActionType.WITHDRAW), expected = true),
            CompositeModel(Blockchain.Solana, listOf(StakingActionType.CLAIM_REWARDS), expected = false),
            CompositeModel(Blockchain.Solana, null, expected = false),
            CompositeModel(Blockchain.Solana, emptyList(), expected = false),
            CompositeModel(Blockchain.Cosmos, listOf(StakingActionType.WITHDRAW), expected = false),
            CompositeModel(Blockchain.TON, listOf(StakingActionType.WITHDRAW), expected = false),
        )
    }

    internal data class SingleActionModel(
        val blockchain: Blockchain,
        val types: List<StakingActionType>,
        val expected: Boolean,
    )

    internal data class PassedListModel(val blockchain: Blockchain, val balanceType: BalanceType)

    internal data class TronStakedModel(val blockchain: Blockchain, val type: StakingActionType?, val expected: Boolean)

    internal data class CompositeModel(
        val blockchain: Blockchain,
        val types: List<StakingActionType>?,
        val expected: Boolean,
    )

    private fun actions(vararg types: StakingActionType): List<PendingAction> =
        types.map { MockStaking.pendingAction(type = it) }

    private companion object {
        val STUB_UNSTAKE = PendingAction(type = StakingActionType.UNSTAKE, passthrough = "", args = null)
    }
}