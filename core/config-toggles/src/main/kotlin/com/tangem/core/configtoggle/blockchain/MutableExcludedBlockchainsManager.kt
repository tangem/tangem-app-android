package com.tangem.core.configtoggle.blockchain

interface MutableExcludedBlockchainsManager : ExcludedBlockchainsManager {

    suspend fun excludeBlockchain(mainnetId: String, isExcluded: Boolean)

    fun isMatchLocalConfig(): Boolean

    suspend fun recoverLocalConfig()
}