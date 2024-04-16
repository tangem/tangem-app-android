package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory

/**
 * Blockchain SDK components factory
 *
 * @author Andrew Khokhlov on 04/04/2024
 */
interface BlockchainSDKFactory {

    /** Initialize components */
    suspend fun init()

    /** Get [WalletManagerFactory] synchronously */
    suspend fun getWalletManagerFactorySync(): WalletManagerFactory?
}
