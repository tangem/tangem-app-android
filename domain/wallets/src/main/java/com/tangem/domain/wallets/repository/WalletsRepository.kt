package com.tangem.domain.wallets.repository

interface WalletsRepository {

    suspend fun shouldSaveUserWallets(): Boolean
}