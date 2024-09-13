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
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
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
import com.tangem.lib.crypto.BlockchainUtils.isTron
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
                isPrimaryButtonEnabled = notifications.none {
                    it is StakingNotification.Error || it is NotificationUM.Error
                },
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
            StakingActionCommonType.EXIT -> addExitInfoNotifications()
            StakingActionCommonType.ENTER -> addEnterInfoNotifications()
            else -> addPendingInfoNotifications(prevState)
        }
    }

    private fun MutableList<NotificationUM>.addExitInfoNotifications() {
        add(
            StakingNotification.Info.Unstake(
                cooldownPeriodDays = yield.metadata.cooldownPeriod.days,
            ),
        )
    }

    private fun MutableList<NotificationUM>.addEnterInfoNotifications() {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isTron = isTron(cryptoCurrencyStatus.currency.network.id.value)
        val hasStakedBalance = (cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data)?.balance
            ?.items?.any {
                it.type == BalanceType.PREPARING ||
                    it.type == BalanceType.STAKED ||
                    it.type == BalanceType.LOCKED
            } == true
        if (hasStakedBalance && isTron) {
            add(StakingNotification.Info.TronRevote)
        }
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
            StakingActionType.WITHDRAW -> {
                resourceReference(R.string.staking_withdraw) to
                    resourceReference(R.string.staking_notification_withdraw_text)
            }
            StakingActionType.UNLOCK_LOCKED -> {
                val cooldownPeriodDays = yield.metadata.cooldownPeriod.days
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
            }
            else -> null to null
        }

        if (titleReference != null && textReference != null) {
            add(
                StakingNotification.Info.PendingAction(
                    title = titleReference,
                    text = textReference,
                ),
            )
        }
    }
}
