package com.tangem.data.payment

import arrow.core.Either
import com.tangem.common.core.TangemError
import com.tangem.core.error.ext.tangemError
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.payment.models.auth.PaymentAuthChallenge
import com.tangem.sdk.api.visa.PaymentGenerateChallengeHelper

internal class DefaultPaymentGenerateChallengeHelper(
    private val remoteDataSource: PaymentRemoteDataSource,
) : PaymentGenerateChallengeHelper {

    override suspend fun generateChallenge(
        address: String,
        walletId: String,
    ): Either<TangemError, PaymentAuthChallenge> {
        return remoteDataSource.getCustomerWalletAuthChallenge(
            customerWalletAddress = address,
            customerWalletId = walletId,
        ).mapLeft { it.tangemError }
    }
}