package com.tangem.domain.wallets.repository

import kotlinx.coroutines.flow.Flow

interface WalletsRepository {

    suspend fun initialize()

    suspend fun shouldSaveUserWalletsSync(): Boolean

    fun shouldSaveUserWallets(): Flow<Boolean>

    suspend fun saveShouldSaveUserWallets(item: Boolean)
}
