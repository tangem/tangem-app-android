package com.tangem.datasource.local.config.testnet

import com.tangem.datasource.local.config.testnet.models.TestnetTokensConfig

/**
 * Storage of testnet tokens data
 *
[REDACTED_AUTHOR]
 */
interface TestnetTokensStorage {

    /** Get a testnet tokens data */
    suspend fun getConfig(): TestnetTokensConfig
}