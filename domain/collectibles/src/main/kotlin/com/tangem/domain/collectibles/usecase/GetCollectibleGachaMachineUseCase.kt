package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

class GetCollectibleGachaMachineUseCase @Inject constructor(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(
        externalId: String,
        skipCache: Boolean = false,
    ): Either<CollectiblesError, CollectibleGachaMachine> {
        return runSuspendCatching { repository.getMachine(externalId = externalId, skipCache = skipCache) }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}