package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.util.RainCryptoUtil
import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.CardDetailsRequest
import com.tangem.datasource.api.pay.models.request.FreezeUnfreezeCardRequest
import com.tangem.datasource.api.pay.models.request.SetPinRequest
import com.tangem.datasource.api.pay.models.response.FreezeUnfreezeCardResponse
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

@Suppress("OptionalUnit")
internal class DefaultTangemPayCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val visaLibLoader: VisaLibLoader,
    private val apiConfigsManager: ApiConfigsManager,
    private val rainCryptoUtil: RainCryptoUtil,
    private val storage: TangemPayStorage,
) : TangemPayCardDetailsRepository {

    override suspend fun getCardBalance(): Either<UniversalError, TangemPayCardBalance> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request { authHeader ->
                tangemPayApi.getCardBalance(authHeader)
            }.result ?: error("Cannot get card balance")
            TangemPayCardBalance(
                balance = result.availableBalance,
                currencyCode = result.currency,
            )
        }
    }

    override suspend fun revealCardDetails(): Either<UniversalError, TangemPayCardDetails> {
        return requestHelper.runWithErrorLogs(TAG) {
            val publicKeyBase64 = getPublicKeyBase64()
            val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)

            val result = requestHelper.request { authHeader ->
                tangemPayApi.revealCardDetails(
                    authHeader = authHeader,
                    body = CardDetailsRequest(sessionId = sessionId),
                )
            }.result ?: error("Cannot reveal card details")

            val pan = rainCryptoUtil.decryptSecret(
                base64Secret = result.pan.secret,
                base64Iv = result.pan.iv,
                secretKeyBytes = secretKeyBytes,
            )

            val cvv = rainCryptoUtil.decryptSecret(
                base64Secret = result.cvv.secret,
                base64Iv = result.cvv.iv,
                secretKeyBytes = secretKeyBytes,
            )
            secretKeyBytes.fill(0)

            TangemPayCardDetails(
                pan = pan,
                cvv = cvv,
                expirationYear = result.expirationYear,
                expirationMonth = result.expirationMonth,
            )
        }
    }

    override suspend fun setPin(pin: String): Either<UniversalError, SetPinResult> {
        return requestHelper.runWithErrorLogs(TAG) {
            val publicKeyBase64 = getPublicKeyBase64()
            val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
            val encryptedData = rainCryptoUtil.encryptPin(pin = pin, secretKeyBytes = secretKeyBytes)
            secretKeyBytes.fill(0)

            val status = requestHelper.request { authHeader ->
                tangemPayApi.setPin(
                    authHeader = authHeader,
                    body = SetPinRequest(
                        sessionId = sessionId,
                        pin = encryptedData.encryptedBase64,
                        iv = encryptedData.ivBase64,
                    ),
                )
            }.result?.result ?: error("Cannot set pin code")
            when (status) {
                SetPinResult.SUCCESS.name -> SetPinResult.SUCCESS
                SetPinResult.PIN_TOO_WEAK.name -> SetPinResult.PIN_TOO_WEAK
                SetPinResult.DECRYPTION_ERROR.name -> SetPinResult.DECRYPTION_ERROR
                else -> SetPinResult.UNKNOWN_ERROR
            }
        }
    }

    override suspend fun isAddToWalletDone(): Either<UniversalError, Boolean> {
        return requestHelper.runWithErrorLogs(TAG) {
            storage.getAddToWalletDone(requestHelper.getCustomerWalletAddress())
        }
    }

    override suspend fun setAddToWalletAsDone(): Either<UniversalError, Unit> {
        return requestHelper.runWithErrorLogs(TAG) {
            storage.storeAddToWalletDone(requestHelper.getCustomerWalletAddress(), isDone = true)
        }
    }

    override suspend fun freezeCard(cardId: String): Either<UniversalError, Unit> {
        return requestHelper.makeSafeRequest {
            tangemPayApi.freezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.map { response ->
            val result = response.result
            if (result != null && result.status == FreezeUnfreezeCardResponse.Status.COMPLETED) {
                Either.Right(Unit)
            } else {
                Timber.e("Card freeze failed with status: ${response.result?.status}")
                Either.Left(VisaApiError.UnknownWithoutCode)
            }
        }
    }

    override suspend fun unfreezeCard(cardId: String): Either<UniversalError, Unit> {
        return requestHelper.makeSafeRequest {
            tangemPayApi.unfreezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.map { response ->
            val result = response.result
            if (result != null && result.status == FreezeUnfreezeCardResponse.Status.COMPLETED) {
                Either.Right(Unit)
            } else {
                Timber.e("Card unfreeze failed with status: ${response.result?.status}")
                Either.Left(VisaApiError.UnknownWithoutCode)
            }
        }
    }

    private suspend fun getPublicKeyBase64(): String {
        val env = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID.TangemPay).environment
        return when (env) {
            ApiEnvironment.DEV,
            ApiEnvironment.DEV_2,
            ApiEnvironment.DEV_3,
            ApiEnvironment.STAGE,
            ApiEnvironment.MOCK,
            -> visaLibLoader.getOrCreateConfig().rainRSAPublicKey.dev
            ApiEnvironment.PROD -> visaLibLoader.getOrCreateConfig().rainRSAPublicKey.prod
        }
    }
}