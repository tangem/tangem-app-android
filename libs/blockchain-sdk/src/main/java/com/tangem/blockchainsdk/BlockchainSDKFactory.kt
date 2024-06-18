package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory

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
}