package com.tangem.datasource.local.config.testnet

import com.tangem.datasource.local.config.testnet.models.TestnetTokensConfig

/**
 * Storage of testnet tokens data
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
interface TestnetTokensStorage {

    /** Get a testnet tokens data */
    suspend fun getConfig(): TestnetTokensConfig
}
