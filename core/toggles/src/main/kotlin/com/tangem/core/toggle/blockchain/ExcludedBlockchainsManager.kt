package com.tangem.core.toggle.blockchain

interface ExcludedBlockchainsManager {

    val excludedBlockchains: Map<String, Boolean>

    suspend fun init()
}
