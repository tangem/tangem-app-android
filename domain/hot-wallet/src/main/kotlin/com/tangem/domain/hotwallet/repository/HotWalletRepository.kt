package com.tangem.domain.hotwallet.repository

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface HotWalletRepository {

    fun isWalletCreationSupported(): Boolean

    fun getLeastSupportedAndroidVersionName(): String

    fun accessCodeSkipped(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setAccessCodeSkipped(userWalletId: UserWalletId, skipped: Boolean)

    fun shouldShowUpgradeBanner(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setShouldShowUpgradeBanner(userWalletId: UserWalletId, shouldShow: Boolean)

    fun upgradeBannerClosureTimestamp(userWalletId: UserWalletId): Flow<Long?>

    suspend fun setUpgradeBannerClosureTimestamp(userWalletId: UserWalletId, timestamp: Long?)

    suspend fun getWalletCreationTimestamp(userWalletId: UserWalletId): Long?

    suspend fun setWalletCreationTimestamp(userWalletId: UserWalletId, timestamp: Long)

    suspend fun hasHadFirstTopUp(userWalletId: UserWalletId): Boolean

    suspend fun setHasHadFirstTopUp(userWalletId: UserWalletId, hasTopUp: Boolean)

    fun isFirstTopUpDetectedThisSession(userWalletId: UserWalletId): Boolean

    fun markFirstTopUpDetectedThisSession(userWalletId: UserWalletId)
}