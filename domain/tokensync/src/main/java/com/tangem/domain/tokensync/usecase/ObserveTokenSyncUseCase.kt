package com.tangem.domain.tokensync.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokensync.model.TokenSyncProgress
import com.tangem.domain.tokensync.repository.TokenSyncRepository
import kotlinx.coroutines.flow.Flow

class ObserveTokenSyncUseCase(
    private val tokenSyncRepository: TokenSyncRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<TokenSyncProgress> {
        return tokenSyncRepository.observeSyncProgress(userWalletId)
    }
}