package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.memo.MemoValidatorFactory

/**
 * Blockchain SDK components factory
 *
[REDACTED_AUTHOR]
 */
interface BlockchainSDKFactory {

    /** Initialize components */
    suspend fun init()

    /** Get [WalletManagerFactory] synchronously */
    suspend fun getWalletManagerFactorySync(): WalletManagerFactory?

    /** Get [MemoValidatorFactory] synchronously */
    suspend fun getMemoValidatorFactorySync(): MemoValidatorFactory?
}