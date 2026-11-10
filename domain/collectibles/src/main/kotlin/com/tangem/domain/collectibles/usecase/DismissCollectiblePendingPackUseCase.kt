package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import javax.inject.Inject

class DismissCollectiblePendingPackUseCase @Inject constructor(
    private val dismissedTxRepository: CollectiblesDismissedTxRepository,
) {

    suspend operator fun invoke(txId: String) {
        dismissedTxRepository.saveAsDismissed(txId)
    }
}