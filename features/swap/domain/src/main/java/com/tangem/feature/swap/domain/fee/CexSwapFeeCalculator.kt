package com.tangem.feature.swap.domain.fee

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
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
 *    a native or token fee. **No gas-limit bump is applied** here, matching production behavior of
 *    overload 1.
 *  - non-null + token currency → calls [EstimateFeeForTokenUseCase]. **No gas-limit bump.**
 *  - non-null + native (coin) currency → calls [EstimateFeeUseCase]. **The 5% gas-limit bump is
 *    applied via [patchEthGasLimitForSwap]** for parity with `loadFeeForSwapTransaction` overload 2.
 *    The bump is a no-op for non-Ethereum fees, so this is safe across chains.
 *
 * Behavior is byte-for-byte identical to the original methods in `SwapInteractorImpl`. The
 * original code is intentionally retained alongside this calculator until the caller is migrated
 * to delegate to it (the migration is deferred — see plan).
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
                    // Gasless path — overload 1 in SwapInteractorImpl. No gas-limit bump.
                    val feeExtended = estimateFeeForGaslessTxUseCase(
                        amount = amount,
                        userWallet = userWallet,
                        sendingTokenCurrencyStatus = fromSwapCurrencyStatus.status,
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
}