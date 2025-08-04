package com.tangem.domain.visa.repository

import androidx.paging.PagingData
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface VisaRepository {

    suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean = false): VisaCurrency

    suspend fun getTxHistory(
        userWalletId: UserWalletId,
        pageSize: Int,
        isRefresh: Boolean = false,
    ): Flow<PagingData<VisaTxHistoryItem>>

    suspend fun getTxDetails(userWalletId: UserWalletId, txId: String): VisaTxDetails
}