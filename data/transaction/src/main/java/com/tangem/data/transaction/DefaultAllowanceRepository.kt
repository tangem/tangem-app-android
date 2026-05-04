package com.tangem.data.transaction

import com.tangem.blockchain.common.Approver
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.AllowanceRepository
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class DefaultAllowanceRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : AllowanceRepository {

    override suspend fun getAllowanceInfo(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
        requiredAmount: BigDecimal,
    ): AllowanceInfo {
        if (cryptoCurrency !is CryptoCurrency.Token) {
            error("CryptoCurrency must be of type Token")
        }

        val allowance = getAllowance(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
            spenderAddress = spenderAddress,
        )

        return when {
            allowance >= requiredAmount -> AllowanceInfo.Enough(allowance)
            allowance > BigDecimal.ZERO && allowance < requiredAmount &&
                BlockchainUtils.isTetherInEthereum(
                    blockchainId = cryptoCurrency.network.rawId,
                    contractAddress = cryptoCurrency.contractAddress,
                ) -> AllowanceInfo.ResetNeeded(allowance, requiredAmount)
            else -> AllowanceInfo.NotEnough(allowance, requiredAmount)
        }
    }

    override suspend fun getAllowance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        spenderAddress: String,
    ): BigDecimal = withContext(dispatchers.io) {
        if (cryptoCurrency !is CryptoCurrency.Token) {
            error("CryptoCurrency must be of type Token")
        }

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, cryptoCurrency.network)
        val blockchain = cryptoCurrency.network.toBlockchain()
        val allowanceResult = (walletManager as? Approver)?.getAllowance(
            spenderAddress,
            Token(
                symbol = blockchain.currency,
                contractAddress = cryptoCurrency.contractAddress,
                decimals = cryptoCurrency.decimals,
            ),
        ) ?: error("Cannot cast to Approver")

        allowanceResult.fold(
            onSuccess = { it },
            onFailure = { error(it) },
        )
    }
}