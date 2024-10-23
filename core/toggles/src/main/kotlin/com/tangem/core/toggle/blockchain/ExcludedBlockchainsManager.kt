package com.tangem.core.toggle.blockchain

interface ExcludedBlockchainsManager {

    val excludedBlockchainsIds: Set<String>

    suspend fun init()
}
