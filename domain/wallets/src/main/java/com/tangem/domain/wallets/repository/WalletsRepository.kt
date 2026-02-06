package com.tangem.domain.wallets.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface WalletsRepository {

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    suspend fun shouldSaveUserWalletsSync(): Boolean

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    fun shouldSaveUserWallets(): Flow<Boolean>

    @Deprecated("Hot wallet feature makes app always save user wallets. Do not use this method")
    suspend fun saveShouldSaveUserWallets(item: Boolean)

    suspend fun useBiometricAuthentication(): Boolean

    suspend fun setUseBiometricAuthentication(value: Boolean)

    suspend fun requireAccessCode(): Boolean

    suspend fun setRequireAccessCode(value: Boolean)

    suspend fun isWalletWithRing(userWalletId: UserWalletId): Boolean

    suspend fun setHasWalletsWithRing(userWalletId: UserWalletId)

    fun seedPhraseNotificationStatus(userWalletId: UserWalletId): Flow<SeedPhraseNotificationsStatus>

    suspend fun notifiedSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun confirmSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun declineSeedPhraseNotification(userWalletId: UserWalletId)

    suspend fun rejectSeedPhraseSecondNotification(userWalletId: UserWalletId)

    suspend fun acceptSeedPhraseSecondNotification(userWalletId: UserWalletId)

    suspend fun createWallet(userWalletId: UserWalletId)

    fun nftEnabledStatus(userWalletId: UserWalletId): Flow<Boolean>

    fun nftEnabledStatuses(): Flow<Map<UserWalletId, Boolean>>

    suspend fun enableNFT(userWalletId: UserWalletId)

    suspend fun disableNFT(userWalletId: UserWalletId)

    fun notificationsEnabledStatus(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun isNotificationsEnabled(userWalletId: UserWalletId): Boolean

    suspend fun setNotificationsEnabled(userWalletId: UserWalletId, isEnabled: Boolean)

    @Throws
    suspend fun setWalletName(walletId: UserWalletId, walletName: String)

    @Throws
    suspend fun upgradeWallet(walletId: UserWalletId)

    @Throws
    suspend fun getWalletInfo(walletId: UserWalletId): UserWalletRemoteInfo

    @Throws
    suspend fun getWalletsInfo(applicationId: String, updateCache: Boolean = true): List<UserWalletRemoteInfo>

    @Throws
    suspend fun associateWallets(applicationId: String, wallets: List<UserWallet>)

    suspend fun activatePromoCode(
        userWalletId: UserWalletId,
        promoCode: String,
        bitcoinAddress: String,
    ): Either<ActivatePromoCodeError, String>
}