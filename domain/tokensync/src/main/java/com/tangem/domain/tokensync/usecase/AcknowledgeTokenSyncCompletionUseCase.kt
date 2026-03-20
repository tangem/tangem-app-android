package com.tangem.domain.tokensync.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokensync.repository.TokenSyncRepository

class AcknowledgeTokenSyncCompletionUseCase(
    private val tokenSyncRepository: TokenSyncRepository,
) {

    operator fun invoke(userWalletId: UserWalletId) {
        tokenSyncRepository.acknowledgeCompletion(userWalletId)
    }
}