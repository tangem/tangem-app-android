package com.tangem.data.pay.repository

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.visa.error.VisaApiError
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultKycRepository @AssistedInject constructor(
    @NetworkMoshi moshi: Moshi,
    private val tangemPayApi: TangemPayApi,
    private val authDataSource: TangemPayAuthDataSource,
    private val tangemPayStorage: TangemPayStorage,
) : KycRepository {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override suspend fun getKycStartInfo(address: String, cardId: String): Either<UniversalError, KycStartInfo> {
        val authHeader = authDataSource.generateNewAuthHeader(address, cardId)
            .getOrNull()
            .takeIf { !it.isNullOrEmpty() }
            ?: return Either.Left(VisaApiError.UnknownWithoutCode)
        tangemPayStorage.store(authHeader)
        return getKycInfo(authHeader)
    }

    override suspend fun getKycStartInfo(authHeader: String): Either<UniversalError, KycStartInfo> {
        return getKycInfo(authHeader)
    }

    private suspend fun getKycInfo(authHeader: String): Either<UniversalError, KycStartInfo> {
        return request {
            tangemPayApi.getKycAccess(authHeader = authHeader).getOrThrow().result
        }.map {
            KycStartInfo(token = it.token, locale = it.locale)
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