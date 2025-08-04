package com.tangem.domain.visa

import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.domain.visa.repository.VisaRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetVisaTxHistoryUseCase(
    private val visaRepository: VisaRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        isRefresh: Boolean = false,
    ): Flow<Either<Throwable, PagingData<VisaTxHistoryItem>>> {
        return visaRepository
            .getTxHistory(userWalletId, pageSize, isRefresh)
            .map<PagingData<VisaTxHistoryItem>, Either<Throwable, PagingData<VisaTxHistoryItem>>> { it.right() }
            .catch { emit(it.left()) }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 10
    }
}