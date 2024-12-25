package com.tangem.feature.swap.presentation

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addValidateTransactionNotifications
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.getTezosThreshold
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class SwapNotificationsFactory(
    private val actions: UiActions,
) {

    fun getInitialErrorStateNotifications(code: Int, onRefreshClick: () -> Unit): ImmutableList<NotificationUM> {
        return persistentListOf(
            SwapNotificationUM.Warning.ExpressGeneralError(
                code = code,
                onConfirmClick = onRefreshClick,
            ),
        )
    }

    fun getGeneralErrorStateNotifications(
        notifications: ImmutableList<NotificationUM>,
        message: TextReference?,
        onClick: () -> Unit,
    ): ImmutableList<NotificationUM> {
        val updatedNotifications = notifications.toMutableList()
        updatedNotifications.add(
            SwapNotificationUM.Error.GenericError(
                subtitle = message,
                onConfirmClick = onClick,
            ),
        )

        return updatedNotifications.toPersistentList()
    }

    fun getNotAvailableStateNotifications(fromCurrencyName: String): ImmutableList<NotificationUM> {
        return persistentListOf(
            SwapNotificationUM.Warning.NoAvailableTokensToSwap(fromCurrencyName),
        )
    }

    fun getQuotesErrorStateNotifications(
        expressDataError: ExpressDataError,
        fromToken: CryptoCurrency,
        feeItem: FeeItemState,
        includeFeeInAmount: IncludeFeeInAmount,
    ): ImmutableList<NotificationUM> {
        return buildList {
            add(getWarningForError(expressDataError, fromToken, actions.onRetryClick))
            if (includeFeeInAmount is IncludeFeeInAmount.Included && feeItem is FeeItemState.Content) {
                add(
                    NotificationUM.Warning.FeeCoverageNotification(
                        feeItem.amountCrypto,
                        feeItem.amountFiatFormatted,
                    ),
                )
            }
        }.toPersistentList()
    }

    fun getApprovalInProgressStateNotification(
        notifications: ImmutableList<NotificationUM>,
    ): ImmutableList<NotificationUM> {
        val updatedNotifications = notifications
            .filterNot { it is SwapNotificationUM.Info.PermissionNeeded }
            .toMutableList()

        updatedNotifications.add(0, SwapNotificationUM.Error.ApprovalInProgressWarning)

        return updatedNotifications.toPersistentList()
    }

    fun getConfirmationStateNotifications(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        selectedFeeType: FeeType,
        providerName: String,
    ): ImmutableList<NotificationUM> {
        val warnings = buildList {
            maybeAddRentExemptionError(quoteModel)
            maybeAddDomainWarnings(quoteModel, feeCryptoCurrencyStatus, selectedFeeType)
            maybeAddNeedReserveToCreateAccountWarning(quoteModel)
            maybeAddPermissionNeededWarning(quoteModel, fromToken, providerName)
            maybeAddNetworkFeeCoverageWarning(quoteModel, selectedFeeType)
            maybeAddUnableCoverFeeWarning(quoteModel, fromToken)
            maybeAddTransactionInProgressWarning(quoteModel)
        }
        return warnings.toPersistentList()
    }

    private fun MutableList<NotificationUM>.maybeAddRentExemptionError(quoteModel: SwapState.QuotesLoadedState) {
        quoteModel.currencyCheck?.rentWarning?.let {
            add(NotificationUM.Solana.RentInfo(it))
        }
    }

    private fun MutableList<NotificationUM>.maybeAddTransactionInProgressWarning(
        quoteModel: SwapState.QuotesLoadedState,
    ) {
        if (quoteModel.permissionState is PermissionDataState.PermissionLoading) {
            add(SwapNotificationUM.Error.ApprovalInProgressWarning)
        } else if (quoteModel.preparedSwapConfigState.hasOutgoingTransaction) {
            add(
                SwapNotificationUM.Error.TransactionInProgressWarning(
                    currencySymbol = quoteModel.fromTokenInfo.cryptoCurrencyStatus.currency.network.currencySymbol,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.maybeAddDomainWarnings(
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        selectedFeeType: FeeType,
    ) {
        val fromCurrencyStatus = quoteModel.fromTokenInfo.cryptoCurrencyStatus
        val includeFeeInAmount = quoteModel.preparedSwapConfigState.includeFeeInAmount
        val amount = quoteModel.fromTokenInfo.tokenAmount
        val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
            includeFeeInAmount.amountSubtractFee
        } else {
            amount
        }
        val fee = when (val feeState = quoteModel.txFee) {
            TxFeeState.Empty -> null
            is TxFeeState.MultipleFeeState -> if (feeState.normalFee.feeType == selectedFeeType) {
                feeState.normalFee
            } else {
                feeState.priorityFee
            }
            is TxFeeState.SingleFeeState -> feeState.fee
        }
        val isCardano = BlockchainUtils.isCardano(fromCurrencyStatus.currency.network.id.value)
        // blockchain specific

        addExistentialWarningNotification(
            existentialDeposit = quoteModel.currencyCheck?.existentialDeposit,
            feeAmount = fee?.feeValue.orZero(),
            sendingAmount = amount.value,
            cryptoCurrencyStatus = fromCurrencyStatus,
            onReduceClick = { reduceBy, reduceByDiff, _ ->
                actions.onReduceByAmount(
                    amount.copy(value = amount.value.minus(reduceByDiff)),
                    reduceBy,
                )
            },
        )
        addValidateTransactionNotifications(
            dustValue = quoteModel.currencyCheck?.dustValue.orZero(),
            validationError = quoteModel.validationResult,
            cryptoCurrency = fromCurrencyStatus.currency,
            minAdaValue = quoteModel.minAdaValue,
            onReduceClick = { reduceTo, _ ->
                actions.onReduceToAmount(amount.copy(value = reduceTo))
            },
        )
        if (!isCardano) {
            addDustWarningNotification(
                dustValue = quoteModel.currencyCheck?.dustValue,
                feeValue = fee?.feeValue.orZero(),
                sendingAmount = amountToRequest.value,
                cryptoCurrencyStatus = fromCurrencyStatus,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addReserveAmountErrorNotification(
            reserveAmount = quoteModel.currencyCheck?.reserveAmount,
            sendingAmount = amountToRequest.value,
            cryptoCurrency = fromCurrencyStatus.currency,
            isAccountFunded = false,
        )
        addReduceAmountNotification(
            cryptoCurrencyStatus = fromCurrencyStatus,
            fromAmount = quoteModel.fromTokenInfo.tokenAmount,
            onReduceByAmount = actions.onReduceByAmount,
        )
        addTransactionLimitErrorNotification(
            utxoLimit = quoteModel.currencyCheck?.utxoAmountLimit,
            cryptoCurrency = fromCurrencyStatus.currency,
            onReduceClick = { reduceTo, _ ->
                actions.onReduceToAmount(amountToRequest.copy(value = reduceTo))
            },
        )
    }

    private fun MutableList<NotificationUM>.maybeAddNeedReserveToCreateAccountWarning(
        quoteModel: SwapState.QuotesLoadedState,
    ) {
        val status = quoteModel.toTokenInfo.cryptoCurrencyStatus.value
        if (status is CryptoCurrencyStatus.NoAccount) {
            val amount = quoteModel.toTokenInfo.tokenAmount.value
            val amountToCreateAccount = status.amountToCreateAccount
            val currencyTo = quoteModel.fromTokenInfo.cryptoCurrencyStatus.currency
            if (amount < amountToCreateAccount) {
                add(
                    SwapNotificationUM.Warning.NeedReserveToCreateAccount(
                        status.amountToCreateAccount.parseBigDecimal(currencyTo.decimals),
                        currencyTo.symbol,
                    ),
                )
            }
        }
    }

    private fun MutableList<NotificationUM>.maybeAddPermissionNeededWarning(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        providerName: String,
    ) {
        if (!quoteModel.preparedSwapConfigState.isAllowedToSpend &&
            quoteModel.preparedSwapConfigState.feeState is SwapFeeState.Enough &&
            quoteModel.permissionState is PermissionDataState.PermissionReadyForRequest
        ) {
            add(
                SwapNotificationUM.Info.PermissionNeeded(
                    providerName = fromToken.symbol,
                    fromTokenSymbol = providerName,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.maybeAddNetworkFeeCoverageWarning(
        quoteModel: SwapState.QuotesLoadedState,
        selectedFeeType: FeeType,
    ) {
        when (quoteModel.preparedSwapConfigState.includeFeeInAmount) {
            is IncludeFeeInAmount.Included -> {
                val fee = selectFeeByType(selectedFeeType, quoteModel.txFee) ?: return
                if (needShowNetworkFeeCoverageWarningShow(quoteModel)) {
                    add(
                        NotificationUM.Warning.FeeCoverageNotification(
                            fee.feeCryptoFormattedWithNative,
                            fee.feeFiatFormattedWithNative,
                        ),
                    )
                }
            }
            else -> Unit
        }
    }

    private fun selectFeeByType(feeType: FeeType, txFeeState: TxFeeState): TxFee? {
        return when (txFeeState) {
            TxFeeState.Empty -> null
            is TxFeeState.SingleFeeState -> txFeeState.fee
            is TxFeeState.MultipleFeeState -> when (feeType) {
                FeeType.NORMAL -> txFeeState.normalFee
                FeeType.PRIORITY -> txFeeState.priorityFee
            }
        }
    }

    private fun MutableList<NotificationUM>.maybeAddUnableCoverFeeWarning(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
    ) {
        val feeEnoughState = quoteModel.preparedSwapConfigState.feeState as? SwapFeeState.NotEnough ?: return
        val needShowCoverWarning = quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.permissionState !is PermissionDataState.PermissionLoading &&
            feeEnoughState.feeCurrency != fromToken
        if (needShowCoverWarning) {
            add(
                SwapNotificationUM.Error.UnableToCoverFeeWarning(
                    fromToken = fromToken,
                    feeCurrency = feeEnoughState.feeCurrency,
                    currencyName = feeEnoughState.currencyName ?: fromToken.network.name,
                    currencySymbol = feeEnoughState.currencySymbol ?: fromToken.network.currencySymbol,
                    onConfirmClick = actions.onBuyClick,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.addReduceAmountNotification(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fromAmount: SwapAmount,
        onReduceByAmount: (SwapAmount, BigDecimal) -> Unit,
    ) {
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.id.value)
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
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

    private fun getWarningForError(
        expressDataError: ExpressDataError,
        fromToken: CryptoCurrency,
        onRetryClick: () -> Unit,
    ): NotificationUM {
        return when (expressDataError) {
            is ExpressDataError.ExchangeTooSmallAmountError -> SwapNotificationUM.Error.MinimalAmountError(
                expressDataError.amount.value.format {
                    crypto(fromToken.symbol, fromToken.decimals)
                },
            )
            is ExpressDataError.ExchangeTooBigAmountError -> SwapNotificationUM.Error.MaximumAmountError(
                expressDataError.amount.value.format {
                    crypto(fromToken.symbol, fromToken.decimals)
                },
            )
            else -> SwapNotificationUM.Warning.ExpressError(
                expressDataError,
                onConfirmClick = onRetryClick,
            )
        }
    }

    private fun needShowNetworkFeeCoverageWarningShow(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return quoteModel.currencyCheck?.existentialDeposit == null
    }
}