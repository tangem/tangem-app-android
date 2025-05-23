package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExceedsBalanceNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeCoverageNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addRentExemptionNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.StakingErrors
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.checkFeeCoverage
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isTon
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
    private val feeError: GetFeeError?,
    private val stakingError: StakingError?,
    private val currencyCheck: CryptoCurrencyCheck,
    private val isSubtractAvailable: Boolean,
    private val yield: Yield,
) : Transformer<StakingUiState> {

    private val stakingInfoNotificationsFactory = StakingInfoNotificationsFactory(
        cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        yield = yield,
        isSubtractAvailable = isSubtractAvailable,
    )

    @Suppress("LongMethod")
    override fun transform(prevState: StakingUiState): StakingUiState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount.orZero()

        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState
        val amountState = prevState.amountState as? AmountState.Data ?: return prevState
        val feeState = confirmationState.feeState as? FeeState.Content

        val amountValue = amountState.amountTextField.cryptoAmount.value.orZero()
        val feeValue = feeState?.fee?.amount?.value.orZero()
        val reduceAmountBy = confirmationState.reduceAmountBy.orZero()

        val isEnterAction = prevState.actionType is StakingActionCommonType.Enter
        val isFeeCoverage = checkFeeCoverage(
            amountValue = amountValue,
            feeValue = feeValue,
            balance = balance,
            isSubtractAvailable = isSubtractAvailable,
            reduceAmountBy = reduceAmountBy,
        )
        val minimumRequirement = yield.args.enter.args[Yield.Args.ArgType.AMOUNT]?.minimum.orZero()
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
                onReload = prevState.clickIntents::getFee,
                feeValue = feeValue,
            )
            addStakingErrorNotifications(stakingError = stakingError, onReload = prevState.clickIntents::getFee)
            // warnings
            addWarningNotifications(
                prevState = prevState,
                amountState = amountState,
                feeState = feeState,
                sendingAmount = sendingAmount,
                isFeeCoverage = isFeeCoverage && isEnterAction && !sendingAmount.equals(minimumRequirement),
            )

            stakingInfoNotificationsFactory.addInfoNotifications(
                notifications = this,
                prevState = prevState,
                sendingAmount = sendingAmount,
                actionAmount = amountValue,
                feeValue = feeValue,
                tonBalanceExtraFeeThreshold = TON_BALANCE_EXTRA_FEE_THRESHOLD,
            )
        }.toImmutableList()

        val isActualSources = with(cryptoCurrencyStatus.value) {
            sources.yieldBalanceSource.isActual() && sources.networkSource.isActual()
        }

        return prevState.copy(
            confirmationState = confirmationState.copy(
                notifications = notifications.toImmutableList(),
                isPrimaryButtonEnabled = notifications.none {
                    it is StakingNotification.Error ||
                        it is NotificationUM.Error ||
                        it is NotificationUM.Warning.NetworkFeeUnreachable ||
                        it is StakingNotification.Warning.TransactionInProgress
                } && isActualSources,
            ),
        )
    }

    private fun MutableList<NotificationUM>.addStakingErrorNotifications(
        stakingError: StakingError?,
        onReload: () -> Unit,
    ) {
        when (stakingError) {
            is StakingError.StakeKitApiError -> {
                if (stakingError.message != StakingErrors.MinimumAmountNotReachedError.message) {
                    add(
                        StakingNotification.Error.Common(subtitle = stringReference(stakingError.toString())),
                    )
                }
            }
            null -> Unit
            else -> add(NotificationUM.Warning.NetworkFeeUnreachable(onReload))
        }
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

        addFeeUnreachableNotification(
            feeError = feeError,
            tokenName = cryptoCurrencyStatusProvider().currency.name,
            onReload = onReload,
        )
        addStakeExceedBalanceNotification(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            actionType = prevState.actionType,
            isSubtractionAvailable = isSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            onClick = prevState.clickIntents::openTokenDetails,
        )
        addTonExtraFeeErrorNotification()
        addExceedsBalanceNotification(
            cryptoCurrencyWarning = currencyWarning,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(network.backendId),
            onClick = prevState.clickIntents::openTokenDetails,
            onAnalyticsEvent = { /* no-op */ },
        )
        if (!BlockchainUtils.isCardano(network.rawId)) {
            addDustWarningNotification(
                dustValue = currencyCheck.dustValue,
                feeValue = feeValue,
                sendingAmount = sendingAmount,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addTransactionLimitErrorNotification(
            currencyCheck = currencyCheck,
            sendingAmount = sendingAmount,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCryptoCurrencyStatus,
            feeValue = feeValue,
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
        cryptoCurrencyStatus.currency

        addRentExemptionNotification(
            rentWarning = currencyCheck.rentWarning,
        )

        addExistentialWarningNotification(
            existentialDeposit = currencyCheck.existentialDeposit,
            feeAmount = feeState?.fee?.amount?.value.orZero(),
            sendingAmount = sendingAmount,
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
    }

    private fun MutableList<NotificationUM>.addStakeExceedBalanceNotification(
        feeAmount: BigDecimal,
        sendingAmount: BigDecimal,
        actionType: StakingActionCommonType,
        isSubtractionAvailable: Boolean,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        onClick: (CryptoCurrency) -> Unit,
    ) {
        val balance = cryptoCurrencyStatus.value.amount.orZero()
        if (!isSubtractionAvailable) return

        val showNotification = sendingAmount + feeAmount > balance
        if (showNotification) {
            val notification = if (actionType is StakingActionCommonType.Enter) {
                NotificationUM.Error.TotalExceedsBalance
            } else {
                with(cryptoCurrencyStatus.currency) {
                    NotificationUM.Error.ExceedsBalance(
                        networkIconId = networkIconResId,
                        networkName = name,
                        currencyName = name,
                        feeName = name,
                        feeSymbol = symbol,
                        mergeFeeNetworkName = BlockchainUtils.isArbitrum(network.backendId),
                        onClick = { onClick(this) },
                    )
                }
            }
            add(notification)
        }
    }

    private fun MutableList<NotificationUM>.addTonExtraFeeErrorNotification() {
        val amount = cryptoCurrencyStatusProvider().value.amount.orZero()
        val cryptoCurrencyNetworkIdValue = cryptoCurrencyStatusProvider().currency.network.rawId

        if (isTon(cryptoCurrencyNetworkIdValue) && amount < TON_BALANCE_EXTRA_FEE_THRESHOLD) {
            add(NotificationUM.Error.TonStakingExtraFeeError)
        }
    }

    private companion object {
        val TON_BALANCE_EXTRA_FEE_THRESHOLD = BigDecimal(0.2)
    }
}