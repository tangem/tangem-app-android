package com.tangem.data.sociallogin.store

import arrow.core.getOrElse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Which wallets have social recovery fully set up. Kept on device so backup checks never wait on the cloud
 * or the backend.
 */
internal interface SocialBackupStore {

    fun getCompletedWalletIds(): Flow<Set<String>>

    suspend fun setCompleted(walletId: String, isCompleted: Boolean)
}

/**
 * The mark is part of the wallet's own record, so a wallet is never stored without it and takes it along
 * when it is removed.
 *
 * The repository is lazy: through its own dependencies the wallets list already depends on this store.
 */
internal class UserWalletSocialBackupStore(
    private val userWalletsListRepositoryLazy: Lazy<UserWalletsListRepository>,
) : SocialBackupStore {

    private val userWalletsListRepository: UserWalletsListRepository
        get() = userWalletsListRepositoryLazy.get()

    override fun getCompletedWalletIds(): Flow<Set<String>> = userWalletsListRepository.userWallets
        .filterNotNull()
        .map { userWallets ->
            userWallets
                .filterIsInstance<UserWallet.Hot>()
                .filter(UserWallet.Hot::isSocialBackupCompleted)
                .mapTo(mutableSetOf()) { it.walletId.stringValue }
        }

    /** A wallet that is not on this device has no record to mark, so there is nothing to change */
    override suspend fun setCompleted(walletId: String, isCompleted: Boolean) {
        val userWallet = userWalletsListRepository.userWalletsSync()
            .filterIsInstance<UserWallet.Hot>()
            .find { it.walletId.stringValue == walletId }
            ?: return

        if (userWallet.isSocialBackupCompleted == isCompleted) return

        userWalletsListRepository
            .saveWithoutLock(userWallet = userWallet.copy(isSocialBackupCompleted = isCompleted), canOverride = true)
            .getOrElse { error("Failed to save the social backup mark: $it") }
    }
}