package com.tangem.domain.hotwallet.repository

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface HotWalletRepository {

    fun isWalletCreationSupported(): Boolean

    fun getLeastSupportedAndroidVersionName(): String

    fun accessCodeSkipped(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setAccessCodeSkipped(userWalletId: UserWalletId, skipped: Boolean)
}