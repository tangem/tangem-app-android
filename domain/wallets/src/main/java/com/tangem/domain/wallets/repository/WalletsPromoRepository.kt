package com.tangem.domain.wallets.repository

import arrow.core.Option
import com.tangem.domain.wallets.models.AppsFlyerConversionData

interface WalletsPromoRepository {

    suspend fun getConversionData(): Option<AppsFlyerConversionData>

    suspend fun saveConversionData(refcode: String, campaign: String?)

    suspend fun bindRefcodeWithWallets(refcode: String, campaign: String?)

    suspend fun bindSavedRefcodeWithWallets(): AppsFlyerConversionData
}