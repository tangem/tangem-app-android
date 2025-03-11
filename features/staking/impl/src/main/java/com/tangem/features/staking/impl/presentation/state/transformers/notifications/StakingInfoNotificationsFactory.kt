package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.lib.crypto.BlockchainUtils.isCosmos
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.Provider
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class StakingInfoNotificationsFactory(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val yield: Yield,
    private val isSubtractAvailable: Boolean,
) {

    /**
     * @param notifications current notification to display
     * @param prevState     current screen state to update
     * @param sendingAmount amount being transferred from user account
     * @param actionAmount  any amount being transferred or used action
     * @param feeValue      fee amount payed from user account
     */
    fun addInfoNotifications(
        notifications: MutableList<NotificationUM>,
        prevState: StakingUiState,
        sendingAmount: BigDecimal,
        actionAmount: BigDecimal,
        feeValue: BigDecimal,
    ) = with(notifications) {
        addStakingLowBalanceNotification(prevState, actionAmount)

        when (prevState.actionType) {
            StakingActionCommonType.Enter -> addEnterInfoNotifications(sendingAmount, feeValue)
            is StakingActionCommonType.Exit -> addExitInfoNotifications()
            is StakingActionCommonType.Pending -> addPendingInfoNotifications(prevState)
        }
    }

    private fun MutableList<NotificationUM>.addExitInfoNotifications() {
        val cooldownPeriodDays = yield.metadata.cooldownPeriod?.days
        if (cooldownPeriodDays != null) {
            add(
                StakingNotification.Info.Unstake(
                    cooldownPeriodDays = cooldownPeriodDays,
                    subtitleRes = if (isCosmos(cryptoCurrencyStatusProvider().currency.network.id.value)) {
                        R.string.staking_notification_unstake_cosmos_text
                    } else {
                        R.string.staking_notification_unstake_text
                    },
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addEnterInfoNotifications(
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
    ) {
        addTronRevoteNotification()
        addStakingEntireBalanceNotification(sendingAmount, feeValue)
    }

    private fun MutableList<NotificationUM>.addPendingInfoNotifications(prevState: StakingUiState) {
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val pendingActionType = confirmationState?.pendingAction?.type
        val (titleReference, textReference) = when (pendingActionType) {
            StakingActionType.CLAIM_REWARDS -> {
                resourceReference(R.string.common_claim) to
                    resourceReference(R.string.staking_notification_claim_rewards_text)
            }
            StakingActionType.RESTAKE_REWARDS -> {
                resourceReference(R.string.staking_restake) to
                    resourceReference(R.string.staking_notification_restake_rewards_text)
            }
            StakingActionType.CLAIM_UNSTAKED,
            StakingActionType.WITHDRAW,
            -> {
                resourceReference(R.string.staking_withdraw) to
                    resourceReference(R.string.staking_notification_withdraw_text)
            }
            StakingActionType.UNLOCK_LOCKED -> {
                val cooldownPeriodDays = yield.metadata.cooldownPeriod?.days
                if (cooldownPeriodDays != null) {
                    resourceReference(R.string.staking_unlocked_locked) to resourceReference(
                        R.string.staking_notification_unlock_text,
                        wrappedList(
                            pluralReference(
                                id = R.plurals.common_days,
                                count = cooldownPeriodDays,
                                formatArgs = wrappedList(cooldownPeriodDays),
                            ),
                        ),
                    )
                } else {
                    null to null
                }
            }
            StakingActionType.VOTE_LOCKED -> {
                resourceReference(R.string.staking_revote) to
                    resourceReference(R.string.staking_notifications_revote_tron_text)
            }
            StakingActionType.RESTAKE -> {
                resourceReference(R.string.staking_restake) to
                    resourceReference(R.string.staking_notification_restake_text)
            }
            else -> null to null
        }

        if (titleReference != null && textReference != null) {
            add(
                StakingNotification.Info.Ordinary(
                    title = titleReference,
                    text = textReference,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTronRevoteNotification() {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isTron = isTron(cryptoCurrencyStatus.currency.network.id.value)
        val hasStakedBalance = (cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data)?.balance
            ?.items?.any {
                it.type == BalanceType.PREPARING ||
                    it.type == BalanceType.STAKED ||
                    it.type == BalanceType.LOCKED
            } == true
        if (isTron && hasStakedBalance) {
            add(
                StakingNotification.Info.Ordinary(
                    title = resourceReference(R.string.staking_revote),
                    text = resourceReference(R.string.staking_notifications_revote_tron_text),
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addStakingEntireBalanceNotification(
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount.orZero()

        val isEntireBalance = sendingAmount.plus(feeValue) == balance

        if (isEntireBalance && isSubtractAvailable) {
            add(StakingNotification.Info.StakeEntireBalance)
        }
    }

    private fun MutableList<NotificationUM>.addStakingLowBalanceNotification(
        prevState: StakingUiState,
        actionAmount: BigDecimal,
    ) {
        if (prevState.actionType !is StakingActionCommonType.Exit) return

        val maxAmount = prevState.balanceState?.cryptoAmount ?: return
        val exitRequirements = yield.args.exit?.args?.get(Yield.Args.ArgType.AMOUNT) ?: return

        val amountLeft = maxAmount - actionAmount
        val isNotEnoughLeft = !amountLeft.isZero() && amountLeft < exitRequirements.minimum.orZero()

        if (exitRequirements.required && isNotEnoughLeft) {
            add(StakingNotification.Warning.LowStakedBalance)
        }
    }
}