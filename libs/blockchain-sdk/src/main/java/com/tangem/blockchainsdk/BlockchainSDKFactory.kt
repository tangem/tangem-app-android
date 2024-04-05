package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory
import kotlinx.coroutines.flow.Flow

/**
 * Blockchain SDK components factory
 *
[REDACTED_AUTHOR]
 */
interface BlockchainSDKFactory {

    /** Flow of [WalletManagerFactory] */
    val walletManagerFactory: Flow<WalletManagerFactory>

    /** Initialize components */
    suspend fun init()

    /** Get [WalletManagerFactory] synchronously */
    suspend fun getWalletManagerFactorySync(): WalletManagerFactory?
}