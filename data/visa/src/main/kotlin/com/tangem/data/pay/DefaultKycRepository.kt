package com.tangem.data.pay

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

@Suppress("UnusedPrivateMember")
class DefaultKycRepository @AssistedInject constructor(
    @Assisted userWalletId: UserWalletId,
    @NetworkMoshi moshi: Moshi,
    private val tangemPayApi: TangemPayApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : KycRepository {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override suspend fun getKycStartInfo(): Either<UniversalError, KycStartInfo> = withContext(dispatcherProvider.io) {
        val authTokenForSpecificWallet = "get from userWalletId"

        request {
            tangemPayApi.getKycAccess(
                authHeader = authTokenForSpecificWallet,
            ).getOrThrow().result
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
        override fun create(userWalletId: UserWalletId): DefaultKycRepository
    }
}