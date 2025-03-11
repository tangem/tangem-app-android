package com.tangem.domain.card

import arrow.core.Either

interface DeleteSavedAccessCodesUseCase {

    suspend operator fun invoke(cardId: String): Either<Throwable, Unit>
}