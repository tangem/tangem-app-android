package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.utils.coroutines.runSuspendCatching

class GetCollectibleGachaMachineUseCase(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(externalId: String): Either<CollectiblesError, CollectibleGachaMachine> {
        return runSuspendCatching { repository.getMachine(externalId) }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}