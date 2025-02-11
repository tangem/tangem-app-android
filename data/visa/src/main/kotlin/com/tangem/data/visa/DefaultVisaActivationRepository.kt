package com.tangem.data.visa

import com.tangem.data.visa.converter.AccessCodeDataConverter
import com.tangem.data.visa.converter.VisaActivationStatusConverter
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.visa.TangemVisaApi
import com.tangem.datasource.api.visa.models.request.ActivationByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationByCustomerWalletRequest
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
    private val visaActivationStatusConverter: VisaActivationStatusConverter,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val accessCodeDataConverter: AccessCodeDataConverter,
    private val visaAuthRepository: VisaAuthRepository,
) : VisaActivationRepository {

    override suspend fun getActivationRemoteState(): VisaActivationRemoteState = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
            val accessCodeData = accessCodeDataConverter.convert(authTokens)

            visaApi.getRemoteActivationStatus(
                authHeader = authTokens.getAuthHeader(),
                customerId = accessCodeData.customerId,
                productInstanceId = accessCodeData.productInstanceId,
                cardId = visaCardId.cardId,
                cardPublicKey = visaCardId.cardPublicKey,
            ).getOrThrow()
        }

        visaActivationStatusConverter.convert(result)
    }

    override suspend fun getActivationRemoteStateLongPoll(): VisaActivationRemoteState =
        withContext(dispatcherProvider.io) {
            val result = request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
                val accessCodeData = accessCodeDataConverter.convert(authTokens)

                visaApi.getRemoteActivationStatusLongPoll(
                    authHeader = authTokens.getAuthHeader(),
                    customerId = accessCodeData.customerId,
                    productInstanceId = accessCodeData.productInstanceId,
                    cardId = visaCardId.cardId,
                    cardPublicKey = visaCardId.cardPublicKey,
                ).getOrThrow()
            }

            visaActivationStatusConverter.convert(result)
        }

    override suspend fun getCardWalletAcceptanceData(
        request: VisaCardWalletDataToSignRequest,
    ): VisaDataToSignByCardWallet = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
            val accessCodeData = accessCodeDataConverter.convert(authTokens)

            visaApi.getCardWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                customerId = accessCodeData.customerId,
                productInstanceId = accessCodeData.productInstanceId,
                activationOrderId = request.orderId,
                customerWalletAddress = request.customerWalletAddress,
            ).getOrThrow()
        }

        VisaDataToSignByCardWallet(
            request = request,
            hashToSign = result.dataForCardWallet.hash,
        )
    }

    override suspend fun getCustomerWalletAcceptanceData(
        request: VisaCustomerWalletDataToSignRequest,
    ): VisaDataToSignByCustomerWallet = withContext(dispatcherProvider.io) {
        val result = request {
            val authTokens =
                checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
            val accessCodeData = accessCodeDataConverter.convert(authTokens)

            visaApi.getCustomerWalletAcceptance(
                authHeader = authTokens.getAuthHeader(),
                customerId = accessCodeData.customerId,
                productInstanceId = accessCodeData.productInstanceId,
                activationOrderId = request.orderId,
                cardWalletAddress = request.cardWalletAddress,
            ).getOrThrow()
        }

        VisaDataToSignByCustomerWallet(
            request = request,
            hashToSign = result.dataForCardWallet.hash,
        )
    }

    override suspend fun activateCard(signedData: VisaSignedActivationDataByCardWallet) {
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
                val accessCodeData = accessCodeDataConverter.convert(authTokens)

                visaApi.activateByCardWallet(
                    authHeader = authTokens.getAuthHeader(),
                    body = ActivationByCardWalletRequest(
                        customerId = accessCodeData.customerId,
                        productInstanceId = accessCodeData.productInstanceId,
                        activationOrderId = signedData.dataToSign.request.orderId,
                        data = ActivationByCardWalletRequest.Data(
                            cardWallet = ActivationByCardWalletRequest.CardWallet(
                                address = signedData.cardWalletAddress,
                                cardWalletConfirmation = null, // for second iteration
                                deployAcceptanceSignature = signedData.signature,
                            ),
                            otp = ActivationByCardWalletRequest.Otp(
                                rootOtp = signedData.rootOTP,
                                counter = signedData.otpCounter,
                            ),
                        ),
                    ),
                )
            }
        }
    }

    override suspend fun approveByCustomerWallet(signedData: VisaSignedDataByCustomerWallet) {
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
                val accessCodeData = accessCodeDataConverter.convert(authTokens)

                visaApi.activateByCustomerWallet(
                    authHeader = authTokens.getAuthHeader(),
                    body = ActivationByCustomerWalletRequest(
                        customerId = accessCodeData.customerId,
                        productInstanceId = accessCodeData.productInstanceId,
                        activationOrderId = signedData.dataToSign.request.orderId,
                        data = ActivationByCustomerWalletRequest.Data(
                            customerWallet = ActivationByCustomerWalletRequest.CustomerWallet(
                                address = signedData.customerWalletAddress,
                                deployAcceptanceSignature = signedData.signature,
                            ),
                        ),
                    ),
                )
            }
        }
    }

    override suspend fun sendPinCode(pinCode: VisaEncryptedPinCode) {
        withContext(dispatcherProvider.io) {
            request {
                val authTokens =
                    checkNotNull(visaAuthTokenStorage.get(visaCardId.cardId)) { "Visa auth tokens are not stored" }
                val accessCodeData = accessCodeDataConverter.convert(authTokens)

                visaApi.setPinCode(
                    authHeader = authTokens.getAuthHeader(),
                    body = SetPinCodeRequest(
                        customerId = accessCodeData.customerId,
                        productInstanceId = accessCodeData.productInstanceId,
                        activationOrderId = pinCode.activationOrderId,
                        data = SetPinCodeRequest.Data(
                            sessionKey = pinCode.sessionId,
                            iv = pinCode.iv,
                            encryptedPin = pinCode.encryptedPin,
                        ),
                    ),
                )
            }
        }
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