package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface VisaRepository {

    suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean = false): VisaCurrency
}