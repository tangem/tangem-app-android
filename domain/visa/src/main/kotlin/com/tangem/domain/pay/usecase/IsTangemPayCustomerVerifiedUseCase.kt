package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.pay.utils.TangemPayCustomerStateConverter
import com.tangem.domain.visa.error.VisaApiError

class IsTangemPayCustomerVerifiedUseCase(
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Boolean> = either {
        val summary = getCustomerSummaryUseCase(
            userWalletId = userWalletId,
            cachePolicy = CachePolicy.REFRESH,
        ).bind()

        ensureNotNull(summary) { VisaApiError.UnknownWithoutCode }

        TangemPayCustomerStateConverter.convert(summary) == TangemPayCustomerState.KycApproved
    }
}