package com.tangem.domain.wallets.repository

import com.tangem.domain.wallets.models.AppsFlyerConversionData

interface WalletsPromoRepository {

    suspend fun bindRefcodeWithWallets(conversionData: AppsFlyerConversionData)

    suspend fun retryBindRefcodeWithWallets()
}