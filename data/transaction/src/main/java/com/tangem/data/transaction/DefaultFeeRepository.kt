package com.tangem.data.transaction

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.blockchains.ethereum.network.EthereumFeeHistory
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal
import java.math.BigInteger

internal class DefaultFeeRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
) : FeeRepository {

    override fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean {
        return networkId.toBlockchain().isFeeApproximate(amountType)
    }

    override suspend fun getEthereumFeeWithoutGas(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): Fee.Ethereum {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            network = cryptoCurrency.network,
        )

        val blockchain = cryptoCurrency.network.toBlockchain()

        val ethereumWalletManager = walletManager as? EthereumWalletManager
            ?: error("Not supported for ${cryptoCurrency.network}")

        val fee = if (blockchain.isSupportEIP1559) {
            val gasHistory = when (val gasHistory = ethereumWalletManager.getGasHistory()) {
                is Result.Failure -> throw gasHistory.error
                is Result.Success -> gasHistory.data
            }

            val marketPriorityFee = when (gasHistory) {
                is EthereumFeeHistory.Common -> gasHistory.marketPriorityFee
                is EthereumFeeHistory.Fallback -> gasHistory.gasPrice.toBigDecimal() * MULTIPLIER_GAS_PRICE_NORMAL_FEE
            }

            val maxFeePerGas = gasHistory.baseFee * MULTIPLIER_GAS_PRICE_NORMAL_FEE + marketPriorityFee

            getEthEip1559Fee(
                maxFeePerGas = maxFeePerGas.toBigInteger(),
                priorityFee = marketPriorityFee.toBigInteger(),
                blockchain = blockchain,
            )
        } else {
            val gasPrice = when (val gasPrice = ethereumWalletManager.getGasPrice()) {
                is Result.Failure -> throw gasPrice.error
                is Result.Success -> gasPrice.data
            }

            getEthLegacyFee(
                gasPrice = gasPrice,
                blockchain = blockchain,
            )
        }

        return fee
    }

    override suspend fun calculateFee(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
        transactionData: TransactionData,
    ): TransactionFee {
        val transactionSender = if (userWallet is UserWallet.Cold &&
            demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
        ) {
            demoTransactionSender(userWallet, cryptoCurrency)
        } else {
            walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
            ) ?: error("WalletManager is null")
        }

        return when (val result = transactionSender.getFee(transactionData)) {
            is Result.Success -> result.data
            is Result.Failure -> throw result.error
        }
    }

    private suspend fun demoTransactionSender(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, cryptoCurrency.network)
                ?: error("WalletManager is null"),
        )
    }

    private fun getEthLegacyFee(gasPrice: BigInteger, blockchain: Blockchain): Fee.Ethereum.Legacy {
        val amount = Amount(
            value = BigDecimal.ZERO,
            blockchain = blockchain,
        )
        return Fee.Ethereum.Legacy(
            amount = amount,
            gasLimit = BigInteger.ZERO,
            gasPrice = gasPrice,
        )
    }

    private fun getEthEip1559Fee(
        maxFeePerGas: BigInteger,
        priorityFee: BigInteger,
        blockchain: Blockchain,
    ): Fee.Ethereum.EIP1559 {
        val amount = Amount(
            value = BigDecimal.ZERO,
            blockchain = blockchain,
        )
        return Fee.Ethereum.EIP1559(
            amount = amount,
            gasLimit = BigInteger.ZERO,
            maxFeePerGas = maxFeePerGas,
            priorityFee = priorityFee,
        )
    }

    private companion object {
        val MULTIPLIER_GAS_PRICE_NORMAL_FEE = "1.2".toBigDecimal() // 120%
    }
}