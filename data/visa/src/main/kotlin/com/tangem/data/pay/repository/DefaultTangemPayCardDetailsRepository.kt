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
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

@Suppress("LongParameterList")
internal class DefaultTangemPayCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val visaLibLoader: VisaLibLoader,
    private val apiConfigsManager: ApiConfigsManager,
    private val rainCryptoUtil: RainCryptoUtil,
    private val storage: TangemPayStorage,
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
) : TangemPayCardDetailsRepository {

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request(userWalletId) { authHeader ->
                tangemPayApi.getCardBalance(authHeader)
            }.result ?: error("Cannot get card balance")
            TangemPayCardBalance(
                fiatBalance = result.fiat.availableBalance,
                currencyCode = result.fiat.currency,
                cryptoBalance = result.crypto.balance,
                chainId = result.crypto.chainId,
                depositAddress = result.crypto.depositAddress,
                contractAddress = result.crypto.tokenContractAddress,
            )
        }
    }

    override suspend fun revealCardDetails(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardDetails> {
        return requestHelper.runWithErrorLogs(TAG) {
            val publicKeyBase64 = getPublicKeyBase64()
            val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)

            val result = requestHelper.request(userWalletId) { authHeader ->
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

    override suspend fun setPin(userWalletId: UserWalletId, pin: String): Either<UniversalError, SetPinResult> {
        return requestHelper.runWithErrorLogs(TAG) {
            val publicKeyBase64 = getPublicKeyBase64()
            val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
            val encryptedData = rainCryptoUtil.encryptPin(pin = pin, secretKeyBytes = secretKeyBytes)
            secretKeyBytes.fill(0)

            val status = requestHelper.request(userWalletId) { authHeader ->
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

    override suspend fun isAddToWalletDone(userWalletId: UserWalletId): Either<UniversalError, Boolean> {
        return requestHelper.runWithErrorLogs(TAG) {
            storage.getAddToWalletDone(requestHelper.getCustomerWalletAddress(userWalletId))
        }
    }

    override suspend fun setAddToWalletAsDone(userWalletId: UserWalletId): Either<UniversalError, Unit> {
        return requestHelper.runWithErrorLogs(TAG) {
            storage.storeAddToWalletDone(requestHelper.getCustomerWalletAddress(userWalletId), isDone = true)
        }
    }

    override suspend fun freezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardFrozenState> {
        cardFrozenStateStore.store(cardId, TangemPayCardFrozenState.Pending)
        return requestHelper.makeSafeRequest(userWalletId) {
            tangemPayApi.freezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.onLeft {
            cardFrozenStateStore.store(cardId, TangemPayCardFrozenState.Unfrozen)
        }.map { response ->
            val state = when (response.result?.status) {
                FreezeUnfreezeCardResponse.Status.COMPLETED -> TangemPayCardFrozenState.Frozen
                FreezeUnfreezeCardResponse.Status.NEW,
                FreezeUnfreezeCardResponse.Status.PROCESSING,
                -> TangemPayCardFrozenState.Pending
                FreezeUnfreezeCardResponse.Status.CANCELED,
                null,
                -> TangemPayCardFrozenState.Unfrozen
            }
            cardFrozenStateStore.store(cardId, state)

            state
        }
    }

    override suspend fun unfreezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardFrozenState> {
        cardFrozenStateStore.store(cardId, TangemPayCardFrozenState.Pending)
        return requestHelper.makeSafeRequest(userWalletId) {
            tangemPayApi.unfreezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.onLeft {
            cardFrozenStateStore.store(cardId, TangemPayCardFrozenState.Frozen)
        }.map { response ->
            val state = when (response.result?.status) {
                FreezeUnfreezeCardResponse.Status.COMPLETED -> TangemPayCardFrozenState.Unfrozen
                FreezeUnfreezeCardResponse.Status.NEW,
                FreezeUnfreezeCardResponse.Status.PROCESSING,
                -> TangemPayCardFrozenState.Pending
                FreezeUnfreezeCardResponse.Status.CANCELED,
                null,
                -> TangemPayCardFrozenState.Frozen
            }
            cardFrozenStateStore.store(cardId, state)

            state
        }
    }

    override fun cardFrozenState(cardId: String): Flow<TangemPayCardFrozenState> {
        return cardFrozenStateStore.get(cardId)
    }

    override suspend fun cardFrozenStateSync(cardId: String): TangemPayCardFrozenState? {
        return cardFrozenStateStore.getSyncOrNull(cardId)
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