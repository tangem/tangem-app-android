package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.models.network.isBurnAddress
import com.tangem.domain.models.wallet.UserWalletId

class CreateNFTTransferTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {
    /**
     * [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        ownerAddress: String,
        nftAsset: NFTAsset,
        fee: Fee,
        memo: String?,
        destinationAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ) = Either.catch {
        requireSpendableDestination(destinationAddress)

        transactionRepository.createNFTTransferTransaction(
            ownerAddress = ownerAddress,
            nftAsset = nftAsset,
            destinationAddress = destinationAddress,
            fee = fee,
            memo = memo,
            userWalletId = userWalletId,
            network = network,
        )
    }

    /**
     * [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        ownerAddress: String,
        nftAsset: NFTAsset,
        memo: String?,
        destinationAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ) = Either.catch {
        requireSpendableDestination(destinationAddress)

        transactionRepository.createNFTTransferTransaction(
            ownerAddress = ownerAddress,
            nftAsset = nftAsset,
            destinationAddress = destinationAddress,
            fee = null,
            memo = memo,
            userWalletId = userWalletId,
            network = network,
        )
    }

    /** Same barrier as on the coin/token transfer path, see [CreateTransferTransactionUseCase]. */
    private fun requireSpendableDestination(destinationAddress: String) {
        require(destinationAddress.isNotBlank()) { "Transfers with a blank destination are not allowed" }
        require(!destinationAddress.isBurnAddress()) { "Transfers to a burn address are not allowed" }
    }
}