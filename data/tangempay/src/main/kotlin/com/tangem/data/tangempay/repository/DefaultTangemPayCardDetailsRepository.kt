package com.tangem.data.tangempay.repository

import com.tangem.spend.datasource.config.TangemPay

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.tangempay.util.OrderStatusConverter
import com.tangem.data.tangempay.util.RainCryptoUtil
import com.tangem.data.tangempay.util.TangemPayErrorConverter
import com.tangem.data.tangempay.config.TangemPayConfigLoader
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.TangemPayProvisioningApi
import com.tangem.spend.datasource.tangempay.models.request.CardDetailsRequest
import com.tangem.spend.datasource.tangempay.models.request.FreezeUnfreezeCardRequest
import com.tangem.spend.datasource.tangempay.models.request.UpdateCardRequest
import com.tangem.spend.datasource.tangempay.models.request.SetPinRequest
import com.tangem.spend.datasource.tangempay.store.TangemPayCardFrozenStateStore
import com.tangem.data.tangempay.store.TangemPayStorage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.SetPinResult
import com.tangem.domain.tangempay.model.TangemPayCardBalance
import com.tangem.domain.tangempay.model.TangemPayCardDetails
import com.tangem.domain.tangempay.model.TangemPayCardProvisioningData
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

@Suppress("LongParameterList", "LargeClass")
internal class DefaultTangemPayCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val tangemPayProvisioningApi: TangemPayProvisioningApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val configLoader: TangemPayConfigLoader,
    private val apiConfigsManager: ApiConfigsManager,
    private val rainCryptoUtil: RainCryptoUtil,
    private val storage: TangemPayStorage,
    private val cardFrozenStateStore: TangemPayCardFrozenStateStore,
    private val errorConverter: TangemPayErrorConverter,
) : TangemPayCardDetailsRepository {

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> {
        return catch(
            block = {
                val response = requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.getCardBalance(authHeader)
                }.getOrNull()

                val fiatBalance = requireNotNull(response?.result?.fiat) { "Cannot get card balance fiat" }
                TangemPayCardBalance(
                    fiatBalance = fiatBalance.availableBalance,
                    currencyCode = fiatBalance.currency,
                ).right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun revealCardDetails(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardDetails> {
        return catch(
            block = {
                val publicKeyBase64 = getPublicKeyBase64()
                val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
                val result = requireNotNull(
                    requestHelper.performRequest(userWalletId = userWalletId) { authHeader ->
                        tangemPayApi.revealCardDetails(
                            authHeader = authHeader,
                            cardId = cardId,
                            body = CardDetailsRequest(sessionId = sessionId),
                        )
                    }.getOrNull()?.result,
                )

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
                ).right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun getPin(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?> {
        return catch(
            block = {
                val publicKeyBase64 = getPublicKeyBase64()
                val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
                val response = requestHelper.performRequest(userWalletId = userWalletId) { authHeader ->
                    tangemPayApi.getPin(authHeader = authHeader, cardId = cardId, sessionId = sessionId)
                }.getOrNull()
                val result = requireNotNull(response?.result)

                val pin = rainCryptoUtil.decryptPin(
                    base64Secret = result.secret,
                    base64Iv = result.iv,
                    secretKeyBytes = secretKeyBytes,
                ).takeIf { !it.isNullOrEmpty() }

                secretKeyBytes.fill(0)
                pin.right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun setPin(
        userWalletId: UserWalletId,
        cardId: String,
        pin: String,
    ): Either<UniversalError, SetPinResult> {
        return catch(
            block = {
                val publicKeyBase64 = getPublicKeyBase64()
                val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
                val encryptedData = rainCryptoUtil.encryptPin(pin = pin, secretKeyBytes = secretKeyBytes)
                secretKeyBytes.fill(0)

                val response = requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.setPin(
                        authHeader = authHeader,
                        cardId = cardId,
                        body = SetPinRequest(
                            sessionId = sessionId,
                            pin = encryptedData.encryptedBase64,
                            iv = encryptedData.ivBase64,
                        ),
                    )
                }.getOrNull()
                val status = requireNotNull(response?.result?.result)
                val result = when (status) {
                    SetPinResult.SUCCESS.name -> SetPinResult.SUCCESS
                    SetPinResult.PIN_TOO_WEAK.name -> SetPinResult.PIN_TOO_WEAK
                    SetPinResult.DECRYPTION_ERROR.name -> SetPinResult.DECRYPTION_ERROR
                    else -> SetPinResult.UNKNOWN_ERROR
                }
                result.right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun getProvisioningData(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardProvisioningData> = either {
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayProvisioningApi.getCardProcessorDetails(authHeader = authHeader, cardId = cardId)
        }.bind()

        val result = response.result ?: raise(TangemPayApiError.Unspecified)
        val cardSecret = result.cardSecret?.takeIf(String::isNotBlank) ?: run {
            TangemLogger.withTag(TAG).e("Processor details returned no card secret")
            raise(TangemPayApiError.Unspecified)
        }

        TangemPayCardProvisioningData(processorCardId = result.cardId, cardSecret = cardSecret)
    }

    override fun isProductionEnvironment(): Boolean =
        apiConfigsManager.getEnvironmentConfig(TangemPay.Bff.ID).environment == ApiEnvironment.PROD

    override suspend fun isAddToWalletDone(userWalletId: UserWalletId): Either<UniversalError, Boolean> {
        return catch(
            block = {
                storage.getAddToWalletDone(
                    customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId),
                ).right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun setAddToWalletAsDone(userWalletId: UserWalletId): Either<UniversalError, Unit> {
        return catch(
            block = {
                storage.storeAddToWalletDone(
                    customerWalletAddress = requestHelper.getCustomerWalletAddress(userWalletId),
                    isDone = true,
                ).right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun freezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) {
            tangemPayApi.freezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.bind()

        val result = response.result ?: raise(TangemPayApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.orderId,
            orderStatus = OrderStatusConverter.convert(result.status),
        )
    }

    override suspend fun unfreezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) {
            tangemPayApi.unfreezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.bind()

        val result = response.result ?: raise(TangemPayApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.orderId,
            orderStatus = OrderStatusConverter.convert(result.status),
        )
    }

    override suspend fun updateCardDisplayName(
        cardId: String,
        userWalletId: UserWalletId,
        displayName: CardDisplayName,
    ): Either<UniversalError, Unit> {
        return catch(
            block = {
                requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.updateCard(
                        authHeader = authHeader,
                        body = UpdateCardRequest(
                            displayName = displayName.value,
                        ),
                        cardId = cardId,
                    )
                }.fold(
                    ifLeft = { error -> error.left() },
                    ifRight = { Unit.right() },
                )
            },
            catch = ::catchException,
        )
    }

    override suspend fun updateCardLimit(
        cardId: String,
        userWalletId: UserWalletId,
        limit: String,
    ): Either<UniversalError, Unit> {
        return catch(
            block = {
                requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.updateCard(
                        authHeader = authHeader,
                        body = UpdateCardRequest(
                            cardLimit = UpdateCardRequest.CardLimit(limit),
                        ),
                        cardId = cardId,
                    )
                }.fold(
                    ifLeft = { error -> error.left() },
                    ifRight = { Unit.right() },
                )
            },
            catch = ::catchException,
        )
    }

    override fun cardFrozenState(cardId: String): Flow<TangemPayCardFrozenState> {
        return cardFrozenStateStore.get(cardId)
    }

    override suspend fun cardFrozenStateSync(cardId: String): TangemPayCardFrozenState? {
        return cardFrozenStateStore.getSyncOrNull(cardId)
    }

    override suspend fun setCardFrozenState(cardId: String, state: TangemPayCardFrozenState) {
        cardFrozenStateStore.store(cardId, state)
    }

    override suspend fun getOrderInfo(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = either {
        val order = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader, orderId)
        }.mapLeft { error ->
            if (error is TangemPayApiError.NotFound) TangemPayApiError.OrderNotFound else error
        }.bind()

        val result = order.result ?: raise(TangemPayApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.id,
            orderStatus = OrderStatusConverter.convert(result.status),
            orderStep = OrderStep.fromString(result.step),
            orderType = OrderType.fromString(result.type ?: result.data.type),
            productInstanceId = result.data.productInstanceId,
        )
    }

    private suspend fun getPublicKeyBase64(): String {
        val rainRSAPublicKey = configLoader.getOrCreateConfig().rainRSAPublicKey
        return if (isProductionEnvironment()) rainRSAPublicKey.prod else rainRSAPublicKey.dev
    }

    private fun <T> catchException(throwable: Throwable): Either<UniversalError, T> {
        TangemLogger.withTag(TAG).e("Error", throwable)
        return errorConverter.convert(throwable).left()
    }
}