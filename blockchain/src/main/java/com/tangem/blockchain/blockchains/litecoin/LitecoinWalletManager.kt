package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class LitecoinWalletManager(
        cardId: String,
        wallet: Wallet,
        transactionBuilder: BitcoinTransactionBuilder,
        networkManager: LitecoinNetworkManager
) : BitcoinWalletManager(cardId, wallet, transactionBuilder, networkManager), TransactionSender {
    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val minimalFee = BigDecimal("0.00001")
        when (val result = super.getFee(amount, destination)) {
            is Result.Success -> {
                for (fee in result.data) {
                    if (fee.value!! < minimalFee) fee.value = minimalFee
                }
                return result
            }
            is Result.Failure -> return result
        }
    }
}