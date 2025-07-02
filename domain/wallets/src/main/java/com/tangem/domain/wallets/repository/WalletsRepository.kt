package com.tangem.domain.wallets.repository

import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface WalletsRepository {

    suspend fun shouldSaveUserWalletsSync(): Boolean

    fun shouldSaveUserWallets(): Flow<Boolean>

    suspend fun saveShouldSaveUserWallets(item: Boolean)

    suspend fun isWalletWithRing(userWalletId: UserWalletId): Boolean

    suspend fun setHasWalletsWithRing(userWalletId: UserWalletId)

    fun seedPhraseNotificationStatus(userWalletId: UserWalletId): Flow<SeedPhraseNotificationsStatus>

    suspend fun notifiedSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun confirmSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun declineSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun rejectSeedPhraseSecondNotification(userWalletId: UserWalletId)

    suspend fun acceptSeedPhraseSecondNotification(userWalletId: UserWalletId)

    suspend fun markWallet2WasCreated(userWalletId: UserWalletId)

    fun nftEnabledStatus(userWalletId: UserWalletId): Flow<Boolean>

    fun nftEnabledStatuses(): Flow<Map<UserWalletId, Boolean>>

    suspend fun enableNFT(userWalletId: UserWalletId)

    suspend fun disableNFT(userWalletId: UserWalletId)

    fun notificationsEnabledStatus(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun isNotificationsEnabled(userWalletId: UserWalletId): Boolean

    suspend fun setNotificationsEnabled(userWalletId: UserWalletId, isEnabled: Boolean)

    @Throws
    suspend fun setWalletName(walletId: String, walletName: String)

    @Throws
    suspend fun getWalletInfo(walletId: String): UserWalletRemoteInfo

    @Throws
    suspend fun getWalletsInfo(applicationId: String, updateCache: Boolean = true): List<UserWalletRemoteInfo>

    @Throws
    suspend fun associateWallets(applicationId: String, wallets: List<UserWallet>)
}