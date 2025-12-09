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
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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

    private val pollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pollingJobs = mutableMapOf<String, Job>()
    private val storePollingMutex = Mutex()

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> {
        return requestHelper.runWithErrorLogs(TAG) {
            val result = requestHelper.request(userWalletId) { authHeader ->
                tangemPayApi.getCardBalance(authHeader)
            }.result ?: error("Cannot get card balance")
            TangemPayCardBalance(
                fiatBalance = result.fiat.availableBalance,
                currencyCode = result.fiat.currency,
                cryptoBalance = result.crypto.balance,
                availableForWithdrawal = result.availableForWithdrawal.amount,
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
        return requestHelper.performRequest(userWalletId) {
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
            if (state == TangemPayCardFrozenState.Pending) {
                startOrderIdPolling(
                    userWalletId = userWalletId,
                    cardId = cardId,
                    orderId = response.result?.orderId,
                    isFreeze = true,
                )
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
        return requestHelper.performRequest(userWalletId) {
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
            if (state == TangemPayCardFrozenState.Pending) {
                startOrderIdPolling(
                    userWalletId = userWalletId,
                    cardId = cardId,
                    orderId = response.result?.orderId,
                    isFreeze = false,
                )
            }
            cardFrozenStateStore.store(cardId, state)

            state
        }
    }

    private suspend fun startOrderIdPolling(
        userWalletId: UserWalletId,
        cardId: String,
        orderId: String?,
        isFreeze: Boolean,
    ) {
        if (orderId.isNullOrEmpty()) return
        storePollingMutex.withLock {
            if (pollingJobs.containsKey(orderId)) return
            val pollingJob = pollingScope.launch {
                try {
                    var retryCount = 0
                    while (isActive && pollingJobs.containsKey(orderId)) {
                        delay(duration = 5.seconds)

                        val orderStatus = requestHelper.performRequest(userWalletId) { authHeader ->
                            tangemPayApi.getOrder(authHeader, orderId)
                        }

                        orderStatus.onRight { response ->
                            val status = response.result?.status
                            if (status == OrderStatus.COMPLETED.apiName || status == OrderStatus.CANCELED.apiName) {
                                // Remove from jobs
                                pollingJobs.remove(key = orderId)

                                // Final card state
                                val finalState = when {
                                    status == OrderStatus.COMPLETED.apiName && isFreeze
                                    -> TangemPayCardFrozenState.Frozen
                                    status == OrderStatus.COMPLETED.apiName && !isFreeze
                                    -> TangemPayCardFrozenState.Unfrozen
                                    else -> return@launch
                                }

                                cardFrozenStateStore.store(cardId, finalState)
                            }
                        }.onLeft { error ->
                            Timber.e("error ${error.errorCode}")
                            // stop retrying after 3 errors
                            if (retryCount > MAX_POLLING_RETRIES) {
                                pollingJobs.remove(key = orderId)
                            }
                        }
                        retryCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    storePollingMutex.withLock {
                        pollingJobs.remove(orderId)
                    }
                }
            }
            pollingJobs[orderId] = pollingJob
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

    private companion object {
        const val MAX_POLLING_RETRIES = 3
    }
}