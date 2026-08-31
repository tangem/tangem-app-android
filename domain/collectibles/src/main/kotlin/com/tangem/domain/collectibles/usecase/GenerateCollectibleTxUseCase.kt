package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectibleTransaction
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.GenerateTxParams
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.utils.coroutines.runSuspendCatching

class GenerateCollectibleTxUseCase(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(params: GenerateTxParams): Either<CollectiblesError, CollectibleTransaction> {
        return runSuspendCatching { repository.generateTx(params) }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}