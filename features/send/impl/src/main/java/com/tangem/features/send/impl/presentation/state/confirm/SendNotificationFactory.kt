package com.tangem.features.send.impl.presentation.state.confirm

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.minimalAmount
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.usecase.ValidateTransactionUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.Provider
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Suppress("LongParameterList", "LargeClass")
internal class SendNotificationFactory(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val stateRouterProvider: Provider<StateRouter>,
    private val isSubtractAvailableProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: SendClickIntents,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val validateTransactionUseCase: ValidateTransactionUseCase,
) {

    fun create(): Flow<ImmutableList<SendNotification>> = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Send }
        .map {
            val state = currentStateProvider()
            val isEditState = stateRouterProvider().isEditState
            val balance = cryptoCurrencyStatusProvider().value.amount.orZero()
            val sendState = state.sendState ?: return@map persistentListOf()
            val feeState = state.getFeeState(isEditState) ?: return@map persistentListOf()
            val amountState = state.getAmountState(isEditState) as? AmountState.Data ?: return@map persistentListOf()

            val recipientAddress = state.recipientState?.addressTextField?.value.orEmpty()
            val amountValue = amountState.amountTextField.cryptoAmount.value.orZero()
            val feeValue = feeState.fee?.amount?.value.orZero()
            val reduceAmountBy = sendState.reduceAmountBy.orZero()
            val isFeeCoverage = checkFeeCoverage(
                isSubtractAvailable = isSubtractAvailableProvider(),
                balance = balance,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = reduceAmountBy,
            )
            val sendingAmount = checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isFeeCoverage,
                cryptoCurrencyStatus = cryptoCurrencyStatusProvider(),
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = reduceAmountBy,
            )
            buildList {
                // errors
                addFeeUnreachableNotification(feeState.feeSelectorState)
                addExceedBalanceNotification(feeValue, sendingAmount)
                addExceedsBalanceNotification(feeState.fee)
                addDustWarningNotificationForSpecificBlockchains(feeValue, sendingAmount)
                addTransactionLimitErrorNotification(feeValue, sendingAmount)
                addReserveAmountErrorNotification(recipientAddress, sendingAmount)

                // warnings
                addExistentialWarningNotification(feeValue, amountValue)
                addFeeCoverageNotification(
                    isFeeCoverage = isFeeCoverage,
                    amountField = amountState.amountTextField,
                    sendingValue = sendingAmount,
                )
                addHighFeeWarningNotification(amountValue, sendState.ignoreAmountReduce)
                addTooHighNotification(feeState.feeSelectorState)
                addTooLowNotification(feeState)

                // blockchain specific
                addValidateTransactionNotifications(
                    sendingAmount = sendingAmount,
                    fee = feeState.fee,
                    state = state,
                )
            }.toImmutableList()
        }

    fun dismissNotificationState(clazz: Class<out SendNotification>, isIgnored: Boolean = false): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        val notificationsToRemove = sendState.notifications.filterIsInstance(clazz)
        val updatedNotifications = sendState.notifications.toMutableList()
        updatedNotifications.removeAll(notificationsToRemove)
        return state.copy(
            sendState = sendState.copy(
                ignoreAmountReduce = isIgnored,
                reduceAmountBy = if (isIgnored) null else sendState.reduceAmountBy,
                notifications = updatedNotifications.toImmutableList(),
            ),
        )
    }

    private fun MutableList<SendNotification>.addFeeUnreachableNotification(feeSelectorState: FeeSelectorState) {
        when (feeSelectorState) {
            is FeeSelectorState.Error.TronAccountActivationError -> add(
                SendNotification.Warning.TronAccountNotActivated(
                    feeSelectorState.tokenName,
                ),
            )
            is FeeSelectorState.Error.NetworkError -> add(
                SendNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload),
            )
            else -> {
                /* do nothing */
            }
        }
    }

    private fun MutableList<SendNotification>.addExceedBalanceNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO

        if (!isSubtractAvailableProvider()) return

        val showNotification = receivedAmount + feeAmount > balance
        if (showNotification) {
            add(SendNotification.Error.TotalExceedsBalance)
        }
    }

    private suspend fun MutableList<SendNotification>.addReserveAmountErrorNotification(
        recipientAddress: String,
        sendingAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val isAccountFunded = currencyChecksRepository.checkIfAccountFunded(
            userWalletId,
            cryptoCurrency.network,
            recipientAddress,
        )
        val minimumAmount = currencyChecksRepository.getReserveAmount(userWalletId, cryptoCurrency.network)
        if (!isAccountFunded && minimumAmount != null && minimumAmount > sendingAmount) {
            add(
                SendNotification.Error.ReserveAmount(
                    BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = minimumAmount,
                        cryptoCurrency = cryptoCurrency,
                    ),
                ),
            )
        }
    }

    private suspend fun MutableList<SendNotification>.addTransactionLimitErrorNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val utxoLimit = currencyChecksRepository.checkUtxoAmountLimit(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            amount = receivedAmount,
            fee = feeAmount,
        )

        if (utxoLimit != null) {
            add(
                SendNotification.Error.TransactionLimitError(
                    cryptoCurrency = cryptoCurrency.name,
                    utxoLimit = utxoLimit.maxLimit.toPlainString(),
                    amountLimit = BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = utxoLimit.maxAmount,
                        cryptoCurrency = cryptoCurrency,
                    ),
                    onConfirmClick = {
                        clickIntents.onAmountReduceClick(
                            reduceAmountTo = utxoLimit.maxAmount,
                            clazz = SendNotification.Error.TransactionLimitError::class.java,
                        )
                    },
                ),
            )
        }
    }

    private suspend fun MutableList<SendNotification>.addExistentialWarningNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val balance = cryptoCurrencyStatus.value.amount ?: return
        val spendingAmount = if (cryptoCurrency is CryptoCurrency.Token) {
            feeAmount
        } else {
            receivedAmount
        }
        val currencyDeposit = currencyChecksRepository.getExistentialDeposit(
            userWalletId,
            cryptoCurrency.network,
        )
        val diff = balance.minus(spendingAmount)
        if (currencyDeposit != null && diff >= BigDecimal.ZERO && currencyDeposit > diff) {
            add(
                SendNotification.Error.ExistentialDeposit(
                    deposit = BigDecimalFormatter.formatCryptoAmountUncapped(
                        cryptoAmount = currencyDeposit,
                        cryptoCurrency = cryptoCurrency,
                    ),
                    onConfirmClick = {
                        clickIntents.onAmountReduceClick(
                            reduceAmountByDiff = currencyDeposit.minus(diff),
                            reduceAmountBy = currencyDeposit,
                            clazz = SendNotification.Error.ExistentialDeposit::class.java,
                        )
                    },
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addFeeCoverageNotification(
        isFeeCoverage: Boolean,
        amountField: AmountFieldModel,
        sendingValue: BigDecimal,
    ) {
        if (isFeeCoverage) {
            analyticsEventHandler.send(SendAnalyticEvents.NoticeFeeCoverage)
            val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
            val cryptoCurrency = cryptoCurrencyStatus.currency
            val fiatRate = cryptoCurrencyStatus.value.fiatRate
            val amountValue = amountField.cryptoAmount.value ?: return

            val cryptoDiff = amountValue.minus(sendingValue)
            add(
                SendNotification.Warning.FeeCoverageNotification(
                    cryptoAmount = BigDecimalFormatter.formatCryptoAmountUncapped(
                        cryptoAmount = cryptoDiff,
                        cryptoCurrency = cryptoCurrency,
                    ),
                    fiatAmount = getFiatString(
                        value = cryptoDiff,
                        rate = fiatRate,
                        appCurrency = appCurrencyProvider(),
                    ),
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addHighFeeWarningNotification(
        sendAmount: BigDecimal,
        ignoreAmountReduce: Boolean,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.id.value)
        val threshold = Blockchain.Tezos.minimalAmount()
        val isTotalBalance = sendAmount >= balance && balance > threshold
        if (!ignoreAmountReduce && isTotalBalance && isTezos) {
            add(
                SendNotification.Warning.HighFeeError(
                    currencyName = cryptoCurrencyStatus.currency.name,
                    amount = threshold.toPlainString(),
                    onConfirmClick = {
                        clickIntents.onAmountReduceClick(
                            reduceAmountBy = threshold,
                            clazz = SendNotification.Warning.HighFeeError::class.java,
                        )
                    },
                    onCloseClick = {
                        clickIntents.onNotificationCancel(SendNotification.Warning.HighFeeError::class.java)
                    },
                ),
            )
        }
    }

    private suspend fun MutableList<SendNotification>.addDustWarningNotificationForSpecificBlockchains(
        feeValue: BigDecimal,
        sendingAmount: BigDecimal,
    ) {
        val isCardano = BlockchainUtils.isCardano(cryptoCurrencyStatusProvider().currency.network.id.value)

        if (!isCardano) {
            addDustWarningNotification(feeValue, sendingAmount)
        }
    }

    private fun checkDustLimits(feeAmount: BigDecimal, receivedAmount: BigDecimal, dustValue: BigDecimal): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val feeCurrencyStatus = feeCryptoCurrencyStatusProvider() ?: return false

        val change = when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
                balance - (feeAmount + receivedAmount)
            }
            is CryptoCurrency.Token -> {
                val balance = feeCurrencyStatus.value.amount ?: BigDecimal.ZERO
                balance - feeAmount
            }
        }

        val isChangeLowerThanDust = change < dustValue && change > BigDecimal.ZERO
        return receivedAmount < dustValue || isChangeLowerThanDust
    }

    private fun MutableList<SendNotification>.addTooLowNotification(feeState: SendStates.FeeState) {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
        val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return
        val minimumValue = multipleFees.minimum.amount.value ?: return
        val customAmount = feeSelectorState.customValues.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        if (feeSelectorState.selectedFee == FeeType.Custom && minimumValue > customValue) {
            add(SendNotification.Warning.FeeTooLow)
            analyticsEventHandler.send(
                SendAnalyticEvents.NoticeTransactionDelays(
                    cryptoCurrencyStatusProvider().currency.symbol,
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addTooHighNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState !is FeeSelectorState.Content) return

        checkIfFeeTooHigh(feeSelectorState) { diff ->
            add(SendNotification.Warning.TooHigh(diff))
        }
    }

    private suspend fun MutableList<SendNotification>.addExceedsBalanceNotification(fee: Fee?) {
        val feeValue = fee?.amount?.value ?: BigDecimal.ZERO
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val feeCurrencyStatus = feeCryptoCurrencyStatusProvider() ?: return

        val warning = getBalanceNotEnoughForFeeWarningUseCase(
            fee = feeValue,
            userWalletId = userWalletId,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = feeCurrencyStatus,
        ).getOrNull() ?: return

        val mergeFeeNetworkName = cryptoCurrencyStatus.shouldMergeFeeNetworkName()
        when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> {
                add(
                    SendNotification.Error.ExceedsBalance(
                        networkIconId = warning.coinCurrency.networkIconResId,
                        networkName = warning.coinCurrency.name,
                        currencyName = cryptoCurrencyStatus.currency.name,
                        feeName = warning.coinCurrency.name,
                        feeSymbol = warning.coinCurrency.symbol,
                        mergeFeeNetworkName = mergeFeeNetworkName,
                        onClick = {
                            clickIntents.onTokenDetailsClick(
                                userWalletId = userWalletId,
                                currency = warning.coinCurrency,
                            )
                        },
                    ),
                )
                analyticsEventHandler.send(
                    SendAnalyticEvents.NoticeNotEnoughFee(
                        token = cryptoCurrencyStatus.currency.symbol,
                        blockchain = cryptoCurrencyStatus.currency.network.name,
                    ),
                )
            }
            is CryptoCurrencyWarning.CustomTokenNotEnoughForFee -> {
                val currency = warning.feeCurrency
                add(
                    SendNotification.Error.ExceedsBalance(
                        networkIconId = currency?.networkIconResId ?: R.drawable.ic_alert_24,
                        currencyName = warning.currency.name,
                        feeName = warning.feeCurrencyName,
                        feeSymbol = warning.feeCurrencySymbol,
                        networkName = warning.networkName,
                        mergeFeeNetworkName = mergeFeeNetworkName,
                        onClick = currency?.let {
                            {
                                clickIntents.onTokenDetailsClick(
                                    userWalletId,
                                    currency,
                                )
                            }
                        },
                    ),
                )
                analyticsEventHandler.send(
                    SendAnalyticEvents.NoticeNotEnoughFee(
                        token = warning.currency.symbol,
                        blockchain = warning.networkName,
                    ),
                )
            }
            else -> Unit
        }
    }

    // workaround for networks that users have misunderstanding
    private fun CryptoCurrencyStatus.shouldMergeFeeNetworkName(): Boolean {
        return Blockchain.fromNetworkId(this.currency.network.backendId) == Blockchain.Arbitrum
    }

    private suspend fun MutableList<SendNotification>.addValidateTransactionNotifications(
        sendingAmount: BigDecimal,
        fee: Fee?,
        state: SendUiState,
    ) {
        val sendingCurrency = cryptoCurrencyStatusProvider().currency

        validateTransactionUseCase(
            amount = sendingAmount.convertToSdkAmount(sendingCurrency),
            fee = fee ?: return,
            memo = state.recipientState?.memoTextField?.value,
            destination = requireNotNull(state.recipientState?.addressTextField?.value),
            userWalletId = userWalletProvider().walletId,
            network = sendingCurrency.network,
        ).fold(
            ifLeft = {
                when (it) {
                    is BlockchainSdkError.Cardano -> addCardanoTransactionValidationError(
                        error = it,
                        sendingCurrency = sendingCurrency,
                    )
                    is BlockchainSdkError.Koinos -> addKoinosTransactionValidationError(error = it)
                    else -> return
                }
            },
            ifRight = {
                (fee as? Fee.CardanoToken)?.let {
                    add(
                        SendNotification.Cardano.MinAdaValueCharged(
                            tokenName = sendingCurrency.name,
                            minAdaValue = it.minAdaValue.parseBigDecimal(sendingCurrency.decimals),
                        ),
                    )
                }
            },
        )
    }

    private suspend fun MutableList<SendNotification>.addCardanoTransactionValidationError(
        error: BlockchainSdkError.Cardano,
        sendingCurrency: CryptoCurrency,
    ) {
        when (error) {
            BlockchainSdkError.Cardano.InsufficientMinAdaBalanceToSendToken -> {
                add(SendNotification.Cardano.InsufficientBalanceToTransferToken(sendingCurrency.name))
            }
            BlockchainSdkError.Cardano.InsufficientRemainingBalanceToWithdrawTokens -> {
                when (sendingCurrency) {
                    is CryptoCurrency.Coin -> SendNotification.Cardano.InsufficientBalanceToTransferCoin
                    is CryptoCurrency.Token -> {
                        SendNotification.Cardano.InsufficientBalanceToTransferToken(sendingCurrency.name)
                    }
                }.let(::add)
            }
            BlockchainSdkError.Cardano.InsufficientRemainingBalance,
            BlockchainSdkError.Cardano.InsufficientSendingAdaAmount,
            -> {
                val dustValue = currencyChecksRepository.getDustValue(
                    userWalletId = userWalletProvider().walletId,
                    network = sendingCurrency.network,
                ) ?: return

                add(
                    SendNotification.Error.MinimumAmountError(
                        amount = dustValue.parseBigDecimal(sendingCurrency.decimals),
                    ),
                )
            }
        }
    }

    private fun MutableList<SendNotification>.addKoinosTransactionValidationError(error: BlockchainSdkError.Koinos) {
        when (error) {
            is BlockchainSdkError.Koinos.InsufficientBalance -> {
                add(SendNotification.Koinos.InsufficientBalance)
            }
            is BlockchainSdkError.Koinos.InsufficientMana -> {
                add(
                    SendNotification.Koinos.InsufficientRecoverableMana(
                        mana = error.manaBalance ?: BigDecimal.ZERO,
                        maxMana = error.maxMana ?: BigDecimal.ZERO,
                    ),
                )
            }
            is BlockchainSdkError.Koinos.ManaFeeExceedsBalance -> {
                add(
                    SendNotification.Koinos.ManaExceedsBalance(
                        availableKoinForTransfer = error.availableKoinForTransfer,
                        onReduceClick = {
                            clickIntents.onAmountReduceClick(
                                reduceAmountTo = error.availableKoinForTransfer,
                                clazz = SendNotification.Koinos.InsufficientRecoverableMana::class.java,
                            )
                        },
                    ),
                )
            }
            else -> {}
        }
    }

    private suspend fun MutableList<SendNotification>.addDustWarningNotification(
        feeValue: BigDecimal,
        sendingAmount: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val dustValue = currencyChecksRepository.getDustValue(
            userWalletProvider().walletId,
            cryptoCurrencyStatus.currency.network,
        ) ?: return

        if (checkDustLimits(feeValue, sendingAmount, dustValue)) {
            add(
                SendNotification.Error.MinimumAmountError(
                    amount = dustValue.parseBigDecimal(cryptoCurrencyStatus.currency.decimals),
                ),
            )
        }
    }
}
