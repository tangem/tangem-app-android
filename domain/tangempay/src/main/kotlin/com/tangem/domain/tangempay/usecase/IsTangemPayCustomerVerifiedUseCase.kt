package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CachePolicy
import com.tangem.domain.tangempay.model.TangemPayCustomerState
import com.tangem.domain.tangempay.utils.TangemPayCustomerStateConverter
import com.tangem.domain.tangempay.error.TangemPayApiError

class IsTangemPayCustomerVerifiedUseCase(
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, Boolean> = either {
        val summary = getCustomerSummaryUseCase(
            userWalletId = userWalletId,
            cachePolicy = CachePolicy.REFRESH,
        ).bind()

        ensureNotNull(summary) { TangemPayApiError.UnknownWithoutCode }

        TangemPayCustomerStateConverter.convert(summary) == TangemPayCustomerState.KycApproved
    }
}