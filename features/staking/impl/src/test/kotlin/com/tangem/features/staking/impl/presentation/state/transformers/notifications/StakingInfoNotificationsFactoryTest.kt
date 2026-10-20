package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.initialInfoData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.target
import com.tangem.features.staking.impl.presentation.state.utils.toTextReference
import com.tangem.features.staking.impl.presentation.state.yieldBalanceData
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class StakingInfoNotificationsFactoryTest {

    private val onReduce: (BigDecimal, Class<out NotificationUM>) -> Unit = mockk(relaxed = true)

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
    private val solanaStatus = currencyStatus()
    private val solanaIntegration = stakeKitIntegration()

    @BeforeEach
    fun resetMocks() {
        clearMocks(onReduce)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Common {

        private val exitMinimumIntegration = stakeKitIntegration(
            yield = MockStaking.yield(exit = MockStaking.enterArgs(minimum = BigDecimal.ONE)),
        )

        @Test
        fun `GIVEN exit leaving less than exit minimum WHEN add THEN low staked balance first`() {
            // Act
            val actual = collect(
                integration = exitMinimumIntegration,
                state = exitState(stakedAmount = BigDecimal("5")),
                action = BigDecimal("4.5"),
            )

            // Assert
            assertThat(actual.first()).isEqualTo(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN full exit WHEN add THEN no low staked balance`() {
            // Act
            val actual = collect(
                integration = exitMinimumIntegration,
                state = exitState(stakedAmount = BigDecimal("5")),
                action = BigDecimal("5"),
            )

            // Assert
            assertThat(actual).doesNotContain(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN remainder equal to exit minimum WHEN add THEN no low staked balance`() {
            // Act
            val actual = collect(
                integration = exitMinimumIntegration,
                state = exitState(stakedAmount = BigDecimal("5")),
                action = BigDecimal("4"),
            )

            // Assert
            assertThat(actual).doesNotContain(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN exit args absent WHEN add THEN no low staked balance`() {
            // Act
            val actual = collect(
                integration = stakeKitIntegration(yield = MockStaking.yield(exit = null)),
                state = exitState(stakedAmount = BigDecimal("5")),
                action = BigDecimal("4.5"),
            )

            // Assert
            assertThat(actual).doesNotContain(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN exit without balance state WHEN add THEN no low staked balance`() {
            // Act
            val actual = collect(
                integration = exitMinimumIntegration,
                state = stakingUiState(actionType = exit),
                action = BigDecimal("4.5"),
            )

            // Assert
            assertThat(actual).doesNotContain(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN enter WHEN add THEN no low staked balance`() {
            // Act
            val actual = collect(
                integration = exitMinimumIntegration,
                state = exitState(stakedAmount = BigDecimal("5")).copy(actionType = enter),
                action = BigDecimal("4.5"),
            )

            // Assert
            assertThat(actual).doesNotContain(StakingNotification.Warning.LowStakedBalance)
        }

        @Test
        fun `GIVEN exit with cooldown WHEN add THEN unstake notification with default text`() {
            // Act
            val actual = collect(state = stakingUiState(actionType = exit))

            // Assert
            assertThat(actual).contains(
                StakingNotification.Info.Unstake(
                    cooldownPeriod = CooldownPeriod.Fixed(Period.Days(3)),
                    subtitleRes = R.string.staking_notification_unstake_text,
                ),
            )
        }

        @Test
        fun `GIVEN exit without cooldown WHEN add THEN no unstake notification`() {
            // Arrange
            val integration = stakeKitIntegration(
                yield = MockStaking.yield(metadata = MockStaking.metadata(cooldownPeriod = null)),
            )

            // Act
            val actual = collect(integration = integration, state = stakingUiState(actionType = exit))

            // Assert
            assertThat(actual.filterIsInstance<StakingNotification.Info.Unstake>()).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN pending action type WHEN add THEN pending info notification`(model: PendingModel) {
            // Arrange
            val state = stakingUiState(
                actionType = StakingActionCommonType.Pending.Other,
                confirmationState = model.type?.let { confirmationData(pendingAction = MockStaking.pendingAction(it)) }
                    ?: StakingStates.ConfirmationState.Empty(),
            )

            // Act
            val actual = collect(integration = model.integration, state = state)

            // Assert
            if (model.expected == null) {
                assertThat(actual).isEmpty()
            } else {
                assertThat(actual).contains(model.expected)
            }
        }

        private fun provideTestModels(): List<PendingModel> {
            val noCooldown = stakeKitIntegration(
                yield = MockStaking.yield(metadata = MockStaking.metadata(cooldownPeriod = null)),
            )
            return listOf(
                PendingModel(
                    StakingActionType.CLAIM_REWARDS,
                    ordinary(R.string.common_claim, R.string.staking_notification_claim_rewards_text),
                ),
                PendingModel(
                    StakingActionType.RESTAKE_REWARDS,
                    ordinary(R.string.staking_restake, R.string.staking_notification_restake_rewards_text),
                ),
                PendingModel(
                    StakingActionType.WITHDRAW,
                    ordinary(R.string.staking_withdraw, R.string.staking_notification_withdraw_text),
                ),
                PendingModel(
                    StakingActionType.CLAIM_UNSTAKED,
                    ordinary(R.string.staking_withdraw, R.string.staking_notification_withdraw_text),
                ),
                PendingModel(
                    StakingActionType.UNLOCK_LOCKED,
                    StakingNotification.Info.Ordinary(
                        title = resourceReference(R.string.staking_unlocked_locked),
                        text = resourceReference(
                            R.string.staking_notification_unlock_text,
                            wrappedList(CooldownPeriod.Fixed(Period.Days(3)).toTextReference()),
                        ),
                    ),
                ),
                PendingModel(
                    StakingActionType.VOTE_LOCKED,
                    ordinary(R.string.staking_revote, R.string.staking_notifications_revote_tron_text),
                ),
                PendingModel(
                    StakingActionType.RESTAKE,
                    ordinary(R.string.staking_restake, R.string.staking_notification_restake_text),
                ),
                PendingModel(StakingActionType.UNLOCK_LOCKED, expected = null, integration = noCooldown),
                PendingModel(StakingActionType.STAKE, expected = null),
                PendingModel(StakingActionType.UNSTAKE, expected = null),
                PendingModel(StakingActionType.VOTE, expected = null),
                PendingModel(StakingActionType.UNKNOWN, expected = null),
                PendingModel(type = null, expected = null),
            )
        }

        private fun exitState(stakedAmount: BigDecimal) =
            stakingUiState(actionType = exit, balanceState = balanceState(cryptoAmount = stakedAmount))
    }

    @Nested
    inner class EntireBalance {

        private val balance = currencyStatus(amount = BigDecimal("10.00"))
        private val fee = ethFee(BigDecimal("0.10")).amount

        // Pins current behaviour: the reduce button offers three times the fee, not the fee itself.
        @Test
        fun `GIVEN sending plus fee equals balance WHEN add THEN stake entire balance with reduce button`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("9.90"), fee = fee)

            // Assert
            val notification = actual.single()
            assertThat(notification).isInstanceOf(StakingNotification.Info.StakeEntireBalance::class.java)
            assertThat(reduceButton(notification).text).isEqualTo(reduceText(isFeeApproximate = false))
        }

        // Pins current behaviour: the reduce callback receives the tripled fee.
        @Test
        fun `GIVEN entire balance WHEN reduce clicked THEN callback with tripled fee`() {
            // Arrange
            val notification = collectEntire(sending = BigDecimal("9.90"), fee = fee).single()

            // Act
            reduceButton(notification).onClick()

            // Assert
            verify(exactly = 1) {
                onReduce(
                    match { it.compareTo(BigDecimal("0.3")) == 0 },
                    StakingNotification.Info.StakeEntireBalance::class.java,
                )
            }
        }

        @Test
        fun `GIVEN approximate fee WHEN add THEN reduce text uses can be lower fee format`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("9.90"), fee = fee, isFeeApproximate = true)

            // Assert
            assertThat(reduceButton(actual.single()).text).isEqualTo(reduceText(isFeeApproximate = true))
        }

        @Test
        fun `GIVEN no fee WHEN add THEN entire balance without button`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("10.00"), fee = null)

            // Assert
            val notification = actual.single()
            assertThat(notification).isInstanceOf(StakingNotification.Info.StakeEntireBalance::class.java)
            assertThat(notification.config.buttonsState).isNull()
        }

        // Pins current behaviour: balance and sum are compared with equals, so 10 and 10.00 differ.
        @Test
        fun `GIVEN same value different scale WHEN add THEN entire balance not detected`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("9.90"), fee = fee, status = currencyStatus())

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN subtract unavailable WHEN add THEN no entire balance`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("9.90"), fee = fee, isSubtract = false)

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN sending below balance WHEN add THEN no entire balance`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("5"), fee = fee)

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN zero fee WHEN entire balance staked THEN entire balance without reduce button`() {
            // Act
            val actual = collectEntire(sending = BigDecimal("10.00"), fee = ethFee(BigDecimal.ZERO).amount)

            // Assert
            val notification = actual.single()
            assertThat(notification).isInstanceOf(StakingNotification.Info.StakeEntireBalance::class.java)
            assertThat(notification.config.buttonsState).isNull()
        }

        private fun collectEntire(
            sending: BigDecimal,
            fee: Amount?,
            isFeeApproximate: Boolean = false,
            isSubtract: Boolean = true,
            status: CryptoCurrencyStatus = balance,
        ) = collect(
            status = status,
            sending = sending,
            fee = fee,
            isSubtract = isSubtract,
            isFeeApproximate = isFeeApproximate,
        )

        private fun reduceText(isFeeApproximate: Boolean) = resourceReference(
            R.string.send_notification_reduce_by,
            wrappedList(
                BigDecimal("0.30").format {
                    crypto(symbol = "SOL", decimals = 9).fee(canBeLower = isFeeApproximate)
                },
            ),
        )

        private fun reduceButton(notification: NotificationUM) =
            notification.config.buttonsState as NotificationConfig.ButtonsState.PrimaryButtonConfig
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tron {

        private val tron = coin(Blockchain.Tron)
        private val tronIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron)
        private val revote = ordinary(R.string.staking_revote, R.string.staking_notifications_revote_tron_text)

        @ParameterizedTest
        @EnumSource(value = BalanceType::class, names = ["STAKED", "PREPARING", "LOCKED"])
        fun `GIVEN tron with staked balance WHEN add THEN revote notification`(type: BalanceType) {
            // Act
            val actual = collect(status = tronStatus(type), integration = tronIntegration)

            // Assert
            assertThat(actual).contains(revote)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN tron with staked balance and non enter action WHEN add THEN no revote`(model: NonEnterActionModel) {
            // Act
            val actual = collect(
                status = tronStatus(BalanceType.STAKED),
                integration = tronIntegration,
                state = stakingUiState(actionType = model.actionType),
            )

            // Assert
            assertThat(actual).doesNotContain(revote)
        }

        private fun provideTestModels() = listOf(
            NonEnterActionModel(exit),
            NonEnterActionModel(StakingActionCommonType.Pending.Other),
            NonEnterActionModel(StakingActionCommonType.Pending.Rewards),
        )

        @Test
        fun `GIVEN tron with only unstaking balance WHEN add THEN no revote`() {
            // Act
            val actual = collect(status = tronStatus(BalanceType.UNSTAKING), integration = tronIntegration)

            // Assert
            assertThat(actual).doesNotContain(revote)
        }

        @Test
        fun `GIVEN tron without staking balance WHEN add THEN no revote`() {
            // Act
            val actual = collect(status = currencyStatus(tron), integration = tronIntegration)

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN solana with staked balance WHEN add THEN no revote`() {
            // Act
            val actual = collect(status = currencyStatus(stakingBalance = MockStaking.stakeKitBalance()))

            // Assert
            assertThat(actual).doesNotContain(revote)
        }

        private fun tronStatus(type: BalanceType) = currencyStatus(
            currency = tron,
            stakingBalance = MockStaking.stakeKitBalance(
                items = listOf(MockStaking.balanceItem(type = type, token = MockStaking.trxToken)),
                integrationId = MockStaking.TRON_ID,
            ),
        )
    }

    @Nested
    inner class Cardano {

        private val cardano = coin(Blockchain.Cardano)
        private val cardanoIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cardano)
        private val deposit = ordinary(
            R.string.staking_notification_additional_ada_deposit_title,
            R.string.staking_notification_additional_ada_deposit_text,
        )
        private val fee = ethFee(BigDecimal("0.1"), symbol = "ADA", decimals = 6).amount

        @Test
        fun `GIVEN cardano enter with balance minus fee below five WHEN add THEN minimum stake error then deposit info`() {
            // Act
            val actual = collect(status = cardanoStatus("5"), integration = cardanoIntegration, fee = fee)

            // Assert
            assertThat(actual).containsExactly(
                StakingNotification.Error.MinimumAmountNotReachedError(
                    title = resourceReference(R.string.staking_notification_minimum_balance_title),
                    subtitle = resourceReference(R.string.staking_notification_minimum_stake_ada_text),
                ),
                deposit,
            ).inOrder()
        }

        @Test
        fun `GIVEN cardano enter with balance minus fee exactly five WHEN add THEN only deposit info`() {
            // Act
            val actual = collect(status = cardanoStatus("5.1"), integration = cardanoIntegration, fee = fee)

            // Assert
            assertThat(actual).containsExactly(deposit)
        }

        @Test
        fun `GIVEN cardano pending with balance minus fee exactly three WHEN add THEN no restake error`() {
            // Act
            val actual = collect(
                status = cardanoStatus("3.1"),
                integration = cardanoIntegration,
                state = stakingUiState(actionType = StakingActionCommonType.Pending.Other),
                fee = fee,
            )

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN cardano entire balance WHEN add THEN no stake entire balance`() {
            // Act
            val actual = collect(
                status = cardanoStatus("10.00"),
                integration = cardanoIntegration,
                sending = BigDecimal("9.90"),
                fee = ethFee(BigDecimal("0.10"), symbol = "ADA", decimals = 6).amount,
                isSubtract = true,
            )

            // Assert
            assertThat(actual.filterIsInstance<StakingNotification.Info.StakeEntireBalance>()).isEmpty()
        }

        @Test
        fun `GIVEN cardano pending with balance minus fee below three WHEN add THEN minimum restake error`() {
            // Act
            val actual = collect(
                status = cardanoStatus("3"),
                integration = cardanoIntegration,
                state = stakingUiState(actionType = StakingActionCommonType.Pending.Other),
                fee = fee,
            )

            // Assert
            assertThat(actual).containsExactly(
                StakingNotification.Error.MinimumAmountNotReachedError(
                    title = resourceReference(R.string.staking_notification_minimum_restake_ada_title),
                    subtitle = resourceReference(R.string.staking_notification_minimum_restake_ada_text),
                ),
            )
        }

        @Test
        fun `GIVEN non cardano pending with low balance WHEN add THEN no restake error`() {
            // Act
            val actual = collect(
                status = currencyStatus(amount = BigDecimal("1")),
                state = stakingUiState(actionType = StakingActionCommonType.Pending.Other),
                fee = fee,
            )

            // Assert
            assertThat(actual).isEmpty()
        }

        private fun cardanoStatus(amount: String) = currencyStatus(cardano, amount = BigDecimal(amount))
    }

    @Nested
    inner class Cosmos {

        @Test
        fun `GIVEN cosmos exit WHEN add THEN unstake notification with cosmos text`() {
            // Act
            val actual = collect(
                status = currencyStatus(coin(Blockchain.Cosmos)),
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cosmos),
                state = stakingUiState(actionType = exit),
            )

            // Assert
            assertThat(actual).contains(
                StakingNotification.Info.Unstake(
                    cooldownPeriod = CooldownPeriod.Fixed(Period.Days(3)),
                    subtitleRes = R.string.staking_notification_unstake_cosmos_text,
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Ton {

        private val ton = coin(Blockchain.TON)
        private val tonIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Ton)
        private val extraReserve = ordinary(
            R.string.staking_notification_ton_extra_reserve_title,
            R.string.staking_notification_ton_extra_reserve_info,
        )
        private val unstakeAll = ordinary(
            R.string.staking_notification_ton_have_to_unstake_all_title,
            R.string.staking_notification_ton_have_to_unstake_all_text,
        )
        private val first = target()
        private val second = target(MockStaking.validator(MockStaking.SECOND_VALIDATOR_ADDRESS, name = "Validator 2"))

        @Test
        fun `GIVEN ton balance at threshold WHEN add THEN extra reserve info`() {
            // Act
            val actual = collect(status = currencyStatus(ton, amount = BigDecimal("0.2")), integration = tonIntegration)

            // Assert
            assertThat(actual).contains(extraReserve)
        }

        @Test
        fun `GIVEN ton balance below threshold WHEN add THEN no extra reserve info`() {
            // Act
            val actual = collect(status = currencyStatus(ton, amount = BigDecimal("0.1")), integration = tonIntegration)

            // Assert
            assertThat(actual).doesNotContain(extraReserve)
        }

        @Test
        fun `GIVEN solana above threshold WHEN add THEN no extra reserve info`() {
            // Act
            val actual = collect(status = currencyStatus(amount = BigDecimal("10")))

            // Assert
            assertThat(actual).doesNotContain(extraReserve)
        }

        @Test
        fun `GIVEN ton exit with two stakes at same validator WHEN add THEN have to unstake all`() {
            // Act
            val actual = collectTon(
                exit,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.PREPARING, target = first, groupId = "2"),
            )

            // Assert
            assertThat(actual).contains(unstakeAll)
        }

        @Test
        fun `GIVEN ton pending with staked and unstaked at same validator WHEN add THEN have to unstake all`() {
            // Act
            val actual = collectTon(
                StakingActionCommonType.Pending.Other,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.UNSTAKED, target = first, groupId = "2"),
            )

            // Assert
            assertThat(actual).contains(unstakeAll)
        }

        @Test
        fun `GIVEN ton exit with one stake at validator WHEN add THEN no have to unstake all`() {
            // Act
            val actual = collectTon(
                exit,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.STAKED, target = second, groupId = "2"),
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        @Test
        fun `GIVEN ton exit with unstaking entries at validator WHEN add THEN no have to unstake all`() {
            // Act
            val actual = collectTon(
                exit,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.UNSTAKING, target = first, groupId = "2"),
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        @Test
        fun `GIVEN ton exit without balance target WHEN add THEN no have to unstake all`() {
            // Act
            val actual = collectTon(
                exit,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.PREPARING, target = first, groupId = "2"),
                selected = null,
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        @Test
        fun `GIVEN ton enter WHEN add THEN no have to unstake all`() {
            // Act
            val actual = collectTon(
                enter,
                balanceState(BalanceType.STAKED, target = first),
                balanceState(BalanceType.PREPARING, target = first, groupId = "2"),
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        @Test
        fun `GIVEN solana exit with two stakes at same validator WHEN add THEN no have to unstake all`() {
            // Act
            val actual = collect(
                state = stakingUiState(
                    actionType = exit,
                    balanceState = balanceState(target = first),
                    initialInfoState = initialInfoData(
                        yieldBalance = yieldBalanceData(
                            listOf(
                                balanceState(BalanceType.STAKED, target = first),
                                balanceState(BalanceType.PREPARING, target = first, groupId = "2"),
                            ),
                        ),
                    ),
                ),
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN ton exit without yield balance data WHEN add THEN no have to unstake all`(model: TonNoBalancesModel) {
            // Act
            val actual = collect(
                status = currencyStatus(ton),
                integration = tonIntegration,
                state = stakingUiState(
                    actionType = exit,
                    balanceState = balanceState(target = first),
                    initialInfoState = model.initialInfoState,
                ),
            )

            // Assert
            assertThat(actual).doesNotContain(unstakeAll)
        }

        private fun provideTestModels() = listOf(
            TonNoBalancesModel(initialInfoState = StakingStates.InitialInfoState.Empty()),
            TonNoBalancesModel(initialInfoState = initialInfoData()),
        )

        private fun collectTon(
            actionType: StakingActionCommonType,
            vararg balances: BalanceState,
            selected: BalanceState? = balanceState(target = first),
        ) = collect(
            status = currencyStatus(ton),
            integration = tonIntegration,
            state = stakingUiState(
                actionType = actionType,
                balanceState = selected,
                initialInfoState = initialInfoData(yieldBalance = yieldBalanceData(balances.toList())),
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun collect(
        status: CryptoCurrencyStatus = solanaStatus,
        integration: StakingIntegration = solanaIntegration,
        state: StakingUiState = stakingUiState(actionType = enter),
        sending: BigDecimal = BigDecimal.ZERO,
        action: BigDecimal = BigDecimal.ZERO,
        fee: Amount? = null,
        isSubtract: Boolean = false,
        isFeeApproximate: Boolean = false,
    ): List<NotificationUM> {
        val notifications = mutableListOf<NotificationUM>()
        StakingInfoNotificationsFactory(
            cryptoCurrencyStatusProvider = Provider { status },
            integration = integration,
            isSubtractAvailable = isSubtract,
        ).addInfoNotifications(
            notifications = notifications,
            prevState = state,
            sendingAmount = sending,
            actionAmount = action,
            feeAmount = fee,
            tonBalanceExtraFeeThreshold = BigDecimal("0.2"),
            isFeeApproximate = isFeeApproximate,
            onAmountReduceByFeeClick = onReduce,
        )
        return notifications
    }

    private fun ordinary(title: Int, text: Int) =
        StakingNotification.Info.Ordinary(title = resourceReference(title), text = resourceReference(text))

    internal data class TonNoBalancesModel(val initialInfoState: StakingStates.InitialInfoState)

    internal data class NonEnterActionModel(val actionType: StakingActionCommonType)

    internal data class PendingModel(
        val type: StakingActionType?,
        val expected: NotificationUM?,
        val integration: StakingIntegration = stakeKitIntegration(),
    )
}