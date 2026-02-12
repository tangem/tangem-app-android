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

    fun shouldShowNextTimeUpgradeBanner(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setShouldShowNextTimeUpgradeBanner(userWalletId: UserWalletId, shouldShow: Boolean)

    suspend fun getUpgradeBannerClosureTimestamp(userWalletId: UserWalletId): Long?

    suspend fun setUpgradeBannerClosureTimestamp(userWalletId: UserWalletId, timestamp: Long)

    suspend fun getWalletCreationTimestamp(userWalletId: UserWalletId): Long?

    suspend fun setWalletCreationTimestamp(userWalletId: UserWalletId, timestamp: Long)

    suspend fun hasHadFirstTopUp(userWalletId: UserWalletId): Boolean

    suspend fun setHasHadFirstTopUp(userWalletId: UserWalletId, hasTopUp: Boolean)
}