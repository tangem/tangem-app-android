package com.tangem.core.toggle.blockchain

interface MutableExcludedBlockchainsManager : ExcludedBlockchainsManager {

    suspend fun excludeBlockchain(mainnetId: String, isExcluded: Boolean)

    fun isMatchLocalConfig(): Boolean

    suspend fun recoverLocalConfig()
}
