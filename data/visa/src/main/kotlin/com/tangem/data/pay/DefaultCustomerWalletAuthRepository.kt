package com.tangem.data.pay

import arrow.core.Either
import com.tangem.common.map
import com.tangem.datasource.local.visa.PayStorage
import com.tangem.domain.pay.repository.CustomerWalletAuthRepository
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaDataToSignByCustomerWallet
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

class DefaultCustomerWalletAuthRepository @Inject constructor(
    private val visaAuthRepository: VisaAuthRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val payStorage: PayStorage,
) : CustomerWalletAuthRepository {

    override suspend fun generateNewAuthHeader(address: String, cardId: String): Either<Throwable, String> {
        return customerWalletAuth(address, cardId)
    }

    private suspend fun customerWalletAuth(address: String, cardId: String): Either<Throwable, String> {
        var authHeader = ""
        visaAuthRepository.getCustomerWalletAuthChallenge(address).getOrNull()?.let { result ->
            tangemSdkManager.visaCustomerWalletApprove(
                VisaDataForApprove(
                    customerWalletCardId = cardId,
                    targetAddress = address,
                    dataToSign = VisaDataToSignByCustomerWallet(hashToSign = result.challenge),
                ),
            ).map { signResult ->
                visaAuthRepository.getTokenWithCustomerWallet(
                    sessionId = result.session.sessionId,
                    signature = signResult.signature,
                    nonce = signResult.dataToSign.hashToSign,
                ).getOrNull()?.let { authHeader = it }
            }
        }
        return if (authHeader.isEmpty()) {
            Either.Left(IllegalStateException("Cannot get auth header for Tangem Pay"))
        } else {
            payStorage.store(authHeader)
            Either.Right(authHeader)
        }
    }
}