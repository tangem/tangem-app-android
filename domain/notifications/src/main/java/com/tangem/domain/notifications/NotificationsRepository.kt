package com.tangem.domain.notifications

import com.tangem.domain.notifications.models.NotificationsEligibleNetwork

interface NotificationsRepository {

    @Throws
    suspend fun createApplicationId(pushToken: String? = null): String

    suspend fun saveApplicationId(appId: String)

    suspend fun getApplicationId(): String?

    @Throws
    suspend fun setNotificationsEnabledForWallet(walletId: String, enabled: Boolean)

    @Throws
    suspend fun associateApplicationIdWithWallets(appId: String, wallets: List<String>)

    @Throws
    suspend fun isNotificationsEnabledForWallet(walletId: String): Boolean

    @Throws
    suspend fun setWalletName(walletId: String, walletName: String)

    @Throws
    suspend fun getWalletName(walletId: String): String?

    @Throws
    suspend fun sendPushToken(appId: String, pushToken: String)

    @Throws
    suspend fun getEligibleNetworks(): List<NotificationsEligibleNetwork>
}