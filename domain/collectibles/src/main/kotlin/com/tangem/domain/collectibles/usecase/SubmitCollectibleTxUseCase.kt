package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.SubmittedCollectibleTransaction
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching

class SubmitCollectibleTxUseCase(private val repository: CollectiblesRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        signedTxData: String,
    ): Either<CollectiblesError, SubmittedCollectibleTransaction> {
        return runSuspendCatching { repository.submitTx(userWalletId, signedTxData) }
            .fold(
                onSuccess = { it.right() },
                onFailure = { CollectiblesError.Api(it).left() },
            )
    }
}