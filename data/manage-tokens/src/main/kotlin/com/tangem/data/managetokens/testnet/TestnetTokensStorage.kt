package com.tangem.data.managetokens.testnet

import com.tangem.data.managetokens.testnet.models.TestnetTokensConfig

/**
 * Storage of testnet tokens data
 */
internal interface TestnetTokensStorage {

    /** Get a testnet tokens data */
    suspend fun getConfig(): TestnetTokensConfig
}