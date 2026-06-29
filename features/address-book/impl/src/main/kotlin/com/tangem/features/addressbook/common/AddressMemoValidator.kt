package com.tangem.features.addressbook.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class AddressMemoValidator @Inject constructor(
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend fun isValid(blockchain: Blockchain, memo: String): Boolean = withContext(dispatchers.io) {
        val factory = blockchainSDKFactory.getMemoValidatorFactorySync() ?: return@withContext true
        when (val result = factory.create(blockchain).validateMemo(memo)) {
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