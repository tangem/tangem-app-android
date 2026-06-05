package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.util.RainCryptoUtil
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.data.visa.config.VisaLibLoader
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.CardDetailsRequest
import com.tangem.datasource.api.pay.models.request.FreezeUnfreezeCardRequest
import com.tangem.datasource.api.pay.models.request.UpdateCardRequest
import com.tangem.datasource.api.pay.models.request.SetPinRequest
import com.tangem.datasource.api.pay.models.response.FreezeUnfreezeCardResponse
import com.tangem.datasource.api.pay.models.response.OrderResponse.Result.Status
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

private const val TAG = "TangemPay: CardDetailsRepository"

@Suppress("LongParameterList", "LargeClass")
internal class DefaultTangemPayCardDetailsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val visaLibLoader: VisaLibLoader,
    private val apiConfigsManager: ApiConfigsManager,
    private val rainCryptoUtil: RainCryptoUtil,
    private val storage: TangemPayStorage,
    private val errorConverter: TangemPayErrorConverter,
) : TangemPayCardDetailsRepository {

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> {
        return catch(
            block = {
                val response = requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.getCardBalance(authHeader)
                }.getOrNull()

                val fiatBalance = requireNotNull(response?.result?.fiat) { "Cannot get card balance fiat" }
                val cryptoBalance = requireNotNull(response.result?.crypto) { "Cannot get card balance crypto" }
                val withdrawalAmount = requireNotNull(response.result?.availableForWithdrawal) {
                    "Cannot get card balance availableForWithdrawal"
                }
                TangemPayCardBalance(
                    fiatBalance = fiatBalance.availableBalance,
                    currencyCode = fiatBalance.currency,
                    cryptoBalance = cryptoBalance.balance,
                    availableForWithdrawal = withdrawalAmount.amount,
                    chainId = cryptoBalance.chainId,
                    depositAddress = cryptoBalance.depositAddress,
                    contractAddress = cryptoBalance.tokenContractAddress,
                ).right()
            },
            catch = ::catchException,
        )
    }

    override suspend fun revealCardDetails(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardDetails> {
        return catch(
            block = {
                val publicKeyBase64 = getPublicKeyBase64()
                val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
                val result = requireNotNull(
                    requestHelper.performRequest(userWalletId = userWalletId) { authHeader ->
                        tangemPayApi.revealCardDetails(
                            authHeader = authHeader,
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
                    tangemPayApi.getPin(authHeader = authHeader, sessionId = sessionId)
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

    override suspend fun setPin(userWalletId: UserWalletId, pin: String): Either<UniversalError, SetPinResult> {
        return catch(
            block = {
                val publicKeyBase64 = getPublicKeyBase64()
                val (secretKeyBytes, sessionId) = rainCryptoUtil.generateSecretKeyAndSessionId(publicKeyBase64)
                val encryptedData = rainCryptoUtil.encryptPin(pin = pin, secretKeyBytes = secretKeyBytes)
                secretKeyBytes.fill(0)

                val response = requestHelper.performRequest(userWalletId) { authHeader ->
                    tangemPayApi.setPin(
                        authHeader = authHeader,
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

        val result = response.result ?: raise(VisaApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.orderId,
            orderStatus = when (result.status) {
                FreezeUnfreezeCardResponse.Status.NEW -> OrderStatus.NEW
                FreezeUnfreezeCardResponse.Status.PROCESSING -> OrderStatus.PROCESSING
                FreezeUnfreezeCardResponse.Status.COMPLETED -> OrderStatus.COMPLETED
                FreezeUnfreezeCardResponse.Status.CANCELED -> OrderStatus.CANCELED
            },
        )
    }

    override suspend fun unfreezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = either {
        val response = requestHelper.performRequest(userWalletId) {
            tangemPayApi.unfreezeCard(authHeader = it, body = FreezeUnfreezeCardRequest(cardId = cardId))
        }.bind()

        val result = response.result ?: raise(VisaApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.orderId,
            orderStatus = when (result.status) {
                FreezeUnfreezeCardResponse.Status.NEW -> OrderStatus.NEW
                FreezeUnfreezeCardResponse.Status.PROCESSING -> OrderStatus.PROCESSING
                FreezeUnfreezeCardResponse.Status.COMPLETED -> OrderStatus.COMPLETED
                FreezeUnfreezeCardResponse.Status.CANCELED -> OrderStatus.CANCELED
            },
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

    override suspend fun getOrderInfo(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = either {
        val order = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader, orderId)
        }.bind()

        val result = order.result ?: raise(VisaApiError.Unspecified)

        TangemPayOrderInfo(
            orderId = result.id,
            orderStatus = when (result.status) {
                Status.NEW -> OrderStatus.NEW
                Status.PROCESSING -> OrderStatus.PROCESSING
                Status.COMPLETED -> OrderStatus.COMPLETED
                Status.CANCELED -> OrderStatus.CANCELED
            },
        )
    }

    private suspend fun getPublicKeyBase64(): String {
        val env = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID.TangemPay).environment
        return when (env) {
            ApiEnvironment.DEV,
            ApiEnvironment.DEV_2,
            ApiEnvironment.DEV_3,
            ApiEnvironment.STAGE,
            ApiEnvironment.STAGE_2,
            ApiEnvironment.STAGE_3,
            ApiEnvironment.MOCK,
            -> visaLibLoader.getOrCreateConfig().rainRSAPublicKey.dev
            ApiEnvironment.PROD -> visaLibLoader.getOrCreateConfig().rainRSAPublicKey.prod
        }
    }

    private fun <T> catchException(throwable: Throwable): Either<UniversalError, T> {
        TangemLogger.withTag(TAG).e("Error", throwable)
        return errorConverter.convert(throwable).left()
    }
}