package com.tangem.core.toggle.blockchain

interface MutableExcludedBlockchainsManager : ExcludedBlockchainsManager {

    suspend fun toggleBlockchain(mainnetId: String, isExcluded: Boolean)

    fun isMatchLocalConfig(): Boolean

    suspend fun recoverLocalConfig()
}
