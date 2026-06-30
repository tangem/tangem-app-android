package com.tangem.feature.swap.ui.transfer

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
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.getTezosThreshold
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapTransferNotificationsFactory @Inject constructor() {

    @Suppress("LongParameterList")
    fun getNotifications(
        transferState: SwapState.Transfer,
        feeSelectorUM: FeeSelectorUM?,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        actions: UiActions,
    ): ImmutableList<NotificationUM> {
        // The fee selector exposes a single sealed state; narrow it here so call sites pass the raw
        // FeeSelectorUM and this factory owns the Content/Error/Loading discrimination.
        val feeContent = feeSelectorUM
        val getFeeError = (feeSelectorUM as? FeeSelectorUM.Error)?.error
        return buildList {
            maybeAddRentExemptionError(transferState)
            maybeAddDomainWarnings(
                state = transferState,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                feeSelectorUM = feeContent,
                onReduceByAmount = actions.onReduceByAmount,
                onReduceToAmount = actions.onReduceToAmount,
            )
            maybeAddNeedReserveToCreateAccountWarning(transferState)
            maybeAddExceedsBalanceNotifications(
                transferState = transferState,
                feeSelectorUM = feeContent,
                onBuyClick = actions.openTokenDetailsScreen,
            )
            maybeAddTooHighOrTooLowNotification(feeContent)
            addTronNetworkFeesNotification(
                cryptoCurrencyStatus = transferState.fromTokenInfo.swapCurrencyStatus.status,
                transferState = transferState,
            )
            maybeAddFeeUnreachableNotification(
                transferState = transferState,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                feeError = getFeeError,
                actions = actions,
            )
        }.toPersistentList()
    }

    private fun MutableList<NotificationUM>.maybeAddRentExemptionError(state: SwapState.Transfer) {
        state.currencyCheck?.rentWarning?.let {
            add(NotificationUM.Solana.RentInfo(it))
        }
    }

    private fun MutableList<NotificationUM>.maybeAddDomainWarnings(
        state: SwapState.Transfer,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        feeSelectorUM: FeeSelectorUM?,
        onReduceByAmount: (SwapAmount, BigDecimal) -> Unit,
        onReduceToAmount: (SwapAmount) -> Unit,
    ) {
        val swapCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus
        val amount = state.fromTokenInfo.tokenAmount
        val balance = swapCurrencyStatus.status.value.amount ?: BigDecimal.ZERO
        val fee = (feeSelectorUM as? FeeSelectorUM.Content)?.selectedFeeItem?.fee
        val feeValue = fee?.amount?.value.orZero()
        val isCardano = BlockchainUtils.isCardano(swapCurrencyStatus.currency.network.rawId)
        addExistentialWarningNotification(
            existentialDeposit = state.currencyCheck?.existentialDeposit,
            feeAmount = feeValue,
            sendingAmount = amount.value,
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            onReduceClick = { reduceBy, reduceByDiff, _ ->
                onReduceByAmount(
                    amount.copy(value = amount.value.minus(reduceByDiff)),
                    reduceBy,
                )
            },
        )
        addValidateTransactionNotifications(
            dustValue = state.currencyCheck?.dustValue.orZero(),
            validationError = state.validationResult,
            cryptoCurrency = swapCurrencyStatus.currency,
            minAdaValue = state.minAdaValue,
            onReduceClick = { reduceTo, _ ->
                onReduceToAmount(amount.copy(value = reduceTo))
            },
        )
        if (!isCardano) {
            addDustWarningNotification(
                dustValue = state.currencyCheck?.dustValue,
                feeValue = feeValue,
                sendingAmount = amount.value,
                cryptoCurrencyStatus = swapCurrencyStatus.status,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addReserveAmountErrorNotification(
            reserveAmount = state.currencyCheck?.reserveAmount,
            sendingAmount = amount.value,
            cryptoCurrency = swapCurrencyStatus.currency,
            feeCryptoCurrency = feeCryptoCurrencyStatus?.currency,
            isAccountFunded = true,
        )
        addReduceAmountNotification(
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            fromAmount = state.fromTokenInfo.tokenAmount,
            balance = balance,
            onReduceByAmount = onReduceByAmount,
        )
        addTransactionLimitErrorNotification(
            currencyCheck = state.currencyCheck,
            sendingAmount = amount.value,
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            feeCurrencyStatus = feeCryptoCurrencyStatus,
            feeValue = feeValue,
            onReduceClick = { reduceTo, _ ->
                onReduceToAmount(amount.copy(value = reduceTo))
            },
        )
        maybeAddFeeCoverageNotification(state = state, amount = amount)
    }

    private fun MutableList<NotificationUM>.maybeAddFeeCoverageNotification(
        state: SwapState.Transfer,
        amount: SwapAmount,
    ) {
        addFeeCoverageNotification(
            isFeeCoverage = state.isFeeCoverage,
            enteredAmountValue = amount.value,
            sendingValue = state.sendingAmount,
            appCurrency = state.appCurrency,
            cryptoCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus.status,
        )
    }

    private fun MutableList<NotificationUM>.maybeAddNeedReserveToCreateAccountWarning(state: SwapState.Transfer) {
        val status = state.toTokenInfo.swapCurrencyStatus.status.value
        if (status is CryptoCurrencyStatus.NoAccount) {
            val amount = state.toTokenInfo.tokenAmount.value
            val amountToCreateAccount = status.amountToCreateAccount
            val currencyTo = state.toTokenInfo.swapCurrencyStatus.currency
            if (amount < amountToCreateAccount) {
                add(
                    SwapNotificationUM.Warning.NeedReserveToCreateAccount(
                        receiveAmount = status.amountToCreateAccount.parseBigDecimal(currencyTo.decimals),
                        receiveToken = currencyTo.symbol,
                    ),
                )
            }
        }
    }

    private fun MutableList<NotificationUM>.addReduceAmountNotification(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fromAmount: SwapAmount,
        balance: BigDecimal,
        onReduceByAmount: (SwapAmount, BigDecimal) -> Unit,
    ) {
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.rawId)
        val threshold = getTezosThreshold()
        val isTotalBalance = fromAmount.value >= balance && balance > threshold
        if (isTezos && isTotalBalance) {
            add(
                SwapNotificationUM.Warning.ReduceAmount(
                    currencyName = cryptoCurrencyStatus.currency.name,
                    amount = threshold.toPlainString(),
                    onConfirmClick = {
                        val patchedAmount = fromAmount.copy(
                            value = fromAmount.value - threshold,
                        )
                        onReduceByAmount(patchedAmount, threshold)
                    },
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.maybeAddExceedsBalanceNotifications(
        transferState: SwapState.Transfer,
        feeSelectorUM: FeeSelectorUM?,
        onBuyClick: (CryptoCurrency) -> Unit,
    ) {
        val cryptoCurrencyStatus = transferState.fromTokenInfo.swapCurrencyStatus.status
        addExceedsBalanceNotification(
            cryptoCurrencyWarning = transferState.cryptoCurrencyWarning,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            shouldMergeFeeNetworkName = BlockchainUtils.isArbitrum(
                networkId = cryptoCurrencyStatus.currency.network.rawId,
            ),
            onClick = onBuyClick,
            onAnalyticsEvent = {},
            onResetAnalyticsEvent = {},
        )
        val feeAmount = (feeSelectorUM as? FeeSelectorUM.Content)?.selectedFeeItem?.fee?.amount?.value
        if (feeAmount != null) {
            addExceedBalanceNotification(
                feeAmount = feeAmount,
                sendingAmount = transferState.sendingAmount,
                isSubtractionAvailable = transferState.isAmountSubtractAvailable,
                cryptoCurrencyStatus = transferState.fromTokenInfo.swapCurrencyStatus.status,
            )
        }
    }

    @Suppress("CanBeNonNullable")
    private fun MutableList<NotificationUM>.maybeAddTooHighOrTooLowNotification(feeSelectorUM: FeeSelectorUM?) {
        val content = feeSelectorUM as? FeeSelectorUM.Content ?: return
        val (isFeeTooHigh, diff) = FeeCalculationUtils.checkIfCustomFeeTooHigh(feeSelectorUM = content)
        if (isFeeTooHigh) {
            add(NotificationUM.Warning.TooHigh(diff))
        }
        if (FeeCalculationUtils.checkIfCustomFeeTooLow(feeSelectorUM = content)) {
            add(NotificationUM.Warning.FeeTooLow)
        }
    }

    private fun MutableList<NotificationUM>.addTronNetworkFeesNotification(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        transferState: SwapState.Transfer,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val isTronToken = cryptoCurrency is CryptoCurrency.Token && isTron(cryptoCurrency.network.rawId)
        val isVisible = isTronToken &&
            transferState.tronFeeNotificationShowCount <= TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT

        if (isVisible) {
            add(SwapNotificationUM.Info.TronTokenFee)
        }
    }

    private fun MutableList<NotificationUM>.maybeAddFeeUnreachableNotification(
        transferState: SwapState.Transfer,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        feeError: GetFeeError?,
        actions: UiActions,
    ) {
        feeCryptoCurrencyStatus ?: return
        addFeeUnreachableNotification(
            tokenStatus = transferState.fromTokenInfo.swapCurrencyStatus.status,
            coinStatus = feeCryptoCurrencyStatus,
            feeError = feeError,
            dustValue = transferState.currencyCheck?.dustValue,
            onReload = actions.onRetryClick,
            onClick = actions.openTokenDetailsScreen,
        )
    }

    companion object {
        private const val TRON_FEE_NOTIFICATION_MAX_SHOW_COUNT = 3
    }
}