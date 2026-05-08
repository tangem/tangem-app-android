package com.tangem.feature.swap.domain.fee

import android.util.Base64
import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.CreateTransactionDataExtrasUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

/**
 * Calculates the on-chain transaction fee for a DEX swap.
 *
 * [REDACTED_TASK_KEY] — extracted verbatim from `SwapInteractorImpl.loadFeeForDex`,
 * `getFeeDataForDexSwap` and `getFeeDataForSolanaDexSwap` so the DEX-fee strategy is testable in
 * isolation. The original methods are intentionally retained in `SwapInteractorImpl` until the
 * caller is migrated to delegate to this calculator (the migration is deferred — see plan).
 *
 * Strategy selection mirrors the source: Solana uses [TransactionData.Compiled] from the
 * Express-supplied `txData` and skips the gas patch; everything else uses
 * [TransactionData.Uncompiled] and applies the 12% gas-limit bump via [patchEthGasLimitForSwap].
 *
 * If [GetFeeUseCase] throws `IllegalStateException` (e.g. payload too large to estimate), the
 * calculator falls back to [GetEthSpecificFeeUseCase] using the gas value carried by the Express
 * transaction model — same as the production path.
 *
 * @see DexFeeResult for the returned shape.
 */
@Suppress("LongParameterList")
class DexSwapFeeCalculator(
    private val getFeeUseCase: GetFeeUseCase,
    private val getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val patchEthGasLimitForSwap: PatchEthGasLimitForSwap,
) {

    suspend fun calculate(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        selectedToken: CryptoCurrencyStatus? = null,
    ): Either<ExpressDataError, DexFeeResult> = either {
        val networkRawId = fromSwapCurrencyStatus.currency.network.rawId
        val nativeCoinDecimals = Blockchain.fromNetworkId(networkRawId)?.decimals()
            ?: error("Blockchain not found")
        val otherNativeFee = transaction.otherNativeFeeWei
            ?.movePointLeft(nativeCoinDecimals)
            ?: BigDecimal.ZERO

        if (isSolana(networkRawId)) {
            val transactionBytes = Base64.decode(transaction.txData, Base64.NO_WRAP)
            val formattedHash = getFormattedHash(transactionBytes)

            if (formattedHash.size > SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES &&
                fromSwapCurrencyStatus.userWallet is UserWallet.Cold
            ) {
                raise(ExpressDataError.TooLargeSolanaTransactionError)
            }

            val solanaFee = getFeeDataForSolanaDexSwap(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                transactionBytes = transactionBytes,
            )
            DexFeeResult(
                transactionFee = TransactionFeeResult.Loaded(solanaFee),
                otherNativeFee = otherNativeFee,
                gas = null,
            )
        } else {
            val rawFeeResult = getFeeDataForDexSwap(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                transaction = transaction,
                selectedToken = selectedToken,
            ).bind()
            // Apply the 12% bump on EVM, mirroring SwapInteractorImpl.loadFeeForDex.
            // The original cast `(fee as TransactionFeeResult.Loaded)` only holds when
            // selectedToken == null; we defensively support LoadedExtended too so the calculator
            // also handles the gasless-token DEX branch (currently unreachable from production
            // callers, kept for symmetry with the CEX calculator).
            val patched: TransactionFeeResult = when (rawFeeResult) {
                is TransactionFeeResult.Loaded ->
                    TransactionFeeResult.Loaded(patchEthGasLimitForSwap(rawFeeResult.fee))
                is TransactionFeeResult.LoadedExtended ->
                    TransactionFeeResult.LoadedExtended(
                        rawFeeResult.fee.copy(
                            transactionFee = patchEthGasLimitForSwap(rawFeeResult.fee.transactionFee),
                        ),
                    )
            }
            DexFeeResult(
                transactionFee = patched,
                otherNativeFee = otherNativeFee,
                gas = transaction.gas,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun getFeeDataForDexSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        selectedToken: CryptoCurrencyStatus?,
    ): Either<ExpressDataError, TransactionFeeResult> = either {
        val nativeBalance = walletManagersFacade.getNativeTokenBalance(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            networkId = fromSwapCurrencyStatus.currency.network.rawId,
            derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
        )

        // if native balance is zero - we can't calculate fee
        if (nativeBalance.signum() == 0) {
            raise(ExpressDataError.UnknownError)
        }

        try {
            val txAmountValue = transaction.txValue ?: error("unable to get txValue")
            val amountToSend = createNativeAmountForDex(txAmountValue, fromSwapCurrencyStatus.currency.network)

            // transaction.txValue is always native coin
            if (nativeBalance < amountToSend.value) {
                error("It's impossible to calculate fee for nativeBalance.value < amountToSend.value")
            }

            val extras = createTransactionExtrasUseCase(
                data = transaction.txData,
                network = fromSwapCurrencyStatus.currency.network,
            ).getOrNull() ?: error("unable to create extras")

            val transactionData = TransactionData.Uncompiled(
                amount = amountToSend,
                destinationAddress = transaction.txTo,
                fee = null,
                sourceAddress = transaction.txFrom,
                extras = extras,
            )
            if (selectedToken != null && selectedToken.currency is CryptoCurrency.Token) {
                getFeeForTokenUseCase(
                    transactionData = transactionData,
                    token = selectedToken.currency,
                    userWallet = fromSwapCurrencyStatus.userWallet,
                ).getOrNull()?.let { TransactionFeeResult.LoadedExtended(it) }
                    ?: error("unable to calculate fee for token")
            } else {
                getFeeUseCase(
                    transactionData = transactionData,
                    network = fromSwapCurrencyStatus.currency.network,
                    userWallet = fromSwapCurrencyStatus.userWallet,
                ).getOrNull()?.let { TransactionFeeResult.Loaded(it) } ?: error("unable to calculate fee")
            }
        } catch (_: IllegalStateException) {
            getEthSpecificFeeUseCase(
                userWallet = fromSwapCurrencyStatus.userWallet,
                cryptoCurrency = fromSwapCurrencyStatus.currency,
                gasLimit = transaction.gas,
            ).getOrNull()?.let { TransactionFeeResult.Loaded(it) }
                ?: error("can't get fee for getEthSpecificFeeUseCase")
        }
    }

    private suspend fun getFeeDataForSolanaDexSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transactionBytes: ByteArray,
    ): TransactionFee {
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.Bytes(transactionBytes),
        )

        return getFeeUseCase(
            transactionData = transactionData,
            network = fromSwapCurrencyStatus.currency.network,
            userWallet = fromSwapCurrencyStatus.userWallet,
        ).getOrNull() ?: error("unable to calculate fee")
    }

    private fun createNativeAmountForDex(txValueAmount: String, network: Network): Amount {
        val nativeDecimals = Blockchain.fromNetworkId(network.rawId)?.decimals()
            ?: error("Blockchain not found")
        val decimalValue = txValueAmount.toBigDecimalOrNull()?.movePointLeft(nativeDecimals)
            ?: error("txValue parse error")
        return Amount(
            currencySymbol = network.currencySymbol,
            value = decimalValue,
            decimals = nativeDecimals,
        )
    }

    // TODO create usecase [REDACTED_TASK_KEY] (parity with SwapInteractorImpl.getFormattedHash)
    private fun getFormattedHash(hash: ByteArray): ByteArray {
        return try {
            SolanaTransactionHelper.removeSignaturesPlaceholders(hash)
        } catch (e: Exception) {
            TangemLogger.e("Failed to format the hash: ${e.message.orEmpty()}", e)
            hash
        }
    }
}