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
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.CardDetailsRepository
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

internal class DefaultCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val visaLibLoader: VisaLibLoader,
    private val apiConfigsManager: ApiConfigsManager,
    private val rainCryptoUtil: RainCryptoUtil,
) : CardDetailsRepository {

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
            val (secretKeyHex, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)

            val result = requestHelper.request { authHeader ->
                tangemPayApi.revealCardDetails(
                    authHeader = authHeader,
                    body = CardDetailsRequest(sessionId = sessionId),
                )
            }.result ?: error("Cannot reveal card details")

            val pan = rainCryptoUtil.decryptSecret(
                base64Secret = result.pan.secret,
                base64Iv = result.pan.iv,
                secretKeyHex = secretKeyHex,
            )

            val cvv = rainCryptoUtil.decryptSecret(
                base64Secret = result.cvv.secret,
                base64Iv = result.cvv.iv,
                secretKeyHex = secretKeyHex,
            )

            TangemPayCardDetails(
                pan = pan,
                cvv = cvv,
                expirationYear = result.expirationYear,
                expirationMonth = result.expirationMonth,
            )
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