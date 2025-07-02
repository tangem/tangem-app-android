package com.tangem.data.visa

import arrow.core.Either
import arrow.core.getOrElse
import com.squareup.moshi.Moshi
import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.data.visa.converter.VisaActivationStatusConverterWithState
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.datasource.api.visa.models.request.*
import com.tangem.datasource.api.visa.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class DefaultVisaActivationRepository @AssistedInject constructor(
    @Assisted private val visaCardId: VisaCardId,
    @NetworkMoshi private val moshi: Moshi,
    private val visaApi: TangemVisaApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val visaAuthRepository: VisaAuthRepository,
    private val visaLibLoader: VisaLibLoader,
    private val apiConfigsManager: ApiConfigsManager,
) : VisaActivationRepository {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override suspend fun getActivationRemoteState(): Either<VisaApiError, VisaActivationRemoteState> =
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

                visaApi.getRemoteActivationStatus(
                    authHeader = authTokens.getAuthHeader(),
                    request = ActivationStatusRequest(
                        cardId = visaCardId.cardId,
                        cardPublicKey = visaCardId.cardPublicKey,
                    ),
                ).getOrThrow()
            }.map {
                VisaActivationStatusConverterWithState.convert(it)
            }
        }

    override suspend fun getCardWalletAcceptanceData(
        request: VisaCardWalletDataToSignRequest,
    ): Either<VisaApiError, VisaDataToSignByCardWallet> = withContext(dispatcherProvider.io) {
        request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.getCardWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                request = GetCardWalletAcceptanceRequest(
                    customerWalletAddress = request.activationOrderInfo.customerWalletAddress,
                    cardWalletAddress = request.cardWalletAddress,
                ),
            ).getOrThrow()
        }.map {
            VisaDataToSignByCardWallet(
                request = request,
                hashToSign = it.result.hash,
            )
        }
    }

    override suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): Either<VisaApiError, VisaDataToSignByCustomerWallet> = withContext(dispatcherProvider.io) {
        request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.getCustomerWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                request = GetCustomerWalletAcceptanceRequest(
                    cardWalletAddress = request.cardWalletAddress,
                    customerWalletAddress = request.customerWalletAddress,
                ),
            ).getOrThrow()
        }.map {
            VisaDataToSignByCustomerWallet(
                request = request,
                hashToSign = it.result.hash,
            )
        }
    }

    override suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet): Either<VisaApiError, Unit> =
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

                visaApi.activateByCardWallet(
                    authHeader = authTokens.getAuthHeader(),
                    body = ActivationByCardWalletRequest(
                        orderId = signedData.dataToSign.request.activationOrderInfo.orderId,
                        cardWallet = ActivationByCardWalletRequest.CardWallet(
                            address = signedData.dataToSign.request.cardWalletAddress,
                            cardWalletConfirmation = null, // for second iteration
                        ),
                        deployAcceptanceSignature = signedData.signature,
                        otp = ActivationByCardWalletRequest.Otp(
                            rootOtp = signedData.rootOTP,
                            counter = signedData.otpCounter,
                        ),
                    ),
                ).getOrThrow()
            }
        }

    override suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet) =
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

                visaApi.activateByCustomerWallet(
                    authHeader = authTokens.getAuthHeader(),
                    body = ActivationByCustomerWalletRequest(
                        orderId = signedData.dataToSign.request.orderId,
                        customerWallet = ActivationByCustomerWalletRequest.CustomerWallet(
                            deployAcceptanceSignature = signedData.signature,
                            customerWalletAddress = signedData.customerWalletAddress,
                        ),
                    ),
                ).getOrThrow()
            }
        }

    override suspend fun sendPinCode(pinCode: VisaEncryptedPinCode) = withContext(dispatcherProvider.io) {
        request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.setPinCode(
                authHeader = authTokens.getAuthHeader(),
                body = SetPinCodeRequest(
                    orderId = pinCode.activationOrderId,
                    sessionId = pinCode.sessionId,
                    iv = pinCode.iv,
                    pin = pinCode.encryptedPin,
                ),
            ).getOrThrow()
        }
    }

    override suspend fun getPinCodeRsaEncryptionPublicKey(): String {
        val env = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID.TangemVisa).environment
        val rsaPublicKey = visaLibLoader.getOrCreateConfig().rsaPublicKey
        return when (env) {
            ApiEnvironment.DEV,
            ApiEnvironment.STAGE,
            -> rsaPublicKey.dev
            ApiEnvironment.PROD -> rsaPublicKey.prod
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<VisaApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.code != ApiResponseError.HttpException.Code.UNAUTHORIZED &&
                responseError.errorBody != null
            ) {
                val errorCode =
                    visaErrorAdapter.fromJson(responseError.errorBody!!)?.error?.code ?: responseError.code.numericCode
                return Either.Left(VisaApiError.fromBackendError(errorCode))
            }

            if (responseError !is ApiResponseError.HttpException) {
                return Either.Left(VisaApiError.UnknownWithoutCode)
            }

            val authTokens = visaAuthTokenStorage.get(visaCardId.cardId) ?: error("Auth tokens are not stored")
            val newTokens = visaAuthRepository.refreshAccessTokens(authTokens.refreshToken).getOrElse {
                return Either.Left(VisaApiError.RefreshTokenExpired)
            }

            visaAuthTokenStorage.store(visaCardId.cardId, newTokens)

            runCatching {
                Either.Right(requestBlock())
            }.getOrElse { responseError ->
                if (responseError is ApiResponseError.HttpException && responseError.errorBody != null) {
                    val errorCode =
                        visaErrorAdapter.fromJson(responseError.errorBody!!)?.error?.code
                            ?: responseError.code.numericCode
                    Either.Left(VisaApiError.fromBackendError(errorCode))
                } else {
                    Either.Left(VisaApiError.UnknownWithoutCode)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : VisaActivationRepository.Factory {
        override fun create(cardId: VisaCardId): DefaultVisaActivationRepository
    }
}