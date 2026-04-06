package com.tangem.data.dynamicaddresses

import arrow.core.Either
import com.tangem.blockchain.common.DynamicAddressesManager
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultConsolidationRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : ConsolidationRepository {

    override suspend fun createConsolidationTransaction(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, TransactionData> = Either.catch {
        withContext(dispatchers.io) {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
                ?: error("WalletManager not found")

            val dynamicAddressesManager = walletManager as? DynamicAddressesManager
                ?: error("WalletManager does not support dynamic addresses")

            val fee = getNormalFee(walletManager)

            when (val result = dynamicAddressesManager.createConsolidationTransaction(fee)) {
                is Result.Success -> result.data
                is Result.Failure -> error("Failed to create consolidation tx: ${result.error}")
            }
        }
    }

    @Suppress("UnusedParameter")
    private fun getNormalFee(walletManager: WalletManager): Fee {
        TODO("Fee calculation for consolidation from multiple addresses is not yet implemented")
    }
}