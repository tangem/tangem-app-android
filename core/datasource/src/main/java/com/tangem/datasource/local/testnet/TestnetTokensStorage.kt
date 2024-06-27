package com.tangem.datasource.local.testnet

import com.tangem.datasource.local.testnet.models.TestnetTokensConfig

/**
 * Storage of testnet tokens data
 *
[REDACTED_AUTHOR]
 */
interface TestnetTokensStorage {

    /** Get a testnet tokens data */
    suspend fun getConfig(): TestnetTokensConfig
}