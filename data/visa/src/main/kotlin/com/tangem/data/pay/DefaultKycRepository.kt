package com.tangem.data.pay

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.common.map
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaDataToSignByCustomerWallet
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.sdk.api.TangemSdkManager
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultKycRepository @AssistedInject constructor(
    @NetworkMoshi moshi: Moshi,
    private val tangemPayApi: TangemPayApi,
    private val visaAuthRepository: VisaAuthRepository,
    private val tangemSdkManager: TangemSdkManager,
) : KycRepository {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override suspend fun getKycStartInfo(address: String, cardId: String): Either<UniversalError, KycStartInfo> {
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
        return request {
            authHeader.ifEmpty { error("Cannot get auth header for KYC") }
            tangemPayApi.getKycAccess(authHeader = authHeader).getOrThrow().result
        }.map {
            KycStartInfo(
                token = it.token,
                locale = it.locale,
            )
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<VisaApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.errorBody != null
            ) {
                return runCatching {
                    visaErrorAdapter.fromJson(responseError.errorBody!!)?.error?.code ?: responseError.code.numericCode
                }.map {
                    Either.Left(VisaApiError.fromBackendError(it))
                }.getOrElse {
                    Either.Left(VisaApiError.UnknownWithoutCode)
                }
            }

            return Either.Left(VisaApiError.UnknownWithoutCode)
        }
    }

    @AssistedFactory
    interface Factory : KycRepository.Factory {
        override fun create(): DefaultKycRepository
    }
}