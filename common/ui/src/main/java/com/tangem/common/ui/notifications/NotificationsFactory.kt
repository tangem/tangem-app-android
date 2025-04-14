package com.tangem.common.ui.notifications

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.uncapped
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.lib.crypto.BlockchainUtils.getTezosThreshold
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

@Suppress("LargeClass")
object NotificationsFactory {

    fun MutableList<NotificationUM>.addFeeUnreachableNotification(
        feeError: GetFeeError?,
        tokenName: String,
        onReload: () -> Unit,
    ) {
        when (feeError) {
            is GetFeeError.BlockchainErrors.TronActivationError -> add(
                NotificationUM.Warning.TronAccountNotActivated(tokenName),
            )
            is GetFeeError.DataError,
            is GetFeeError.UnknownError,
            -> add(
                NotificationUM.Warning.NetworkFeeUnreachable(onReload),
            )
            else -> {
                /* do nothing */
            }
        }
    }

    fun MutableList<NotificationUM>.addFeeUnreachableNotification(
        tokenStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus,
        feeError: GetFeeError?,
        onReload: () -> Unit,
        onClick: (currency: CryptoCurrency) -> Unit,
    ) {
        when (feeError) {
            is GetFeeError.BlockchainErrors.TronActivationError -> add(
                NotificationUM.Warning.TronAccountNotActivated(coinStatus.currency.name),
            )
            is GetFeeError.BlockchainErrors.KaspaZeroUtxo -> add(
                NotificationUM.Error.TokenExceedsBalance(
                    networkIconId = coinStatus.currency.networkIconResId,
                    networkName = coinStatus.currency.name,
                    currencyName = tokenStatus.currency.name,
                    feeName = coinStatus.currency.name,
                    feeSymbol = coinStatus.currency.symbol,
                    mergeFeeNetworkName = false,
                    onClick = {
                        onClick(coinStatus.currency)
                    },
                ),
            )
            is GetFeeError.BlockchainErrors.SuiOneCoinRequired ->
                add(NotificationUM.Sui.NotEnoughCoinForTokenTransaction)
            is GetFeeError.DataError,
            is GetFeeError.UnknownError,
            -> add(
                NotificationUM.Warning.NetworkFeeUnreachable(onReload),
            )
            else -> {
                /* do nothing */
            }
        }
    }

    fun MutableList<NotificationUM>.addExceedBalanceNotification(
        feeAmount: BigDecimal,
        sendingAmount: BigDecimal,
        isSubtractionAvailable: Boolean,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        minimumRequirement: BigDecimal? = null,
    ) {
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        if (!isSubtractionAvailable) return

        val showNotification = sendingAmount + feeAmount > balance - minimumRequirement.orZero()
        if (showNotification) {
            add(NotificationUM.Error.TotalExceedsBalance)
        }
    }

