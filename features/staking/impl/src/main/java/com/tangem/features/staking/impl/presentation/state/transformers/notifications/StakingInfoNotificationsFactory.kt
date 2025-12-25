package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.tangem.blockchain.common.Amount
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.toTextReference
import com.tangem.lib.crypto.BlockchainUtils.isCardano
import com.tangem.lib.crypto.BlockchainUtils.isCosmos
import com.tangem.lib.crypto.BlockchainUtils.isTon
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.Provider
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

internal class StakingInfoNotificationsFactory(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val integration: StakingIntegration,
    private val isSubtractAvailable: Boolean,
) {

    /**
     * @param notifications current notification to display
     * @param prevState     current screen state to update
     * @param sendingAmount amount being transferred from user account
     * @param actionAmount  any amount being transferred or used action
     * @param feeAmount     fee amount payed from user account
     * @param tonBalanceExtraFeeThreshold threshold for TON extra fee notification
     * @param isFeeApproximate whether fee is approximate
     * @param onAmountReduceByFeeClick callback to reduce amount by fee
     */
    @Suppress("LongParameterList")
    fun addInfoNotifications(
        notifications: MutableList<NotificationUM>,
        prevState: StakingUiState,
        sendingAmount: BigDecimal,
        actionAmount: BigDecimal,
        feeAmount: Amount?,
        tonBalanceExtraFeeThreshold: BigDecimal,
        isFeeApproximate: Boolean,
        onAmountReduceByFeeClick: (BigDecimal, notification: Class<out NotificationUM>) -> Unit,
    ) = with(notifications) {
        addStakingLowBalanceNotification(prevState, actionAmount)
        addTonExtraFeeInfoNotification(tonBalanceExtraFeeThreshold)

        when (prevState.actionType) {
            is StakingActionCommonType.Enter -> addEnterInfoNotifications(
                sendingAmount = sendingAmount,
                feeAmount = feeAmount,
                isFeeApproximate = isFeeApproximate,
                onAmountReduceByFeeClick = onAmountReduceByFeeClick,
            )
            is StakingActionCommonType.Exit -> addExitInfoNotifications(prevState)
            is StakingActionCommonType.Pending -> {
                addCardanoRestakeMinimumAmountNotification(feeAmount?.value.orZero())
                addPendingInfoNotifications(prevState)
                addTonHaveToUnstakeAllNotification(prevState)
            }
        }
    }

    private fun MutableList<NotificationUM>.addExitInfoNotifications(prevState: StakingUiState) {
        addUnstakeInfoNotification()
        addTonHaveToUnstakeAllNotification(prevState)
    }

    private fun MutableList<NotificationUM>.addEnterInfoNotifications(
        sendingAmount: BigDecimal,
        feeAmount: Amount?,
        isFeeApproximate: Boolean,
        onAmountReduceByFeeClick: (BigDecimal, notification: Class<out NotificationUM>) -> Unit,
    ) {
        addCardanoStakeMinimumAmountNotification(feeAmount?.value.orZero())
        addTronRevoteNotification()
        addCardanoStakeNotification()
        addStakingEntireBalanceNotification(
            sendingAmount = sendingAmount,
            feeAmount = feeAmount,
            isFeeApproximate = isFeeApproximate,
            onAmountReduceByFeeClick = onAmountReduceByFeeClick,
        )
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
                val cooldownPeriod = integration.cooldownPeriod
                if (cooldownPeriod != null) {
                    resourceReference(R.string.staking_unlocked_locked) to resourceReference(
                        R.string.staking_notification_unlock_text,
                        wrappedList(cooldownPeriod.toTextReference()),
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
        val isTron = isTron(cryptoCurrencyStatus.currency.network.rawId)
        val hasStakedBalance = (cryptoCurrencyStatus.value.stakingBalance as? StakingBalance.Data.StakeKit)?.balance
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

    private fun MutableList<NotificationUM>.addCardanoStakeNotification() {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isCardano = isCardano(cryptoCurrencyStatus.currency.network.rawId)

        if (isCardano) {
            add(
                StakingNotification.Info.Ordinary(
                    title = resourceReference(R.string.staking_notification_additional_ada_deposit_title),
                    text = resourceReference(R.string.staking_notification_additional_ada_deposit_text),
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addCardanoStakeMinimumAmountNotification(feeValue: BigDecimal) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isCardano = isCardano(cryptoCurrencyStatus.currency.network.rawId)
        val balance = cryptoCurrencyStatus.value.amount.orZero()
        if (isCardano && balance - feeValue < MINIMUM_STAKE_BALANCE) {
            add(
                StakingNotification.Error.MinimumAmountNotReachedError(
                    title = resourceReference(R.string.staking_notification_minimum_balance_title),
                    subtitle = resourceReference(R.string.staking_notification_minimum_stake_ada_text),
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addCardanoRestakeMinimumAmountNotification(feeValue: BigDecimal) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isCardano = isCardano(cryptoCurrencyStatus.currency.network.rawId)
        val balance = cryptoCurrencyStatus.value.amount.orZero()
        if (isCardano && balance - feeValue < MINIMUM_RESTAKE_BALANCE) {
            add(
                StakingNotification.Error.MinimumAmountNotReachedError(
                    title = resourceReference(R.string.staking_notification_minimum_restake_ada_title),
                    subtitle = resourceReference(R.string.staking_notification_minimum_restake_ada_text),
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addStakingEntireBalanceNotification(
        sendingAmount: BigDecimal,
        feeAmount: Amount?,
        isFeeApproximate: Boolean,
        onAmountReduceByFeeClick: (BigDecimal, notification: Class<out NotificationUM>) -> Unit,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount.orZero()
        val feeValue = feeAmount?.value.orZero()

        val isEntireBalance = sendingAmount.plus(feeValue) == balance

        if (isEntireBalance && isSubtractAvailable && !isCardano(cryptoCurrencyStatus.currency.network.rawId)) {
            val value = feeValue.multiply(FEE_DECIMALS_MULTIPLIER)

            val reduceAmountValue = feeAmount
                ?.takeIf { feeValue != BigDecimal.ZERO }
                ?.let { amount ->
                    resourceReference(
                        R.string.send_notification_reduce_by,
                        wrappedList(
                            value.format {
                                crypto(
                                    symbol = amount.currencySymbol,
                                    decimals = amount.decimals,
                                ).fee(canBeLower = isFeeApproximate)
                            },
                        ),
                    )
                }

            add(
                StakingNotification.Info.StakeEntireBalance(
                    reduceAmountValue,
                    {
                        onAmountReduceByFeeClick.invoke(
                            value,
                            StakingNotification.Info.StakeEntireBalance::class.java,
                        )
                    },
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addStakingLowBalanceNotification(
        prevState: StakingUiState,
        actionAmount: BigDecimal,
    ) {
        if (prevState.actionType !is StakingActionCommonType.Exit) return

        val maxAmount = prevState.balanceState?.cryptoAmount ?: return
        val exitRequirements = integration.exitArgs?.amountRequirement ?: return

        val amountLeft = maxAmount - actionAmount
        val isNotEnoughLeft = !amountLeft.isZero() && amountLeft < exitRequirements.minimum.orZero()

        if (exitRequirements.isRequired && isNotEnoughLeft) {
            add(StakingNotification.Warning.LowStakedBalance)
        }
    }

    private fun MutableList<NotificationUM>.addUnstakeInfoNotification() {
        val cooldownPeriod = integration.cooldownPeriod

        val cryptoCurrencyNetworkIdValue = cryptoCurrencyStatusProvider().currency.network.rawId
        if (cooldownPeriod != null) {
            add(
                StakingNotification.Info.Unstake(
                    cooldownPeriod = cooldownPeriod,
                    subtitleRes = if (isCosmos(cryptoCurrencyNetworkIdValue)) {
                        R.string.staking_notification_unstake_cosmos_text
                    } else {
                        R.string.staking_notification_unstake_text
                    },
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addTonHaveToUnstakeAllNotification(prevState: StakingUiState) {
        val cryptoCurrencyNetworkIdValue = cryptoCurrencyStatusProvider().currency.network.rawId

        if (isTon(cryptoCurrencyNetworkIdValue)) {
            val initialInfoState = prevState.initialInfoState as? StakingStates.InitialInfoState.Data
            val stakingBalances = (initialInfoState?.yieldBalance as? InnerYieldBalanceState.Data)?.balances

            val targetAddress = prevState.balanceState?.target?.address ?: return

            val stakesCountWithCertainTarget = stakingBalances.orEmpty()
                .count { state ->
                    (state.type == BalanceType.STAKED ||
                        state.type == BalanceType.PREPARING ||
                        state.type == BalanceType.UNSTAKED) &&
                        state.target?.address == targetAddress
                }

            if (stakesCountWithCertainTarget > 1) {
                add(
                    StakingNotification.Info.Ordinary(
                        title = resourceReference(R.string.staking_notification_ton_have_to_unstake_all_title),
                        text = resourceReference(R.string.staking_notification_ton_have_to_unstake_all_text),
                    ),
                )
            }
        }
    }

    private fun MutableList<NotificationUM>.addTonExtraFeeInfoNotification(tonBalanceExtraFeeThreshold: BigDecimal) {
        val amount = cryptoCurrencyStatusProvider().value.amount.orZero()
        val cryptoCurrencyNetworkIdValue = cryptoCurrencyStatusProvider().currency.network.rawId

        if (isTon(cryptoCurrencyNetworkIdValue) && amount >= tonBalanceExtraFeeThreshold) {
            add(
                StakingNotification.Info.Ordinary(
                    title = resourceReference(R.string.staking_notification_ton_extra_reserve_title),
                    text = resourceReference(R.string.staking_notification_ton_extra_reserve_info),
                ),
            )
        }
    }

    private companion object {
        val MINIMUM_STAKE_BALANCE = "5".toBigDecimal()
        val MINIMUM_RESTAKE_BALANCE = "3".toBigDecimal()
        val FEE_DECIMALS_MULTIPLIER = "3".toBigDecimal()
    }
}