package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedsBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addValidateTransactionNotifications
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.checkFeeCoverage
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class AddStakingNotificationsTransformer(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val currencyWarning: CryptoCurrencyWarning?,
    private val validatorError: Throwable?,
    private val feeError: GetFeeError?,
    private val currencyCheck: CryptoCurrencyCheck,
    private val isSubtractAvailable: Boolean,
    private val yield: Yield,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount.orZero()

        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState
        val amountState = prevState.amountState as? AmountState.Data ?: return prevState
        val feeState = confirmationState.feeState as? FeeState.Content

        val amountValue = amountState.amountTextField.cryptoAmount.value.orZero()
        val feeValue = feeState?.fee?.amount?.value.orZero()
        val reduceAmountBy = confirmationState.reduceAmountBy.orZero()

        val isEnterAction = prevState.actionType == StakingActionCommonType.ENTER
        val isFeeCoverage = checkFeeCoverage(
            amountValue = amountValue,
            feeValue = feeValue,
            balance = balance,
            isSubtractAvailable = isSubtractAvailable,
            reduceAmountBy = reduceAmountBy,
        )
        val sendingAmount = if (isEnterAction) {
            checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isSubtractAvailable,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = reduceAmountBy,
            )
        } else {
            // No amount is taken from account balance on exit or pending actions
            BigDecimal.ZERO
        }

        val notifications = buildList {
            // errors
            addErrorNotifications(
                prevState = prevState,
                feeError = feeError,
                sendingAmount = sendingAmount,
                onReload = { prevState.clickIntents.getFee(confirmationState.pendingAction) },
                feeValue = feeValue,
            )
            // warnings
            addWarningNotifications(
                prevState = prevState,
                amountState = amountState,
                feeState = feeState,
                sendingAmount = sendingAmount,
                isFeeCoverage = isFeeCoverage && isEnterAction,
            )

            addInfoNotifications(prevState)
        }.toImmutableList()

        return prevState.copy(
            confirmationState = confirmationState.copy(
                notifications = notifications.toImmutableList(),
                isPrimaryButtonEnabled = notifications.any { it is StakingNotification.Error },
            ),
        )
    }

    private fun MutableList<NotificationUM>.addErrorNotifications(
        prevState: StakingUiState,
        onReload: () -> Unit,
        feeError: GetFeeError?,
        sendingAmount: BigDecimal,
        feeValue: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val network = cryptoCurrency.network

        if (feeError != null) {
            addFeeUnreachableNotification(
                feeError = feeError,
                tokenName = cryptoCurrencyStatusProvider().currency.name,
                onReload = onReload,
            )
        }
        addExceedBalanceNotification(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            isSubtractionAvailable = isSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        addExceedsBalanceNotification(
            cryptoCurrencyWarning = currencyWarning,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(network.backendId),
            onClick = prevState.clickIntents::openTokenDetails,
            onAnalyticsEvent = { /* todo staking AND-7208 */ },
        )
        if (!BlockchainUtils.isCardano(network.id.value)) {
            addDustWarningNotification(
                dustValue = currencyCheck.dustValue,
                feeValue = feeValue,
                sendingAmount = sendingAmount,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addTransactionLimitErrorNotification(
            utxoLimit = currencyCheck.utxoAmountLimit,
            cryptoCurrency = cryptoCurrency,
            onReduceClick = prevState.clickIntents::onAmountReduceToClick,
        )
        addReserveAmountErrorNotification(
            reserveAmount = currencyCheck.reserveAmount,
            sendingAmount = sendingAmount,
            cryptoCurrency = cryptoCurrency,
            isAccountFunded = false,
        )
    }

    private fun MutableList<NotificationUM>.addWarningNotifications(
        prevState: StakingUiState,
        amountState: AmountState.Data,
        feeState: FeeState.Content?,
        sendingAmount: BigDecimal,
        isFeeCoverage: Boolean,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        addExistentialWarningNotification(
            existentialDeposit = currencyCheck.existentialDeposit,
            feeAmount = feeState?.fee?.amount?.value.orZero(),
            receivedAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            onReduceClick = prevState.clickIntents::onAmountReduceByClick,
        )
        addFeeCoverageNotification(
            isFeeCoverage = isFeeCoverage,
            amountField = amountState.amountTextField,
            sendingValue = sendingAmount,
            appCurrency = appCurrency,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )

        // blockchain specific
        addValidateTransactionNotifications(
            dustValue = currencyCheck.dustValue.orZero(),
            fee = feeState?.fee,
            validationError = validatorError,
            cryptoCurrency = cryptoCurrency,
            onReduceClick = prevState.clickIntents::onAmountReduceToClick,
        )
    }

    private fun MutableList<NotificationUM>.addInfoNotifications(prevState: StakingUiState) {
        when (prevState.actionType) {
            StakingActionCommonType.EXIT -> {
                add(
                    StakingNotification.Info.Unstake(
                        cooldownPeriodDays = yield.metadata.cooldownPeriod.days,
                    ),
                )
            }
            StakingActionCommonType.ENTER -> {
                add(
                    StakingNotification.Info.EarnRewards(
                        subtitleText = resourceReference(
                            id = getEarnRewardsPeriod(yield.metadata.rewardSchedule),
                            formatArgs = wrappedList(yield.token.name),
                        ),
                    ),
                )
            }
            else -> {
                val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
                val pendingActionType = confirmationState?.pendingAction?.type
                val (titleId, textId) = when (pendingActionType) {
                    StakingActionType.CLAIM_REWARDS -> {
                        R.string.common_claim to R.string.staking_notification_claim_rewards_text
                    }
                    StakingActionType.RESTAKE_REWARDS -> {
                        R.string.staking_restake to R.string.staking_notification_restake_rewards_text
                    }
                    StakingActionType.WITHDRAW -> {
                        R.string.staking_withdraw to R.string.staking_notification_withdraw_text
                    }
                    else -> null to null
                }

                if (titleId != null && textId != null) {
                    add(
                        StakingNotification.Info.PendingAction(
                            title = resourceReference(titleId),
                            text = resourceReference(textId),
                        ),
                    )
                }
            }
        }
    }

    private fun getEarnRewardsPeriod(rewardSchedule: Yield.Metadata.RewardSchedule): Int {
        return when (rewardSchedule) {
            Yield.Metadata.RewardSchedule.BLOCK,
            Yield.Metadata.RewardSchedule.DAY,
            Yield.Metadata.RewardSchedule.ERA,
            Yield.Metadata.RewardSchedule.EPOCH,
            -> R.string.staking_notification_earn_rewards_text_period_day

            Yield.Metadata.RewardSchedule.HOUR,
            -> R.string.staking_notification_earn_rewards_text_period_hour

            Yield.Metadata.RewardSchedule.WEEK,
            -> R.string.staking_notification_earn_rewards_text_period_week

            Yield.Metadata.RewardSchedule.MONTH,
            -> R.string.staking_notification_earn_rewards_text_period_month

            else
            -> R.string.staking_notification_earn_rewards_text_period_day
        }
    }
}
