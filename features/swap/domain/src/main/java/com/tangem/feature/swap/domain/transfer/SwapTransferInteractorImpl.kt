package com.tangem.feature.swap.domain.transfer

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tokens.GetCurrencyCheckUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkAndCalculateSubtractedAmount
import com.tangem.features.send.v2.api.subcomponents.feeSelector.utils.FeeCalculationUtils.checkFeeCoverage
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class SwapTransferInteractorImpl @Inject constructor(
    private val swapFeatureToggles: SwapFeatureToggles,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
    private val getCurrencyCheckUseCase: GetCurrencyCheckUseCase,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val tangemPayWithdrawUseCase: TangemPayWithdrawUseCase,
) : SwapTransferInteractor {

    override suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
        feePaidCurrencyStatus: CryptoCurrencyStatus?,
        fee: Fee?,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val isBalanceHidden = getBalanceHidingSettingsUseCase.isBalanceHidden().first()
        val isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()
        val fromTokenAmountValue = fromTokenAmount.parseBigDecimalOrNull() ?: return createEmptyAmountState(appCurrency)
        val fromTokenAmountFiat = fromSwapCurrencyStatus.status.value.fiatRate.orZero() * fromTokenAmountValue
        val fromTokenBalance = fromSwapCurrencyStatus.status.value.amount.orZero()
        val userWallet = toSwapCurrencyStatus.userWallet

        val fromTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(fromTokenAmountValue, fromToken.decimals),
            swapCurrencyStatus = fromSwapCurrencyStatus,
            amountFiat = fromTokenAmountFiat,
        )
        // it is the same with fromToken
        val toTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(fromTokenAmountValue, toToken.decimals),
            swapCurrencyStatus = toSwapCurrencyStatus,
            amountFiat = fromTokenAmountFiat,
        )
        // Mirrors legacy manageWarnings in SwapInteractorImpl.applySwapFee: when the fee is paid in
        // a token different from the from-token, the fee is deducted from a separate balance, so
        // it must not be subtracted from the from-token balance here.
        val feePaidCurrency = feePaidCurrencyStatus?.currency
        val isFeeInOtherToken = feePaidCurrency is CryptoCurrency.Token && feePaidCurrency.id != fromToken.id
        val warningsFee = if (isFeeInOtherToken) BigDecimal.ZERO else fee?.amount?.value.orZero()
        val currencyCheck = getCurrencyCheckUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            currencyStatus = fromSwapCurrencyStatus.status,
            feeCurrencyStatus = feePaidCurrencyStatus,
            amount = fromTokenAmountValue,
            fee = warningsFee,
            feeCurrencyBalanceAfterTransaction = null,
        )
        val (isFeeCoverage, sendingAmount) = getCoverageState(
            fromTokenInfo = fromTokenInfo,
            userWallet = userWallet,
            fee = fee,
            currencyCheck = currencyCheck,
        )
        return SwapState.Transfer(
            userWallet = userWallet,
            fromTokenInfo = fromTokenInfo,
            toTokenInfo = toTokenInfo,
            isInsufficientBalance = fromTokenAmountValue > fromTokenBalance,
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsMode = isAccountsMode,
            isFeeCoverage = isFeeCoverage,
            sendingAmount = sendingAmount,
            currencyCheck = currencyCheck,
        )
    }

    private suspend fun getCoverageState(
        fromTokenInfo: TokenSwapInfo,
        userWallet: UserWallet,
        fee: Fee?,
        currencyCheck: CryptoCurrencyCheck,
    ): Pair<Boolean, BigDecimal> {
        val swapCurrencyStatus = fromTokenInfo.swapCurrencyStatus
        val isAmountSubtractAvailable = isAmountSubtractAvailable(
            userWalletId = userWallet.walletId,
            currency = swapCurrencyStatus.currency,
            fee = fee,
        )
        val balance = swapCurrencyStatus.status.value.amount ?: BigDecimal.ZERO
        val reduceAmountBy = currencyCheck.existentialDeposit.orZero()
        val amount = fromTokenInfo.tokenAmount
        val feeValue = fee?.amount?.value.orZero()
        val isFeeCoverage = checkFeeCoverage(
            isSubtractAvailable = isAmountSubtractAvailable,
            balance = balance,
            amountValue = amount.value,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )
        val sendingAmount = checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = fromTokenInfo.swapCurrencyStatus.status,
            amountValue = amount.value,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )
        return isFeeCoverage to sendingAmount
    }

    private suspend fun isAmountSubtractAvailable(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        fee: Fee?,
    ): Boolean {
        val feeCurrencyId = currency.id
        return isAmountSubtractAvailableUseCase(
            userWalletId = userWalletId,
            currency = currency,
            maybeGaslessFee = fee?.let { feeCurrencyId to fee },
        ).getOrElse { false }
    }

    private fun createEmptyAmountState(appCurrency: AppCurrency): SwapState.EmptyAmountState {
        return SwapState.EmptyAmountState(
            zeroAmountEquivalent = stringReference(
                BigDecimal.ZERO.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
            isTransferMode = true,
        )
    }

    override fun shouldTransferInsteadOfSwap(
        fromSwapCurrency: CryptoCurrency?,
        toSwapCurrency: CryptoCurrency?,
    ): Boolean {
        if (swapFeatureToggles.isSwapSwitchToTransferEnabled.not()) return false
        val isSameCurrency = when {
            fromSwapCurrency is CryptoCurrency.Coin && toSwapCurrency is CryptoCurrency.Coin -> {
                fromSwapCurrency.network.rawId == toSwapCurrency.network.rawId
            }
            fromSwapCurrency is CryptoCurrency.Token && toSwapCurrency is CryptoCurrency.Token -> {
                val isContractAddressSame = fromSwapCurrency.contractAddress == toSwapCurrency.contractAddress
                fromSwapCurrency.network.rawId == toSwapCurrency.network.rawId && isContractAddressSame
            }
            else -> false
        }
        return isSameCurrency
    }

    override suspend fun loadFee(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): Either<GetFeeError, TransactionFee> {
        val amount = fromTokenAmount.parseBigDecimalOrNull() ?: BigDecimal.ZERO
        val destination = toSwapCurrencyStatus.destinationAddress() ?: return feeDataError(
            message = "Destination address is null",
        )

        return getFeeUseCase(
            amount = amount,
            destination = destination,
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoCurrency = fromSwapCurrencyStatus.currency,
        )
    }

    override suspend fun loadFeeExtended(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): Either<GetFeeError, TransactionFeeExtended> {
        val amount = fromTokenAmount.parseBigDecimalOrNull() ?: BigDecimal.ZERO
        val destination = toSwapCurrencyStatus.destinationAddress() ?: return feeDataError(
            message = "Destination address is null",
        )
        val userWallet = fromSwapCurrencyStatus.userWallet
        val currency = fromSwapCurrencyStatus.currency

        val transactionData = createTransferTransactionUseCase(
            amount = amount.convertToSdkAmount(
                cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            ),
            memo = null,
            destination = destination,
            userWalletId = userWallet.walletId,
            network = currency.network,
        ).getOrNull() ?: return feeDataError("Failed to build transfer transaction")

        return getFeeForGaslessUseCase(
            userWallet = userWallet,
            network = currency.network,
            transactionData = transactionData,
        )
    }

    override suspend fun sendTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        sendingAmount: BigDecimal,
        fee: Fee,
        transactionFeeResult: TransactionFeeResult,
    ): Either<SendTransactionError, String> {
        val destination = toSwapCurrencyStatus.destinationAddress() ?: return getDataError(
            message = "Destination address is null",
        )
        val userWallet = fromSwapCurrencyStatus.userWallet
        val currency = fromSwapCurrencyStatus.currency

        val txData = createTransferTransactionUseCase(
            amount = sendingAmount.convertToSdkAmount(cryptoCurrencyStatus = fromSwapCurrencyStatus.status),
            fee = fee,
            memo = null,
            destination = destination,
            userWalletId = userWallet.walletId,
            network = currency.network,
        ).getOrNull() ?: return getDataError(
            message = "Failed to build transfer transaction",
        )

        return sendTransferForFeeType(
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            transactionFeeResult = transactionFeeResult,
            txData = txData,
        )
    }

    override suspend fun withdrawTangemPay(
        userWallet: UserWallet,
        cryptoAmount: BigDecimal,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ): Either<SendTransactionError, WithdrawalResult> {
        val destination = toSwapCurrencyStatus.destinationAddress() ?: return getDataError(
            message = "Destination address is null",
        )
        val cryptoCurrencyId = toSwapCurrencyStatus.currency.id.rawCurrencyId ?: return getDataError(
            message = "Crypto currency id should be null",
        )
        return tangemPayWithdrawUseCase(
            userWallet = userWallet,
            cryptoAmount = cryptoAmount,
            cryptoCurrencyId = cryptoCurrencyId,
            receiverCexAddress = destination,
        ).mapLeft { error ->
            SendTransactionError.DataError("Tangem Pay withdrawal error code is ${error.errorCode}")
        }
    }

    private fun getDataError(message: String): Either<SendTransactionError.DataError, Nothing> {
        return SendTransactionError.DataError(message).left()
    }

    private suspend fun sendTransferForFeeType(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        transactionFeeResult: TransactionFeeResult,
        txData: TransactionData,
    ): Either<SendTransactionError, String> {
        val isToken = cryptoCurrencyStatus.currency is CryptoCurrency.Token
        val isGaslessToken = isToken && transactionFeeResult is TransactionFeeResult.LoadedExtended
        return if (isGaslessToken) {
            createAndSendGaslessTransactionUseCase(
                transactionData = txData,
                userWallet = userWallet,
                fee = transactionFeeResult.fee,
            )
        } else {
            sendTransactionUseCase(
                txData = txData,
                userWallet = userWallet,
                network = cryptoCurrencyStatus.currency.network,
            )
        }
    }

    private fun feeDataError(message: String): Either<GetFeeError, Nothing> {
        return GetFeeError.DataError(IllegalStateException(message)).left()
    }

    private fun SwapCurrencyStatus.destinationAddress(): String? {
        return status.value.networkAddress?.defaultAddress?.value
    }
}