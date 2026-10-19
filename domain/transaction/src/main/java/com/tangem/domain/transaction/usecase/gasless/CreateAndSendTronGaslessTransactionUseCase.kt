package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.mapToGaslessSendError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessApprove
import com.tangem.domain.transaction.models.tron.TronGaslessApproveEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessSwap
import com.tangem.domain.transaction.models.tron.TronGaslessSwapEstimateParams
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger

/**
 * Builds the compensation token transfer, signs it together with the original transaction (a token
 * transfer or an exact token approval) in ONE signing session (single NFC tap, compensation first),
 * submits the signed pair to the gasless backend, and returns the ORIGINAL transaction hash.
 *
 * The flow is non-atomic (compensation broadcasts before the original); success is reported only when
 * the original transfer broadcasts. Tron does not use the EVM EIP-712/7702 gasless machinery.
 */
class CreateAndSendTronGaslessTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val gaslessSignerProvider: GaslessSignerProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = either {
        catch(
            block = {
                val quotedFee = fee.requireTronQuote()
                val network = resolveFeeTokenNetwork(userWallet, fee)
                val original = transactionData.requireUncompiled()
                if (original.amount.type !is AmountType.Token) error("Tron gasless requires a token transfer")

                val feeToken = fee.requireFeeToken()
                val approve = original.tronGaslessApproveOrNull()
                val quote = if (approve != null) {
                    quoteForFinalApprove(
                        userWallet = userWallet,
                        quotedFee = quotedFee,
                        approve = approve,
                        feeToken = feeToken,
                    )
                } else {
                    quoteForFinalAmount(
                        userWallet = userWallet,
                        quotedFee = quotedFee,
                        original = original,
                        feeToken = feeToken,
                    )
                }
                val compensationTx = createCompensationTransaction(
                    userWallet = userWallet,
                    network = network,
                    quote = quote,
                    feeToken = feeToken,
                )

                // One signing session (single NFC tap): compensation FIRST, then the original transfer.
                val signed = sign(userWallet, network, listOf(compensationTx, original))

                val result = tronGaslessTransactionRepository.submit(
                    userWalletId = userWallet.walletId,
                    quoteId = quote.quoteId,
                    signedCompensationTx = signed[0],
                    signedOriginalTx = signed[1],
                )
                result.requireOriginalTxHash()
            },
            catch = { raise(it.mapToGaslessSendError()) },
        )
    }

    suspend fun sendSwap(
        userWallet: UserWallet,
        swap: TronGaslessSwap,
        swapTransaction: TransactionData.Uncompiled,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = either {
        catch(
            block = {
                val quotedFee = fee.requireTronQuote()
                val network = resolveFeeTokenNetwork(userWallet, fee)
                val feeToken = fee.requireFeeToken()
                swapTransaction.requireMatches(swap)
                val quote = quoteForFinalSwap(
                    userWallet = userWallet,
                    quotedFee = quotedFee,
                    swap = swap,
                    feeToken = feeToken,
                )
                val compensationTx = createCompensationTransaction(
                    userWallet = userWallet,
                    network = network,
                    quote = quote,
                    feeToken = feeToken,
                )

                val signed = sign(userWallet, network, listOf(compensationTx, swapTransaction))

                val result = tronGaslessTransactionRepository.submit(
                    userWalletId = userWallet.walletId,
                    quoteId = quote.quoteId,
                    signedCompensationTx = signed[0],
                    signedOriginalTx = signed[1],
                )
                result.requireOriginalTxHash()
            },
            catch = { raise(it.mapToGaslessSendError()) },
        )
    }

    private fun TransactionFeeExtended.requireTronQuote(): TronGaslessQuote =
        (gaslessProtocolData as? GaslessProtocolData.Tron)?.quote ?: error("Tron gasless quote is missing")

    private fun TransactionFeeExtended.requireFeeToken(): Token =
        (transactionFee.normal.amount.type as? AmountType.Token)?.token
            ?: error("Tron gasless fee must be denominated in a token")

    // Resolved as in the EVM flow, so both send paths agree on the account the fee token comes from.
    private suspend fun resolveFeeTokenNetwork(userWallet: UserWallet, fee: TransactionFeeExtended): Network =
        singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
            .getCryptoCurrency(currencyId = fee.feeTokenId, network = null)
            .getOrElse { error("Token for fee not found") }
            .network

    private suspend fun createCompensationTransaction(
        userWallet: UserWallet,
        network: Network,
        quote: TronGaslessQuote,
        feeToken: Token,
    ): TransactionData.Uncompiled {
        // Fail loudly on a backend token mismatch so it cannot silently transfer
        // the wrong asset to the fee recipient (base58 addresses, case-sensitive).
        require(feeToken.contractAddress == quote.compensationToken) {
            "Tron gasless compensation token (${quote.compensationToken}) differs from the fee " +
                "token (${feeToken.contractAddress})"
        }

        // Built from the raw base-unit amount to avoid decimal drift vs the quote.
        val compensationValue = quote.compensationAmountRaw.toBigDecimal().movePointLeft(feeToken.decimals)
        return transactionRepository.createTransferTransaction(
            amount = Amount(token = feeToken, value = compensationValue),
            fee = null,
            memo = null,
            destination = quote.feeRecipient,
            userWalletId = userWallet.walletId,
            network = network,
        )
    }

    private suspend fun sign(
        userWallet: UserWallet,
        network: Network,
        legs: List<TransactionData.Uncompiled>,
    ): List<String> {
        val signed = walletManagersFacade.signTronGaslessTransactions(
            userWalletId = userWallet.walletId,
            network = network,
            transactionDataList = legs,
            signer = gaslessSignerProvider(userWallet),
        ) ?: error("Tron gasless signing failed")
        require(signed.size == legs.size) { "Expected ${legs.size} signed transactions, got ${signed.size}" }
        return signed
    }

    private fun TronGaslessSubmitResult.requireOriginalTxHash(): String {
        if (originalTxHash.isBlank() || status.contains("FAIL", ignoreCase = true)) {
            error("Tron gasless original transaction failed: status=$status")
        }
        return originalTxHash
    }

    private suspend fun Raise<SendTransactionError>.quoteForFinalAmount(
        userWallet: UserWallet,
        quotedFee: TronGaslessQuote,
        original: TransactionData.Uncompiled,
        feeToken: Token,
    ): TronGaslessQuote {
        val amountRaw = original.amount.toTronGaslessBaseUnits()
            ?: error("Tron gasless: transaction amount is null")
        val isQuoteForThisTransfer = amountRaw == quotedFee.quotedAmountRaw &&
            original.destinationAddress == quotedFee.quotedToAddress
        if (isQuoteForThisTransfer && quotedFee.isUsableForSubmit()) return quotedFee

        val refreshed = tronGaslessTransactionRepository.estimate(
            TronGaslessEstimateParams(
                userWalletId = userWallet.walletId,
                fromAddress = original.sourceAddress,
                toAddress = original.destinationAddress,
                tokenContract = original.contractAddress ?: feeToken.contractAddress,
                amount = amountRaw,
                feeTokenContract = feeToken.contractAddress,
            ),
        )
        ensureCompensationNotGrown(quotedFee, refreshed) { "$amountRaw to ${original.destinationAddress}" }
        return refreshed
    }

    private suspend fun Raise<SendTransactionError>.quoteForFinalApprove(
        userWallet: UserWallet,
        quotedFee: TronGaslessQuote,
        approve: TronGaslessApprove,
        feeToken: Token,
    ): TronGaslessQuote {
        if (quotedFee.quotedApprove == approve && quotedFee.isUsableForSubmit()) return quotedFee

        val refreshed = tronGaslessTransactionRepository.estimateApprove(
            TronGaslessApproveEstimateParams(
                userWalletId = userWallet.walletId,
                approve = approve,
                feeTokenContract = feeToken.contractAddress,
            ),
        )
        ensureCompensationNotGrown(quotedFee, refreshed) { "the approval of ${approve.spenderAddress}" }
        return refreshed
    }

    private suspend fun Raise<SendTransactionError>.quoteForFinalSwap(
        userWallet: UserWallet,
        quotedFee: TronGaslessQuote,
        swap: TronGaslessSwap,
        feeToken: Token,
    ): TronGaslessQuote {
        if (quotedFee.quotedSwap == swap && quotedFee.isUsableForSubmit()) return quotedFee

        val refreshed = tronGaslessTransactionRepository.estimateSwap(
            TronGaslessSwapEstimateParams(
                userWalletId = userWallet.walletId,
                swap = swap,
                feeTokenContract = feeToken.contractAddress,
            ),
        )
        ensureCompensationNotGrown(quotedFee, refreshed) { "the swap through ${swap.routerAddress}" }
        return refreshed
    }

    /**
     * The backend drops a quote at [TronGaslessQuote.expiresAtEpochMs] and rejects its submit, so a quote
     * that would lapse during the card tap and the submit is re-quoted instead of signed.
     */
    private fun TronGaslessQuote.isUsableForSubmit(): Boolean =
        expiresAtEpochMs - currentTimeMillis() > QUOTE_SUBMIT_MARGIN_MS

    private fun Raise<SendTransactionError>.ensureCompensationNotGrown(
        quotedFee: TronGaslessQuote,
        refreshed: TronGaslessQuote,
        requoted: () -> String,
    ) {
        if (refreshed.compensationAmountRaw > quotedFee.compensationAmountRaw) {
            TangemLogger.e(
                "Tron gasless compensation grew from ${quotedFee.compensationAmountRaw} to " +
                    "${refreshed.compensationAmountRaw} while re-quoting ${requoted()}",
            )
            raise(SendTransactionError.GaslessFeeIncreased)
        }
    }

    /** Signing a swap other than the quoted one would submit it under a quote priced for different calldata. */
    private fun TransactionData.Uncompiled.requireMatches(swap: TronGaslessSwap) {
        val callDataHex = (extras as? TronTransactionExtras)?.callData?.dataHex?.removePrefix("0x")
        require(
            sourceAddress == swap.fromAddress &&
                destinationAddress == swap.routerAddress &&
                callDataHex.equals(swap.callDataHex, ignoreCase = true),
        ) { "Tron gasless swap transaction differs from the quoted swap through ${swap.routerAddress}" }
    }

    private companion object {
        const val QUOTE_SUBMIT_MARGIN_MS = 30_000L
    }
}