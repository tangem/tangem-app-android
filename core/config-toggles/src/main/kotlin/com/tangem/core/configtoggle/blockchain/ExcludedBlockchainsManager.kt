package com.tangem.core.configtoggle.blockchain

interface ExcludedBlockchainsManager {

    val excludedBlockchainsIds: Set<String>

    suspend fun init()
}