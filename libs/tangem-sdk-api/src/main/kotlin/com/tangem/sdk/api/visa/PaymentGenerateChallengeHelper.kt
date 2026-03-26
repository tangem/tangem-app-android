package com.tangem.sdk.api.visa

import arrow.core.Either
import com.tangem.common.core.TangemError
import com.tangem.domain.payment.models.auth.PaymentAuthChallenge

interface PaymentGenerateChallengeHelper {

    suspend fun generateChallenge(address: String, walletId: String): Either<TangemError, PaymentAuthChallenge>
}