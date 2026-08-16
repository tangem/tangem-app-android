package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.models.network.isBurnAddress
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigInteger

/**
 * Use case to create and get transfer transaction
 *
 * !!!IMPORTANT
 * Use when transaction data is compiled by us using BlockchainSDK methods
 */
class CreateTransferTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    /**
     * [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        nonce: BigInteger? = null,
    ) = Either.catch {
        requireSpendableDestination(destination)

        transactionRepository.createTransferTransaction(
            amount = amount,
            fee = fee,
            memo = memo,
            nonce = nonce,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
        )
    }

    /**
     * [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        amount: Amount,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        nonce: BigInteger? = null,
    ) = Either.catch {
        requireSpendableDestination(destination)

        transactionRepository.createTransferTransaction(
            amount = amount,
            memo = memo,
            fee = null,
            nonce = nonce,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
        )
    }

    /**
     * The recipient typed on the send screen is already rejected by [ValidateWalletAddressUseCase], but a send
     * deeplink jumps straight to the confirmation step, bypassing that screen — hence the check here, at the single
     * point every transfer is built at.
     *
     * A blank recipient is rejected together with the burn ones: it degrades into the zero address once the call
     * data is encoded.
     */
    private fun requireSpendableDestination(destination: String) {
        require(destination.isNotBlank()) { "Transfers with a blank destination are not allowed" }
        require(!destination.isBurnAddress()) { "Transfers to a burn address are not allowed" }
    }
}