    fun MutableList<NotificationUM>.addReserveAmountErrorNotification(
        reserveAmount: BigDecimal?,
        sendingAmount: BigDecimal,
        cryptoCurrency: CryptoCurrency,
        isAccountFunded: Boolean,
    ) {
        if (!isAccountFunded && reserveAmount != null && reserveAmount > sendingAmount) {
            add(
                NotificationUM.Error.ReserveAmount(
                    reserveAmount.format {
                        crypto(cryptoCurrency)
                    },
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addMinimumAmountErrorNotification(
        minimumSendAmount: BigDecimal?,
        sendingAmount: BigDecimal,
        cryptoCurrency: CryptoCurrency,
    ) {
        if (minimumSendAmount != null && minimumSendAmount > sendingAmount) {
            add(
                NotificationUM.Error.MinimumSendAmountError(
                    amount = minimumSendAmount.format { crypto(cryptoCurrency) },
                ),
            )
        }
    }

    @Suppress("LongParameterList")
    fun MutableList<NotificationUM>.addTransactionLimitErrorNotification(
        currencyCheck: CryptoCurrencyCheck?,
        sendingAmount: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        feeValue: BigDecimal,
        onReduceClick: (
            reduceAmountTo: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val utxoLimit = currencyCheck?.utxoAmountLimit
        val availableToSend = utxoLimit?.availableToSend
        val isDustLimit = checkDustLimits(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            dustValue = currencyCheck?.dustValue.orZero(),
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCurrencyStatus,
        )
        if (availableToSend != null && !feeValue.isZero() && !isDustLimit) {
            add(
                NotificationUM.Error.TransactionLimitError(
                    cryptoCurrency = cryptoCurrency.name,
                    utxoLimit = utxoLimit.limit.toPlainString(),
                    amountLimit = availableToSend.format { crypto(cryptoCurrency) },
                    onConfirmClick = {
                        onReduceClick(
                            availableToSend,
                            NotificationUM.Error.TransactionLimitError::class.java,
                        )
                    },
                ),
            )
        }
    }

    /**
     * Adds Existential Warning
     *
     * @param existentialDeposit existential deposit of blockchain
     * @param feeAmount amount of fee spending for transaction
     * @param sendingAmount amount sending by user (excluding fee for coins)
     * @param cryptoCurrencyStatus blockchain currency status
     * @param onReduceClick action to leave existential amount in balance after transaction
     */
    fun MutableList<NotificationUM>.addExistentialWarningNotification(
        existentialDeposit: BigDecimal?,
        feeAmount: BigDecimal,
        sendingAmount: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        onReduceClick: (
            reduceAmountBy: BigDecimal,
            reduceAmountByDiff: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val balance = cryptoCurrencyStatus.value.amount ?: return
        val spendingAmount = if (cryptoCurrency is CryptoCurrency.Token) {
            feeAmount
        } else {
            sendingAmount + feeAmount
        }
        val diff = balance.minus(spendingAmount)
        if (existentialDeposit != null && diff >= BigDecimal.ZERO && existentialDeposit > diff) {
            add(
                NotificationUM.Error.ExistentialDeposit(
                    deposit = existentialDeposit.format { crypto(cryptoCurrency).uncapped() },
                    onConfirmClick = {
                        onReduceClick(
                            existentialDeposit,
                            existentialDeposit.minus(diff),
                            NotificationUM.Error.ExistentialDeposit::class.java,
                        )
                    },
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addFeeCoverageNotification(
        isFeeCoverage: Boolean,
        amountField: AmountFieldModel,
        sendingValue: BigDecimal,
        appCurrency: AppCurrency,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        val amountValue = amountField.cryptoAmount.value ?: return

        val cryptoDiff = amountValue.minus(sendingValue)
        if (isFeeCoverage) {
            add(
                NotificationUM.Warning.FeeCoverageNotification(
                    cryptoAmount = cryptoDiff.format { crypto(cryptoCurrency).uncapped() },
                    fiatAmount = getFiatString(
                        value = cryptoDiff,
                        rate = fiatRate,
                        appCurrency = appCurrency,
                    ),
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addFeeCoverageNotification(
        isFeeCoverage: Boolean,
        enteredAmountValue: BigDecimal,
        sendingValue: BigDecimal,
        appCurrency: AppCurrency,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val fiatRate = cryptoCurrencyStatus.value.fiatRate

        val cryptoDiff = enteredAmountValue.minus(sendingValue)
        if (isFeeCoverage) {
            add(
                NotificationUM.Warning.FeeCoverageNotification(
                    cryptoAmount = cryptoDiff.format { crypto(cryptoCurrency).uncapped() },
                    fiatAmount = getFiatString(
                        value = cryptoDiff,
                        rate = fiatRate,
                        appCurrency = appCurrency,
                    ),
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addDustWarningNotification(
        dustValue: BigDecimal?,
        feeValue: BigDecimal,
        sendingAmount: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ) {
        if (dustValue == null) return
        val isExceedsLimit = checkDustLimits(
            feeAmount = feeValue,
            sendingAmount = sendingAmount,
            dustValue = dustValue,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCurrencyStatus,
        )
        if (isExceedsLimit) {
            add(
                NotificationUM.Error.MinimumAmountError(
                    amount = dustValue.format { crypto(cryptoCurrencyStatus.currency) },
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addExceedsBalanceNotification(
        cryptoCurrencyWarning: CryptoCurrencyWarning?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        shouldMergeFeeNetworkName: Boolean,
        onClick: (CryptoCurrency) -> Unit,
        onAnalyticsEvent: (CryptoCurrency) -> Unit,
    ) {
        when (cryptoCurrencyWarning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> {
                add(
                    NotificationUM.Error.TokenExceedsBalance(
                        networkIconId = cryptoCurrencyWarning.coinCurrency.networkIconResId,
                        networkName = cryptoCurrencyWarning.coinCurrency.name,
                        currencyName = cryptoCurrencyStatus.currency.name,
                        feeName = cryptoCurrencyWarning.coinCurrency.name,
                        feeSymbol = cryptoCurrencyWarning.coinCurrency.symbol,
                        mergeFeeNetworkName = shouldMergeFeeNetworkName,
                        onClick = {
                            onClick(cryptoCurrencyWarning.coinCurrency)
                        },
                    ),
                )
                onAnalyticsEvent(cryptoCurrencyStatus.currency)
            }
            is CryptoCurrencyWarning.CustomTokenNotEnoughForFee -> {
                val currency = cryptoCurrencyWarning.feeCurrency
                add(
                    NotificationUM.Error.TokenExceedsBalance(
                        networkIconId = currency?.networkIconResId ?: R.drawable.ic_alert_24,
                        currencyName = cryptoCurrencyWarning.currency.name,
                        feeName = cryptoCurrencyWarning.feeCurrencyName,
                        feeSymbol = cryptoCurrencyWarning.feeCurrencySymbol,
                        networkName = cryptoCurrencyWarning.networkName,
                        mergeFeeNetworkName = shouldMergeFeeNetworkName,
                        onClick = {
                            currency?.let {
                                onClick(currency)
                            }
                        },
                    ),
                )
                onAnalyticsEvent(cryptoCurrencyWarning.currency)
            }
            else -> Unit
        }
    }

    fun MutableList<NotificationUM>.addValidateTransactionNotifications(
        dustValue: BigDecimal,
        validationError: Throwable?,
        cryptoCurrency: CryptoCurrency,
        minAdaValue: BigDecimal?, // TODO revert to Fee, after swap TxFee refactored
        onReduceClick: (
            reduceAmountTo: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
    ) {
        when (validationError) {
            is BlockchainSdkError.Cardano -> addCardanoTransactionValidationError(
                error = validationError,
                sendingCurrency = cryptoCurrency,
                dustValue = dustValue,
            )
            is BlockchainSdkError.Koinos -> addKoinosTransactionValidationError(
                error = validationError,
                onReduceClick = onReduceClick,
            )
            is BlockchainSdkError.XRP -> addXRPTransactionValidationError(
                error = validationError,
            )
            null -> minAdaValue?.let {
                add(
                    NotificationUM.Cardano.MinAdaValueCharged(
                        tokenName = cryptoCurrency.name,
                        minAdaValue = minAdaValue.parseBigDecimal(cryptoCurrency.decimals),
                    ),
                )
            }
            else -> return
        }
    }

    private fun MutableList<NotificationUM>.addCardanoTransactionValidationError(
        error: BlockchainSdkError.Cardano,
        sendingCurrency: CryptoCurrency,
        dustValue: BigDecimal?,
    ) {
        when (error) {
            BlockchainSdkError.Cardano.InsufficientMinAdaBalanceToSendToken -> {
                add(NotificationUM.Cardano.InsufficientBalanceToTransferToken(sendingCurrency.name))
            }
            BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens -> {
                when (sendingCurrency) {
                    is CryptoCurrency.Coin -> NotificationUM.Cardano.InsufficientBalanceToTransferCoin
                    is CryptoCurrency.Token -> {
                        NotificationUM.Cardano.InsufficientBalanceToTransferToken(sendingCurrency.name)
                    }
                }.let(::add)
            }
            BlockchainSdkError.Cardano.InsufficientRemainingBalance,
            BlockchainSdkError.Cardano.InsufficientSendingAdaAmount,
            -> {
                dustValue?.let {
                    add(
                        NotificationUM.Error.MinimumAmountError(
                            amount = it.format { crypto(sendingCurrency) },
                        ),
                    )
                }
            }
            else -> return
        }
    }

    private fun MutableList<NotificationUM>.addKoinosTransactionValidationError(
        error: BlockchainSdkError.Koinos,
        onReduceClick: (
            reduceAmountTo: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
    ) {
        when (error) {
            is BlockchainSdkError.Koinos.InsufficientBalance -> {
                add(NotificationUM.Koinos.InsufficientBalance)
            }
            is BlockchainSdkError.Koinos.InsufficientMana -> {
                add(
                    NotificationUM.Koinos.InsufficientRecoverableMana(
                        mana = error.manaBalance ?: BigDecimal.ZERO,
                        maxMana = error.maxMana ?: BigDecimal.ZERO,
                    ),
                )
            }
            is BlockchainSdkError.Koinos.ManaFeeExceedsBalance -> {
                add(
                    NotificationUM.Koinos.ManaExceedsBalance(
                        availableKoinForTransfer = error.availableKoinForTransfer,
                        onReduceClick = {
                            onReduceClick(
                                error.availableKoinForTransfer,
                                NotificationUM.Koinos.InsufficientRecoverableMana::class.java,
                            )
                        },
                    ),
                )
            }
            else -> {}
        }
    }

    private fun MutableList<NotificationUM>.addXRPTransactionValidationError(error: BlockchainSdkError.XRP) {
        when (error) {
            is BlockchainSdkError.XRP.DestinationMemoRequired -> addRequireDestinationFlagErrorNotification()
        }
    }

    fun MutableList<NotificationUM>.addRentExemptionNotification(rentWarning: CryptoCurrencyWarning.Rent?) {
        if (rentWarning == null) return
        add(NotificationUM.Solana.RentInfo(rentWarning))
    }

    fun MutableList<NotificationUM>.addHighFeeWarningNotification(
        enteredAmountValue: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        ignoreAmountReduce: Boolean,
        onReduceClick: (
            reduceAmountBy: BigDecimal,
            reduceAmountByDiff: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
        onCloseClick: (Class<out NotificationUM>) -> Unit,
    ) {
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.id.value)
        val threshold = getTezosThreshold()
        val isTotalBalance = enteredAmountValue >= balance && balance > threshold
        if (!ignoreAmountReduce && isTotalBalance && isTezos) {
            add(
                NotificationUM.Warning.HighFeeError(
                    currencyName = cryptoCurrencyStatus.currency.name,
                    amount = threshold.toPlainString(),
                    onConfirmClick = {
                        onReduceClick(
                            threshold,
                            threshold,
                            NotificationUM.Warning.HighFeeError::class.java,
                        )
                    },
                    onCloseClick = {
                        onCloseClick(NotificationUM.Warning.HighFeeError::class.java)
                    },
                ),
            )
        }
    }

    private fun checkDustLimits(
        feeAmount: BigDecimal,
        sendingAmount: BigDecimal,
        dustValue: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ): Boolean {
        val change = when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                val balance = cryptoCurrencyStatus.value.amount.orZero()
                balance - (feeAmount + sendingAmount)
            }
            is CryptoCurrency.Token -> {
                val balance = feeCurrencyStatus?.value?.amount.orZero()
                balance - feeAmount
            }
        }

        val dust = when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> dustValue
            is CryptoCurrency.Token -> BigDecimal.ZERO
        }

        val isChangeLowerThanDust = change < dust && change > BigDecimal.ZERO
        return when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> sendingAmount < dust || isChangeLowerThanDust
            is CryptoCurrency.Token -> isChangeLowerThanDust
        }
    }

    private fun MutableList<NotificationUM>.addRequireDestinationFlagErrorNotification() {
        add(NotificationUM.Error.DestinationMemoRequired)
    }
}