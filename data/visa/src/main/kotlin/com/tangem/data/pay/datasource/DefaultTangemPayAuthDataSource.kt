package com.tangem.data.pay.datasource

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.CompletionResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaDataToSignByCustomerWallet
import com.tangem.domain.visa.datasource.VisaAuthRemoteDataSource
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val visaAuthRemoteDataSource: VisaAuthRemoteDataSource,
    private val tangemSdkManager: TangemSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun generateNewAuthHeader(address: String, cardId: String): Either<Throwable, String> = either {
        val challenge = visaAuthRemoteDataSource
            .getCustomerWalletAuthChallenge(address)
            .mapLeft { IllegalStateException("TangemPay challenge failed. Error code: ${it.errorCode}") }
            .bind()

        val signed = tangemSdkManager.visaCustomerWalletApprove(
            VisaDataForApprove(
                customerWalletCardId = cardId,
                targetAddress = address,
                dataToSign = VisaDataToSignByCustomerWallet(hashToSign = challenge.challenge),
            ),
        ).toEither { IllegalStateException("TangemPay signing failed: $it") }.bind()

        visaAuthRemoteDataSource.getTokenWithCustomerWallet(
            sessionId = challenge.session.sessionId,
            signature = signed.signature,
            nonce = signed.dataToSign.hashToSign,
        )
            .mapLeft { IllegalStateException("TangemPay token fetch failed. Error code: ${it.errorCode}") }
            .bind()
    }
}

private fun <T> CompletionResult<T>.toEither(map: (Throwable) -> Throwable) = when (this) {
    is CompletionResult.Success -> Either.Right(data)
    is CompletionResult.Failure -> Either.Left(map(error))
}