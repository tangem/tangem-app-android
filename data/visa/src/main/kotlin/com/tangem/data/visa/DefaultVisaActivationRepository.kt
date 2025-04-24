package com.tangem.data.visa

import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.data.visa.converter.VisaActivationStatusConverterWithState
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.datasource.api.visa.models.request.ActivationByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationByCustomerWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationStatusRequest
import com.tangem.datasource.api.visa.models.request.GetCardWalletAcceptanceRequest
import com.tangem.datasource.api.visa.models.request.GetCustomerWalletAcceptanceRequest
import com.tangem.datasource.api.visa.models.request.SetPinCodeRequest
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.domain.visa.exception.RefreshTokenExpiredException
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
    private val visaApi: TangemVisaApi,
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val visaAuthRepository: VisaAuthRepository,
    private val visaLibLoader: VisaLibLoader,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.getRemoteActivationStatus(
                authHeader = authTokens.getAuthHeader(),
                request = ActivationStatusRequest(
                    cardId = visaCardId.cardId,
                    cardPublicKey = visaCardId.cardPublicKey,
                ),
            ).getOrThrow()
        }

        VisaActivationStatusConverterWithState.convert(result)
    }

    override suspend fun getCardWalletAcceptanceData(
        request: VisaCardWalletDataToSignRequest,
    ): VisaDataToSignByCardWallet = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.getCardWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                request = GetCardWalletAcceptanceRequest(
                    customerWalletAddress = request.activationOrderInfo.customerWalletAddress,
                    cardWalletAddress = request.cardWalletAddress,
                ),
            ).getOrThrow()
        }

        VisaDataToSignByCardWallet(
            request = request,
            hashToSign = result.result.hash,
        )
    }

    override suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): VisaDataToSignByCustomerWallet = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }

            visaApi.getCustomerWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                request = GetCustomerWalletAcceptanceRequest(
                    cardWalletAddress = request.cardWalletAddress,
                    customerWalletAddress = request.customerWalletAddress,
                ),
            ).getOrThrow()
        }

        VisaDataToSignByCustomerWallet(
            request = request,
            hashToSign = result.result.hash,
        )
    }

    override suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet) {
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
    }

    override suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet) {
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
    }

    override suspend fun sendPinCode(pinCode: VisaEncryptedPinCode) {
        withContext(dispatcherProvider.io) {
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
    }

    override suspend fun getPinCodeRsaEncryptionPublicKey(): String {
        return visaLibLoader.getOrCreateConfig().rsaPublicKey
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): T {
        return runCatching {
            requestBlock()
        }.getOrElse { responseError ->
            if (responseError !is ApiResponseError.HttpException ||
                responseError.code != ApiResponseError.HttpException.Code.UNAUTHORIZED
            ) {
                throw responseError
            }

            val authTokens = visaAuthTokenStorage.get(visaCardId.cardId) ?: error("Auth tokens are not stored")
            val newTokens = runCatching {
                visaAuthRepository.refreshAccessTokens(authTokens.refreshToken)
            }.getOrElse { throw RefreshTokenExpiredException() }

            visaAuthTokenStorage.store(visaCardId.cardId, newTokens)

            requestBlock()
        }
    }

    @AssistedFactory
    interface Factory : VisaActivationRepository.Factory {
        override fun create(cardId: VisaCardId): DefaultVisaActivationRepository
    }
}