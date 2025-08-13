package com.tangem.data.visa

import androidx.paging.PagingData
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.visa.repository.VisaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class MockVisaRepository @Inject constructor() : VisaRepository {

    override suspend fun getVisaCurrency(userWalletId: UserWalletId, isRefresh: Boolean): VisaCurrency {
        TODO("Not implemented for this build type")
    }

    override suspend fun getTxHistory(
        userWalletId: UserWalletId,
        pageSize: Int,
        isRefresh: Boolean,
    ): Flow<PagingData<VisaTxHistoryItem>> {
        TODO("Not implemented for this build type")
    }

    override suspend fun getTxDetails(userWalletId: UserWalletId, txId: String): VisaTxDetails {
        TODO("Not implemented for this build type")
    }
}