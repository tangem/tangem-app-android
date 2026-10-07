package com.tangem.feature.swap.domain.fee

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.blockchain.common.address.EstimationFeeAddressFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.isTronActivationRefusal
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Calculates the transaction fee for a CEX swap.
 *
 * [REDACTED_TASK_KEY] — combines the two existing CEX fee paths in `SwapInteractorImpl` into one place:
 *  - `loadFeeForSwapTransaction` overload 2 (CEX branch, native fee via [EstimateFeeUseCase])
 *  - `loadFeeForSwapTransaction` overload 1 (token/gasless fee via [EstimateFeeForTokenUseCase] or
 *    [EstimateFeeForGaslessTxUseCase])
 *
 * Strategy is selected by [selectedFeeToken]:
 *  - `null` on a Tron token supported by Tron gasless → [GetTronGaslessFeeUseCase], falling back to the
 *    `null` path below when the quote is unavailable. The swap screen reloads the fee without a selected
 *    token after every quote reload, so this is what keeps a wallet without TRX on the token fee.
 *  - `null` → gasless. Calls [EstimateFeeForGaslessTxUseCase] which itself decides whether to use
 *    a native or token fee. When it resolves to a **native** Ethereum fee, the 5% gas-limit bump is
 *    applied for parity with the explicit native path; token-paid fees carry their own safety
 *    margins and are **not bumped**.
 *  - non-null + token currency on Tron → calls [GetTronGaslessFeeUseCase]: the fee is the Tron gasless
 *    compensation charged in the sent token. **No gas-limit bump.**
 *  - non-null + token currency elsewhere → calls [EstimateFeeForTokenUseCase]. **No gas-limit bump.**
 *  - non-null + native (coin) currency → calls [EstimateFeeUseCase]. **The 5% gas-limit bump is
 *    applied via [patchEthGasLimitForSwap]** for parity with `loadFeeForSwapTransaction` overload 2.
 *    The bump is a no-op for non-Ethereum fees, so this is safe across chains.
 *
 * This is the single CEX fee path: `SwapInteractorImpl.loadSwapFee` delegates here for CEX swaps.
 */
