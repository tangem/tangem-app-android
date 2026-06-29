package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Builds the compensation USDT transfer, signs it together with the original transfer in ONE signing
 * session (single NFC tap, compensation first), submits the signed pair to the gasless backend, and
 * returns the ORIGINAL transaction hash.
 *
 * The flow is non-atomic (compensation broadcasts before the original); success is reported only when
 * the original transfer broadcasts. Tron does not use the EVM EIP-712/7702 gasless machinery.
 */
class CreateAndSendTronGaslessTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotWalletSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> = either {
        catch(
            block = {
                val quote = fee.tronGaslessQuote ?: error("Tron gasless quote is missing")
                val original = transactionData.requireUncompiled()
                val sentToken = (original.amount.type as? AmountType.Token)?.token
                    ?: error("Tron gasless requires a token transfer")

                // The compensation must be paid in the quoted compensation token. Today only the
                // single-token case (sent == compensation, e.g. USDT->USDT) is supported, so the
                // compensation transfer reuses the sent token. Fail loudly if the backend ever quotes a
                // different compensation token (Tron contract addresses are base58, case-sensitive) so a
                // future multi-token path cannot silently transfer the wrong asset to the fee recipient.
                require(sentToken.contractAddress == quote.compensationToken) {
                    "Tron gasless compensation token (${quote.compensationToken}) differs from the sent " +
                        "token (${sentToken.contractAddress}); multi-token compensation is not supported yet"
                }

                // Compensation = transfer of the quoted token amount to the Tangem fee recipient.
                val compensationTx = transactionRepository.createTransferTransaction(
                    amount = Amount(token = sentToken, value = quote.compensationAmountDecimal),
                    fee = null,
                    memo = null,
                    destination = quote.feeRecipient,
                    userWalletId = userWallet.walletId,
                    network = network,
                )

                // One signing session (single NFC tap): compensation FIRST, then the original transfer.
                val signed = walletManagersFacade.signTronGaslessTransactions(
                    userWalletId = userWallet.walletId,
                    network = network,
                    transactionDataList = listOf(compensationTx, original),
                    signer = getSigner(userWallet),
                ) ?: error("Tron gasless signing failed")
                require(signed.size == 2) { "Expected 2 signed transactions, got ${signed.size}" }

                val result = tronGaslessTransactionRepository.submit(
                    quoteId = quote.quoteId,
                    signedCompensationTx = signed[0],
                    signedOriginalTx = signed[1],
                )

                // The flow is non-atomic; success is reported only when the original transfer broadcast.
                if (result.originalTxHash.isBlank() || result.status.contains("FAIL", ignoreCase = true)) {
                    error("Tron gasless original transaction failed: status=${result.status}")
                }
                result.originalTxHash
            },
            catch = { raise(SendTransactionError.DataError(it.message)) },
        )
    }

    private fun getSigner(userWallet: UserWallet): TransactionSigner {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card
                val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
                cardSdkConfigRepository.getCommonSigner(
                    cardId = card.cardId.takeIf { isCardNotBackedUp },
                    twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
                    userWalletId = userWallet.walletId,
                )
            }
            is UserWallet.Hot -> getHotWalletSigner(userWallet)
        }
    }
}