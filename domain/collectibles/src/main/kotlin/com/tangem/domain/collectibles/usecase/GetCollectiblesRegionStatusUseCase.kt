package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.models.CollectiblesRegionStatus
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

class GetCollectiblesRegionStatusUseCase @Inject constructor(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(userWalletId: UserWalletId, skipCache: Boolean = false): CollectiblesRegionStatus {
        return runSuspendCatching { repository.isRegionBlocked(userWalletId = userWalletId, skipCache = skipCache) }
            .fold(
                onSuccess = { isBlocked ->
                    if (isBlocked) CollectiblesRegionStatus.RESTRICTED else CollectiblesRegionStatus.ALLOWED
                },
                onFailure = { error ->
                    TangemLogger.e("Collectibles: region check failed", error)
                    CollectiblesRegionStatus.UNKNOWN
                },
            )
    }
}