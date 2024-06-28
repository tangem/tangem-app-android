package com.tangem.domain.wallets.repository

/**
 * Access to migrate names flag
 */
interface WalletNamesMigrationRepository {

    suspend fun isMigrationDone(): Boolean

    suspend fun setMigrationDone()
}