@Suppress("LongParameterList")
class CexSwapFeeCalculator(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase,
    private val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase,
    private val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase,
    private val swapFeatureToggles: SwapFeatureToggles,
    private val patchEthGasLimitForSwap: PatchEthGasLimitForSwap,
) {

    private val estimationFeeAddressFactory by lazy { EstimationFeeAddressFactory() }

    suspend fun calculate(
        userWallet: UserWallet,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: BigDecimal,
        selectedFeeToken: CryptoCurrencyStatus?,
        isGasless: Boolean,
    ): Either<GetFeeError, CexFeeResult> = either {
        if (amount.signum() == 0) {
            raise(GetFeeError.UnknownError)
        }

        val transactionFeeResult: TransactionFeeResult = if (isGasless) {
            when {
                selectedFeeToken == null -> {
                    val tronGaslessFee = estimateDefaultTronGaslessFeeOrNull(
                        userWallet = userWallet,
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                    ).bind()
                    // Gasless path — overload 1 in SwapInteractorImpl. Bumped only when it
                    // resolves to a native Ethereum fee (see patchIfNativeEthereumFee).
                    val feeExtended = tronGaslessFee ?: estimateFeeForGaslessTxUseCase(
                        amount = amount,
                        userWallet = userWallet,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
                    ).bind().patchIfNativeEthereumFee()
                    TransactionFeeResult.LoadedExtended(feeExtended)
                }
                swapFeatureToggles.isTronGaslessSwapEnabled &&
                    selectedFeeToken.currency is CryptoCurrency.Token &&
                    isTron(fromSwapCurrencyStatus.currency.network.rawId) -> {
                    val feeExtended = estimateTronGaslessFee(
                        userWallet = userWallet,
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        amount = amount,
                        feeToken = selectedFeeToken.currency as CryptoCurrency.Token,
                    ).bind()
                    TransactionFeeResult.LoadedExtended(feeExtended)
                }
                selectedFeeToken.currency is CryptoCurrency.Token -> {
                    // Explicit gasless-token path — overload 1 in SwapInteractorImpl. No gas-limit bump.
                    val feeExtended = estimateFeeForTokenUseCase(
                        userWallet = userWallet,
                        feeTokenCurrencyStatus = selectedFeeToken,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
                        amount = amount,
                    ).bind()
                    TransactionFeeResult.LoadedExtended(feeExtended)
                }
                else -> {
                    // Explicit native fee path — overload 2 in SwapInteractorImpl. Apply 5% bump.
                    val fee = estimateFeeUseCase(
                        amount = amount,
                        userWallet = userWallet,
                        cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
                    ).bind()
                    TransactionFeeResult.Loaded(patchEthGasLimitForSwap(fee))
                }
            }
        } else {
            // Explicit native fee path — overload 2 in SwapInteractorImpl. Apply 5% bump.
            val fee = estimateFeeUseCase(
                amount = amount,
                userWallet = userWallet,
                cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            ).bind()
            TransactionFeeResult.Loaded(patchEthGasLimitForSwap(fee))
        }

        CexFeeResult(transactionFee = transactionFeeResult)
    }

    private suspend fun estimateDefaultTronGaslessFeeOrNull(
        userWallet: UserWallet,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: BigDecimal,
    ): Either<GetFeeError, TransactionFeeExtended?> = either {
        if (!swapFeatureToggles.isTronGaslessSwapEnabled) return@either null
        val token = fromSwapCurrencyStatus.currency as? CryptoCurrency.Token ?: return@either null
        if (!isTronGaslessSupportedUseCase(userWallet.walletId, token.network, token)) return@either null

        estimateTronGaslessFee(
            userWallet = userWallet,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            amount = amount,
            feeToken = token,
        ).getOrElse { error ->
            // An account that must be activated first cannot pay a native fee either.
            if (error.isTronActivationRefusal()) raise(error)
            TangemLogger.i("Tron gasless quote unavailable ($error), falling back to the native fee")
            null
        }
    }

    private suspend fun estimateTronGaslessFee(
        userWallet: UserWallet,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        amount: BigDecimal,
        feeToken: CryptoCurrency.Token,
    ): Either<GetFeeError, TransactionFeeExtended> = either {
        val network = fromSwapCurrencyStatus.currency.network
        val estimationDestination = estimationFeeAddressFactory.makeAddress(network.toBlockchain())
        val transactionData = createTransferTransactionUseCase(
            amount = amount.convertToSdkAmount(fromSwapCurrencyStatus.status),
            memo = null,
            destination = estimationDestination,
            userWalletId = userWallet.walletId,
            network = network,
        ).mapLeft(GetFeeError::DataError).bind()

        getTronGaslessFeeUseCase(
            userWalletId = userWallet.walletId,
            transactionData = transactionData,
            feeToken = feeToken,
        ).bind()
    }

    /**
     * Applies the gas-limit bump when the gasless use case resolved to a native-paid Ethereum fee
     * ([Fee.Ethereum.EIP1559] / [Fee.Ethereum.Legacy]) — such a fee is signed and sent as a regular
     * transaction, exactly like the explicit native path, so it needs the same headroom. Token-paid
     * ([Fee.Ethereum.TokenCurrency]) and non-Ethereum fees are returned unchanged: the former carries
     * its own safety margins, the latter would be a no-op for the patch anyway.
     */
    private fun TransactionFeeExtended.patchIfNativeEthereumFee(): TransactionFeeExtended {
        return when (transactionFee.normal) {
            is Fee.Ethereum.EIP1559,
            is Fee.Ethereum.Legacy,
            -> {
                val patched = patchEthGasLimitForSwap(transactionFee)
                copy(transactionFee = patched, nativeFee = patched)
            }
            else -> this
        }
    }
}