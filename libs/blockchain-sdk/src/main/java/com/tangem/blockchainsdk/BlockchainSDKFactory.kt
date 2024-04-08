package com.tangem.blockchainsdk

import com.tangem.blockchain.common.WalletManagerFactory
import kotlinx.coroutines.flow.Flow

/**
 * Blockchain SDK components factory
 *
 * @author Andrew Khokhlov on 04/04/2024
 */
interface BlockchainSDKFactory {

    /** Flow of [WalletManagerFactory] */
    val walletManagerFactory: Flow<WalletManagerFactory>

    /** Initialize components */
    suspend fun init()

    /** Get [WalletManagerFactory] synchronously */
    suspend fun getWalletManagerFactorySync(): WalletManagerFactory?
}
