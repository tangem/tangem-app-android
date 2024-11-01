package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.sdk.api.TangemSdkManager

internal class DefaultDeleteSavedAccessCodesUseCase(
    private val tangemSdkManager: TangemSdkManager,
) : DeleteSavedAccessCodesUseCase {

    override suspend fun invoke(cardId: String): Either<Throwable, Unit> {
        tangemSdkManager.deleteSavedUserCodes(setOf(cardId))
            .doOnFailure { return it.left() }
            .doOnSuccess { return Unit.right() }

        return Unit.right()
    }
}