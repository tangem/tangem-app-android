package com.tangem.feature.swap.domain.fee

import android.util.Base64
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySwapCallData
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransactionDataExtrasUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.usecase.WrapYieldSwapCallDataWithUpgradeUseCase
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import java.math.BigInteger

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
    private val wrapYieldSwapCallDataWithUpgradeUseCase: WrapYieldSwapCallDataWithUpgradeUseCase,
) {

    suspend fun calculate(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        selectedToken: CryptoCurrencyStatus? = null,
        permissionState: PermissionDataState = PermissionDataState.Empty,
    ): Either<GetFeeError, DexFeeResult> = either {
        val networkRawId = fromSwapCurrencyStatus.currency.network.rawId
        val nativeCoinDecimals = Blockchain.fromNetworkId(networkRawId)?.decimals()
            ?: error("Blockchain not found")
        val otherNativeFee = transaction.otherNativeFeeWei
            ?.movePointLeft(nativeCoinDecimals)
            ?: BigDecimal.ZERO

        if (isSolana(networkRawId)) {
            val transactionBytes = Base64.decode(transaction.txData, Base64.NO_WRAP)
            val formattedHash = getFormattedHash(transactionBytes)

            // TODO Update after new firmware [REDACTED_JIRA]
            if (formattedHash.size > SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES &&
                fromSwapCurrencyStatus.userWallet is UserWallet.Cold
            ) {
                raise(GetFeeError.BlockchainErrors.TooLargeSolanaTransactionError)
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
                permissionState = permissionState,
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

    /**
     * Yield-mode DEX fee path: routes the swap through the user's yield module proxy.
     *
     * Native fee is computed for a [TransactionData.Uncompiled] addressed to [yieldModuleAddress],
     * carrying the wrapped call data produced by [buildYieldSwapCallData]. The 12% gas-limit bump
     * is applied to match the non-yield DEX flow.
     *
     * Fallback to [GetEthSpecificFeeUseCase] (with the gas limit carried by the Express transaction
     * model) is applied in two cases:
     *  - [yieldModuleAddress] is `null` — yield module address could not be resolved upstream;
     *  - the fee estimation call throws `IllegalStateException` (e.g. payload too large).
     *
     * Yield-module errors ([YieldModuleUpgradeUnavailableException],
     * [YieldModuleVersionIndeterminateException]) are mapped to [ExpressDataError.UnknownError]
     * to keep the unified error surface a single type.
     */
    suspend fun calculateYield(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        yieldModuleAddress: String?,
    ): Either<GetFeeError, DexFeeResult> = either {
        val fromCurrency = fromSwapCurrencyStatus.currency as? CryptoCurrency.Token
            ?: raise(GetFeeError.UnknownError)
        val network = fromCurrency.network

        val nativeBalance = walletManagersFacade.getNativeTokenBalance(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            networkId = network.rawId,
            derivationPath = network.derivationPath.value,
        )
        if (nativeBalance.signum() == 0) raise(GetFeeError.UnknownError)

        if (yieldModuleAddress == null) {
            val gasLimit = transaction.gas ?: raise(GetFeeError.UnknownError)
            return@either ethSpecificFeeFallback(fromSwapCurrencyStatus, gasLimit).bind()
        }

        val spenderAddress = transaction.allowanceContract ?: raise(GetFeeError.UnknownError)

        val rawFee = try {
            val wrappedCallData = buildYieldSwapCallData(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                txTo = transaction.txTo,
                dexCallData = transaction.txData,
                amount = transaction.fromAmount.value,
                spenderAddress = spenderAddress,
            )
            val extras = createTransactionExtrasUseCase(
                callData = wrappedCallData,
                network = network,
            ).getOrNull() ?: raise(GetFeeError.UnknownError)

            val transactionData = TransactionData.Uncompiled(
                amount = createNativeAmountForDex("0", network),
                destinationAddress = yieldModuleAddress,
                fee = null,
                sourceAddress = transaction.txFrom,
                extras = extras,
            )
            getFeeUseCase(
                transactionData = transactionData,
                network = network,
                userWallet = fromSwapCurrencyStatus.userWallet,
            ).getOrNull() ?: raise(GetFeeError.UnknownError)
        } catch (_: YieldModuleUpgradeUnavailableException) {
            raise(GetFeeError.UnknownError)
        } catch (_: YieldModuleVersionIndeterminateException) {
            raise(GetFeeError.UnknownError)
        } catch (_: IllegalStateException) {
            val gasLimit = transaction.gas ?: raise(GetFeeError.UnknownError)
            return@either ethSpecificFeeFallback(fromSwapCurrencyStatus, gasLimit).bind()
        }

        val patched = patchEthGasLimitForSwap(rawFee)
        DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(patched),
            otherNativeFee = BigDecimal.ZERO,
            gas = transaction.gas,
        )
    }

    /**
     * Wraps a DEX call data into a yield-supply swap call data, ready to be sent through the
     * user's yield module. Shared with [SwapInteractorImpl.createYieldSwapDexTransaction], which
     * is why this helper is exposed at the calculator level rather than kept private.
     */
    suspend fun buildYieldSwapCallData(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        txTo: String,
        dexCallData: String,
        amount: BigDecimal,
        spenderAddress: String,
    ): SmartContractCallData {
        val fromCurrency = fromSwapCurrencyStatus.currency as CryptoCurrency.Token
        val amountInWei = amount.movePointRight(fromCurrency.decimals).toBigInteger()
        val dexCallDataBytes = dexCallData.removePrefix("0x").hexToBytes()
        val swapCallData = EthereumYieldSupplySwapCallData(
            tokenIn = fromCurrency.contractAddress,
            amountIn = amountInWei,
            target = txTo,
            spender = spenderAddress,
            swapData = dexCallDataBytes,
        )
        return wrapYieldSwapCallDataWithUpgradeUseCase(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = fromCurrency.network,
            callData = swapCallData,
        )
    }

    private suspend fun ethSpecificFeeFallback(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        gasLimit: BigInteger,
    ): Either<GetFeeError, DexFeeResult> = either {
        val fee = getEthSpecificFeeUseCase(
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoCurrency = fromSwapCurrencyStatus.currency,
            gasLimit = gasLimit,
        ).getOrNull() ?: raise(GetFeeError.UnknownError)
        val patched = patchEthGasLimitForSwap(fee)
        DexFeeResult(
            transactionFee = TransactionFeeResult.Loaded(patched),
            otherNativeFee = BigDecimal.ZERO,
            gas = gasLimit,
        )
    }

    @Suppress("LongMethod")
    private suspend fun getFeeDataForDexSwap(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        selectedToken: CryptoCurrencyStatus?,
        permissionState: PermissionDataState,
    ): Either<GetFeeError, TransactionFeeResult> = either {
        catch(
            block = {
                val nativeBalance = walletManagersFacade.getNativeTokenBalance(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    networkId = fromSwapCurrencyStatus.currency.network.rawId,
                    derivationPath = fromSwapCurrencyStatus.currency.network.derivationPath.value,
                )

                // if native balance is zero - we can't calculate fee
                if (nativeBalance.signum() == 0) {
                    raise(GetFeeError.UnknownError)
                }

                val txAmountValue = transaction.txValue ?: error("unable to get txValue")
                val amountToSend = if (permissionState is PermissionDataState.PermissionSettings) {
                    transaction.fromAmount.value.convertToSdkAmount(fromSwapCurrencyStatus.status)
                } else {
                    createNativeAmountForDex(txAmountValue, fromSwapCurrencyStatus.currency.network)
                }

                // transaction.txValue is always native coin
                if (fromSwapCurrencyStatus.currency is CryptoCurrency.Coin && nativeBalance < amountToSend.value) {
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
                    ).fold(
                        // The token branch normally yields LoadedExtended, but when the use case fails
                        // we mirror the exception path and fall back to the eth-specific Loaded fee.
                        ifLeft = { left -> ethSpecificFeeFallbackOrRaise(fromSwapCurrencyStatus, transaction, left) },
                        ifRight = { feeExtended -> TransactionFeeResult.LoadedExtended(feeExtended) },
                    )
                } else {
                    val isSimulateEstimation = permissionState is PermissionDataState.PermissionSettings
                    getFeeUseCase(
                        transactionData = transactionData,
                        network = fromSwapCurrencyStatus.currency.network,
                        userWallet = fromSwapCurrencyStatus.userWallet,
                        spenderAddress = (permissionState as? PermissionDataState.PermissionSettings)?.spenderAddress,
                        isSimulateEstimation = isSimulateEstimation,
                    ).fold(
                        ifLeft = { left -> ethSpecificFeeFallbackOrRaise(fromSwapCurrencyStatus, transaction, left) },
                        ifRight = { fee -> TransactionFeeResult.Loaded(fee) },
                    )
                }
            },
            catch = { error ->
                ethSpecificFeeFallbackOrRaise(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    transaction = transaction,
                    // gas may be null — surface DataError so the provider becomes a SwapError.
                    gasNullError = GetFeeError.DataError(error),
                )
            },
        )
    }

    /**
     * Eth-specific fee fallback shared by the inner `catch`'s exception branch and the
     * `Either.Left` branches of [getFeeForTokenUseCase]/[getFeeUseCase] in [getFeeDataForDexSwap].
     *
     * When [ExpressTransactionModel.DEX.gas] is `null` there is no gas limit to feed
     * [GetEthSpecificFeeUseCase], so [gasNullError] is raised instead. For the exception path
     * [gasNullError] wraps the thrown [Throwable] as [GetFeeError.DataError]; for the
     * `Either.Left` path it is the original left [GetFeeError].
     *
     * Always returns a [TransactionFeeResult.Loaded] (never `LoadedExtended`), matching the
     * legacy exception-catch behaviour.
     */
    private suspend fun Raise<GetFeeError>.ethSpecificFeeFallbackOrRaise(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        transaction: ExpressTransactionModel.DEX,
        gasNullError: GetFeeError,
    ): TransactionFeeResult {
        if (gasNullError is GetFeeError.EstimateOverrideError) {
            raise(gasNullError)
        }

        val gasLimit = transaction.gas ?: raise(gasNullError)
        val fee = getEthSpecificFeeUseCase(
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoCurrency = fromSwapCurrencyStatus.currency,
            gasLimit = gasLimit,
        ).bind()
        return TransactionFeeResult.Loaded(fee)
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