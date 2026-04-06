package com.tangem.data.transaction

import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.MemoValidatorFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultMemoValidatorFacade(
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MemoValidatorFacade {

    override suspend fun isMemoRequired(network: Network, destinationAddress: String): Boolean =
        withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()
            val factory = blockchainSDKFactory.getMemoValidatorFactorySync() ?: return@withContext false
            val validator = factory.create(blockchain)
            when (val result = validator.isMemoRequired(destinationAddress)) {
                is Result.Success -> result.data
                is Result.Failure -> false
            }
        }

    override suspend fun validateMemo(network: Network, memo: String): Boolean = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
        val factory = blockchainSDKFactory.getMemoValidatorFactorySync() ?: return@withContext true
        val validator = factory.create(blockchain)
        when (val result = validator.validateMemo(memo)) {
            is Result.Success -> when (result.data) {
                MemoState.Valid,
                MemoState.NotSupported,
                -> true
                MemoState.Invalid -> false
            }
            is Result.Failure -> true
        }
    }
}