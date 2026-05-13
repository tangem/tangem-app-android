package com.tangem.feature.swap.model

import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addDustWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addExistentialWarningNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addReserveAmountErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addTransactionLimitErrorNotification
import com.tangem.common.ui.notifications.NotificationsFactory.addValidateTransactionNotifications
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.SwapFeeState
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.getTezosThreshold
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LargeClass")
internal class SwapNotificationsFactory(
    private val actions: UiActions,
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val appCurrencyProvider: Provider<AppCurrency> = Provider { AppCurrency.Default },
) {

    fun getGeneralErrorStateNotifications(
        message: TextReference?,
        onClick: () -> Unit,
    ): ImmutableList<NotificationUM> {
        return persistentListOf(
            SwapNotificationUM.Error.GenericError(
                subtitle = message,
                onConfirmClick = onClick,
            ),
        )
    }

    fun getErrorStateNotification(
        expressError: ExpressError,
        onRetryClick: () -> Unit,
    ): ImmutableList<NotificationUM> {
        return persistentListOf(
            SwapNotificationUM.Warning.ExpressErrorWarning(
                expressError = expressError,
                onConfirmClick = onRetryClick,
            ),
        )
    }

    fun getSwapNotSupportedNotifications(): ImmutableList<NotificationUM> {
        return persistentListOf(
            SwapNotificationUM.Warning.SwapNotSupported,
        )
    }

    fun getQuotesErrorStateNotifications(
        expressDataError: ExpressDataError,
        fromToken: CryptoCurrency,
        includeFeeInAmount: IncludeFeeInAmount,
        swapFee: SwapFee?,
    ): ImmutableList<NotificationUM> {
        return buildList {
            add(getWarningForError(expressDataError, fromToken, actions.onRetryClick))
            if (includeFeeInAmount is IncludeFeeInAmount.Included && swapFee != null) {
                add(formatFeeCoverageNotification(swapFee))
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
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        swapFee: SwapFee?,
    ): ImmutableList<NotificationUM> {
        val warnings = buildList {
            maybeAddRentExemptionError(quoteModel)
            maybeAddDomainWarnings(quoteModel, feeCryptoCurrencyStatus, swapFee)
            maybeAddNeedReserveToCreateAccountWarning(quoteModel)
            maybeAddPermissionNeededWarning(quoteModel)
            maybeAddNetworkFeeCoverageWarning(quoteModel, swapFee)
            maybeAddUnableCoverFeeWarning(quoteModel, feeCryptoCurrencyStatus)
            maybeAddTransactionInProgressWarning(quoteModel)
            maybeAddPriceImpactNotification(quoteModel.priceImpact)
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
            val fromCurrency = quoteModel.fromTokenInfo.swapCurrencyStatus.currency
            add(
                SwapNotificationUM.Error.TransactionInProgressWarning(
                    currencySymbol = fromCurrency.network.currencySymbol,
                ),
            )
        }
    }

    private fun MutableList<NotificationUM>.maybeAddPriceImpactNotification(priceImpact: PriceImpact) {
        if (priceImpact.amountSignificance == PriceImpact.AmountSignificance.LOW) return

        val notification = when (priceImpact.type) {
            PriceImpact.Type.HIGH -> SwapNotificationUM.Warning.TradeTooHigh
            PriceImpact.Type.MEDIUM -> SwapNotificationUM.Warning.HighPriceImpact
            else -> return
        }

        add(notification)
    }

    private fun MutableList<NotificationUM>.maybeAddDomainWarnings(
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        swapFee: SwapFee?,
    ) {
        val swapCurrencyStatus = quoteModel.fromTokenInfo.swapCurrencyStatus
        val includeFeeInAmount = quoteModel.preparedSwapConfigState.includeFeeInAmount
        val amount = quoteModel.fromTokenInfo.tokenAmount
        val amountToRequest = if (includeFeeInAmount is IncludeFeeInAmount.Included) {
            includeFeeInAmount.amountSubtractFee
        } else {
            amount
        }
        val feeValue = swapFee?.fee?.amount?.value.orZero()
        val isCardano = BlockchainUtils.isCardano(swapCurrencyStatus.currency.network.rawId)
        // blockchain specific

        addExistentialWarningNotification(
            existentialDeposit = quoteModel.currencyCheck?.existentialDeposit,
            feeAmount = feeValue,
            sendingAmount = amountToRequest.value,
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            onReduceClick = { reduceBy, reduceByDiff, _ ->
                actions.onReduceByAmount(
                    // use in swap notification amountToRequest because fee is already subtracted
                    amountToRequest.copy(value = amountToRequest.value.minus(reduceByDiff)),
                    reduceBy,
                )
            },
        )
        addValidateTransactionNotifications(
            dustValue = quoteModel.currencyCheck?.dustValue.orZero(),
            validationError = quoteModel.validationResult,
            cryptoCurrency = swapCurrencyStatus.currency,
            minAdaValue = quoteModel.minAdaValue,
            onReduceClick = { reduceTo, _ ->
                actions.onReduceToAmount(amount.copy(value = reduceTo))
            },
        )
        if (!isCardano) {
            addDustWarningNotification(
                dustValue = quoteModel.currencyCheck?.dustValue,
                feeValue = feeValue,
                sendingAmount = amountToRequest.value,
                cryptoCurrencyStatus = swapCurrencyStatus.status,
                feeCurrencyStatus = feeCryptoCurrencyStatus,
            )
        }
        addReserveAmountErrorNotification(
            reserveAmount = quoteModel.currencyCheck?.reserveAmount,
            sendingAmount = amountToRequest.value,
            cryptoCurrency = swapCurrencyStatus.currency,
            feeCryptoCurrency = feeCryptoCurrencyStatus?.currency,
            isAccountFunded = true, // consider the account is funded on the provider side
        )
        addReduceAmountNotification(
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            fromAmount = quoteModel.fromTokenInfo.tokenAmount,
            onReduceByAmount = actions.onReduceByAmount,
        )
        addTransactionLimitErrorNotification(
            currencyCheck = quoteModel.currencyCheck,
            sendingAmount = amountToRequest.value,
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            feeCurrencyStatus = feeCryptoCurrencyStatus,
            feeValue = feeValue,
            onReduceClick = { reduceTo, _ ->
                actions.onReduceToAmount(amountToRequest.copy(value = reduceTo))
            },
        )
    }

    private fun MutableList<NotificationUM>.maybeAddNeedReserveToCreateAccountWarning(
        quoteModel: SwapState.QuotesLoadedState,
    ) {
        val status = quoteModel.toTokenInfo.swapCurrencyStatus.status.value
        if (status is CryptoCurrencyStatus.NoAccount) {
            val amount = quoteModel.toTokenInfo.tokenAmount.value
            val amountToCreateAccount = status.amountToCreateAccount
            val currencyTo = quoteModel.toTokenInfo.swapCurrencyStatus.currency
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

    private fun MutableList<NotificationUM>.maybeAddPermissionNeededWarning(quoteModel: SwapState.QuotesLoadedState) {
        if (quoteModel.permissionState is PermissionDataState.PermissionRequired) {
            add(
                SwapNotificationUM.Info.PermissionNeeded(
                    onApproveClick = actions.openPermissionBottomSheet,
                    onLearnMoreClick = { actions.onLinkClick(TangemSiteUrlBuilder.HELP_CENTER_SWAP_URL) },
                ),
            )
        }
    }

    @Suppress("CanBeNonNullable")
    private fun MutableList<NotificationUM>.maybeAddNetworkFeeCoverageWarning(
        quoteModel: SwapState.QuotesLoadedState,
        swapFee: SwapFee?,
    ) {
        when (quoteModel.preparedSwapConfigState.includeFeeInAmount) {
            is IncludeFeeInAmount.Included -> {
                if (swapFee == null) return
                if (needShowNetworkFeeCoverageWarningShow(quoteModel)) {
                    add(formatFeeCoverageNotification(swapFee))
                }
            }
            else -> Unit
        }
    }

    private fun formatFeeCoverageNotification(swapFee: SwapFee): NotificationUM.Warning.FeeCoverageNotification {
        val feeAmount = swapFee.fee.amount
        val totalFeeValue = (feeAmount.value ?: BigDecimal.ZERO) + swapFee.otherNativeFee
        val cryptoAmount = totalFeeValue.format {
            crypto(symbol = feeAmount.currencySymbol, decimals = feeAmount.decimals)
        }
        val appCurrency = appCurrencyProvider()
        val fiatRate = swapFee.selectedFeeToken.value.fiatRate
        val fiatAmount = fiatRate?.let { it * totalFeeValue }.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
        return NotificationUM.Warning.FeeCoverageNotification(
            cryptoAmount = cryptoAmount,
            fiatAmount = fiatAmount,
        )
    }

    private fun MutableList<NotificationUM>.maybeAddUnableCoverFeeWarning(
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    ) {
        val fromCurrency = quoteModel.fromTokenInfo.swapCurrencyStatus.currency
        val feeEnoughState = quoteModel.preparedSwapConfigState.feeState as? SwapFeeState.NotEnough ?: return
        val shouldShowCoverWarning = quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.permissionState !is PermissionDataState.PermissionLoading &&
            feeCryptoCurrencyStatus?.currency != fromCurrency

        val isNotEnoughFee =
            quoteModel.preparedSwapConfigState.includeFeeInAmount is IncludeFeeInAmount.BalanceNotEnough

        val isGaslessAvailable = isGaslessFeeSupportedForNetwork(fromCurrency.network) &&
            quoteModel.swapProvider.type == ExchangeProviderType.CEX
        if (shouldShowCoverWarning && !isGaslessAvailable || isNotEnoughFee) {
            add(
                SwapNotificationUM.Error.UnableToCoverFeeWarning(
                    fromToken = fromCurrency,
                    feeCurrency = feeCryptoCurrencyStatus?.currency,
                    currencyName = feeEnoughState.currencyName ?: fromCurrency.network.name,
                    currencySymbol = feeEnoughState.currencySymbol ?: fromCurrency.network.currencySymbol,
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
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.rawId)
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
            else -> {
                val expressError = expressDataError.toExpressError()
                SwapNotificationUM.Warning.ExpressErrorWarning(
                    expressError = expressError,
                    onConfirmClick = onRetryClick,
                )
            }
        }
    }

    private fun needShowNetworkFeeCoverageWarningShow(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return quoteModel.currencyCheck?.existentialDeposit == null
    }
}

@Deprecated("Remove with ExpressDataError")
@Suppress("CyclomaticComplexMethod")
internal fun ExpressDataError.toExpressError(): ExpressError = when (this) {
    is ExpressDataError.BadRequest -> ExpressError.BadRequest(code)
    is ExpressDataError.SwapsAreUnavailableNowError -> ExpressError.Forbidden(code)
    is ExpressDataError.ExchangeProviderNotFoundError -> ExpressError.ProviderNotFoundError(code)
    is ExpressDataError.ExchangeProviderNotActiveError -> ExpressError.ProviderNotActiveError(code)
    is ExpressDataError.ExchangeProviderNotAvailableError -> ExpressError.ProviderNotAvailableError(code)
    is ExpressDataError.ExchangeProviderProviderInternalError -> ExpressError.ProviderInternalError(code)
    is ExpressDataError.ExchangeNotPossibleError -> ExpressError.ExchangeNotPossibleError(code)
    is ExpressDataError.ExchangeNotEnoughBalanceError -> ExpressError.NotEnoughBalanceError(code)
    is ExpressDataError.ExchangeInvalidAddressError -> ExpressError.InvalidAddressError(code)
    is ExpressDataError.ExchangeTooSmallAmountError -> ExpressError.AmountError.TooSmallError(code, amount.value)
    is ExpressDataError.ExchangeTooBigAmountError -> ExpressError.AmountError.TooBigError(code, amount.value)
    is ExpressDataError.ExchangeNotEnoughAllowanceError -> ExpressError.AmountError.NotEnoughAllowanceError(
        code = code,
        amount = currentAllowance,
    )
    is ExpressDataError.ExchangeInvalidFromDecimalsError -> ExpressError.InvalidFromDecimalsError(
        code = code,
        receivedFromDecimals = receivedFromDecimals,
        expressFromDecimals = expressFromDecimals,
    )
    is ExpressDataError.ProviderDifferentAmountError -> ExpressError.ProviderDifferentAmountError(
        code = code,
        fromAmount = fromAmount,
        fromProviderAmount = fromProviderAmount,
        decimals = decimals,
    )
    is ExpressDataError.InvalidSignatureError -> ExpressError.InvalidSignatureError(code)
    is ExpressDataError.InvalidRequestIdError -> ExpressError.InvalidRequestIdError(code)
    is ExpressDataError.InvalidPayoutAddressError -> ExpressError.InvalidPayoutAddressError(code)
    is ExpressDataError.UnknownErrorWithCode -> ExpressError.InternalError(code)
    ExpressDataError.UnknownError -> ExpressError.UnknownError
    ExpressDataError.TooLargeSolanaTransactionError -> ExpressError.TooLargeSolanaTransactionError()
    ExpressDataError.DexActiveSupplyError -> ExpressError.DexActiveSupplyError()
}