package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class DucatusWalletManager(
        cardId: String,
        wallet: Wallet,
        transactionBuilder: BitcoinTransactionBuilder,
        networkManager: DucatusNetworkManager
) : BitcoinWalletManager(cardId, wallet, transactionBuilder, networkManager), TransactionSender {
    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
        val sizeResult = transactionBuilder.getEstimateSize(
                TransactionData(amount, Amount(amount, feeValue), wallet.address, destination)
        )
        return when (sizeResult) {
            is Result.Failure -> sizeResult
            is Result.Success -> {
                val transactionSize = sizeResult.data.toBigDecimal()
                val minFee = BigDecimal.valueOf(0.00000089).multiply(transactionSize)
                val normalFee = BigDecimal.valueOf(0.00000144).multiply(transactionSize)
                val priorityFee = BigDecimal.valueOf(0.00000350).multiply(transactionSize)
                val fees = listOf(
                        Amount(minFee, blockchain),
                        Amount(normalFee, blockchain),
                        Amount(priorityFee, blockchain)
                )
                Result.Success(fees)
            }
        }
    }
}