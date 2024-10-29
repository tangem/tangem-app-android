package com.tangem.common.ui.notifications

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.transaction.Fee
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
import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
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

    fun MutableList<NotificationUM>.addTransactionLimitErrorNotification(
        utxoLimit: UtxoAmountLimit?,
        cryptoCurrency: CryptoCurrency,
        onReduceClick: (
            reduceAmountTo: BigDecimal,
            notification: Class<out NotificationUM>,
        ) -> Unit,
    ) {
        if (utxoLimit != null) {
            add(
                NotificationUM.Error.TransactionLimitError(
                    cryptoCurrency = cryptoCurrency.name,
                    utxoLimit = utxoLimit.maxLimit.toPlainString(),
                    amountLimit = utxoLimit.maxAmount.format { crypto(cryptoCurrency) },
                    onConfirmClick = {
                        onReduceClick(
                            utxoLimit.maxAmount,
                            NotificationUM.Error.TransactionLimitError::class.java,
                        )
                    },
                ),
            )
        }
    }

    fun MutableList<NotificationUM>.addExistentialWarningNotification(
        existentialDeposit: BigDecimal?,
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
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
            receivedAmount
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
            receivedAmount = sendingAmount,
            dustValue = dustValue,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            feeCurrencyStatus = feeCurrencyStatus,
        )
        if (isExceedsLimit) {
            add(
                NotificationUM.Error.MinimumAmountError(
                    amount = dustValue.parseBigDecimal(cryptoCurrencyStatus.currency.decimals),
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
        fee: Fee?,
        validationError: Throwable?,
        cryptoCurrency: CryptoCurrency,
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
            null -> (fee as? Fee.CardanoToken)?.let {
                add(
                    NotificationUM.Cardano.MinAdaValueCharged(
                        tokenName = cryptoCurrency.name,
                        minAdaValue = it.minAdaValue.parseBigDecimal(cryptoCurrency.decimals),
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
                            amount = it.parseBigDecimal(sendingCurrency.decimals),
                        ),
                    )
                }
            }
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

    private fun checkDustLimits(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
        dustValue: BigDecimal,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ): Boolean {
        val change = when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
                balance - (feeAmount + receivedAmount)
            }
            is CryptoCurrency.Token -> {
                val balance = feeCurrencyStatus?.value?.amount ?: BigDecimal.ZERO
                balance - feeAmount
            }
        }

        val isChangeLowerThanDust = change < dustValue && change > BigDecimal.ZERO
        return receivedAmount < dustValue || isChangeLowerThanDust
    }
}
