package com.tangem.feature.swap.domain.fee

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
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
 *  - `null` → gasless. Calls [EstimateFeeForGaslessTxUseCase] which itself decides whether to use
 *    a native or token fee. When it resolves to a **native** Ethereum fee, the 5% gas-limit bump is
 *    applied for parity with the explicit native path; token-paid fees carry their own safety
 *    margins and are **not bumped**.
 *  - non-null + token currency → calls [EstimateFeeForTokenUseCase]. **No gas-limit bump.**
 *  - non-null + native (coin) currency → calls [EstimateFeeUseCase]. **The 5% gas-limit bump is
 *    applied via [patchEthGasLimitForSwap]** for parity with `loadFeeForSwapTransaction` overload 2.
 *    The bump is a no-op for non-Ethereum fees, so this is safe across chains.
 *
 * This is the single CEX fee path: `SwapInteractorImpl.loadSwapFee` delegates here for CEX swaps.
 */
class CexSwapFeeCalculator(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase,
    private val estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase,
    private val patchEthGasLimitForSwap: PatchEthGasLimitForSwap,
) {

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
                    // Gasless path — overload 1 in SwapInteractorImpl. Bumped only when it
                    // resolves to a native Ethereum fee (see patchIfNativeEthereumFee).
                    val feeExtended = estimateFeeForGaslessTxUseCase(
                        amount = amount,
                        userWallet = userWallet,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
                    ).bind()
                    TransactionFeeResult.LoadedExtended(feeExtended.patchIfNativeEthereumFee())
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