package com.tangem.feature.swap.domain.transfer

import arrow.core.Either
import arrow.core.left
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
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
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
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
) : SwapTransferInteractor {

    override suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val isBalanceHidden = getBalanceHidingSettingsUseCase.isBalanceHidden().first()
        val isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()
        val fromTokenAmountValue = fromTokenAmount.parseBigDecimalOrNull() ?: return createEmptyAmountState(appCurrency)
        val fromTokenAmountFiat = fromSwapCurrencyStatus.status.value.fiatRate.orZero() * fromTokenAmountValue

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
        return SwapState.Transfer(
            userWallet = toSwapCurrencyStatus.userWallet,
            fromTokenInfo = fromTokenInfo,
            toTokenInfo = toTokenInfo,
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsMode = isAccountsMode,
        )
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
        val amount = fromTokenAmount.parseBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?: return feeDataError("Can't parse fromTokenAmount: $fromTokenAmount")
        val destination = toSwapCurrencyStatus.destinationAddress()
            ?: return feeDataError("Destination address is null")

        return getFeeUseCase(
            amount = amount,
            destination = destination,
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoCurrency = fromSwapCurrencyStatus.currency,
        )
    }

    override suspend fun loadFeeForGasless(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): Either<GetFeeError, TransactionFeeExtended> {
        val amount = fromTokenAmount.parseBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?: return feeDataError("Can't parse fromTokenAmount: $fromTokenAmount")
        val destination = toSwapCurrencyStatus.destinationAddress()
            ?: return feeDataError("Destination address is null")
        val userWallet = fromSwapCurrencyStatus.userWallet
        val currency = fromSwapCurrencyStatus.currency

        val transactionData = createTransferTransactionUseCase(
            amount = currency.toBlockchainAmount(amount),
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

    private fun feeDataError(message: String): Either<GetFeeError, Nothing> {
        return GetFeeError.DataError(IllegalStateException(message)).left()
    }

    private fun SwapCurrencyStatus.destinationAddress(): String? {
        return status.value.networkAddress?.defaultAddress?.value
    }

    private fun CryptoCurrency.toBlockchainAmount(value: BigDecimal): Amount = Amount(
        currencySymbol = symbol,
        value = value,
        decimals = decimals,
        type = when (this) {
            is CryptoCurrency.Coin -> AmountType.Coin
            is CryptoCurrency.Token -> AmountType.Token(
                token = Token(
                    symbol = symbol,
                    contractAddress = contractAddress,
                    decimals = decimals,
                ),
            )
        },
    )
}