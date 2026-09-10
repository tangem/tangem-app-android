package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectibleGachaMachineCategories
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import javax.inject.Inject
import com.tangem.utils.coroutines.runSuspendCatching

class GetCollectibleGachaMachineCategoriesUseCase @Inject constructor(
    private val repository: CollectiblesRepository,
) {

    suspend operator fun invoke(): Either<CollectiblesError, CollectibleGachaMachineCategories> {
        return runSuspendCatching { repository.getCategories() }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}