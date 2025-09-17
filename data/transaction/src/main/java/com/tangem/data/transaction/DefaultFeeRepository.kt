package com.tangem.data.transaction

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
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

internal class DefaultFeeRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
) : FeeRepository {

    override fun isFeeApproximate(networkId: Network.ID, amountType: AmountType): Boolean {
        return networkId.toBlockchain().isFeeApproximate(amountType)
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
}