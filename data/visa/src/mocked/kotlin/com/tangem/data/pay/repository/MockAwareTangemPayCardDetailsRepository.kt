package com.tangem.data.pay.repository

import com.tangem.spend.datasource.config.TangemPay

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** In MOCK env short-circuits RSA-encrypted flows (reveal/getPin/setPin) with hardcoded values. */
@Singleton
internal class MockAwareTangemPayCardDetailsRepository @Inject constructor(
    private val real: DefaultTangemPayCardDetailsRepository,
    private val apiConfigsManager: ApiConfigsManager,
    private val cardNameHolder: MockTangemPayCardNameHolder,
) : TangemPayCardDetailsRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> =
        real.getCardBalance(userWalletId)

    override suspend fun revealCardDetails(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardDetails> {
        if (isMockMode) {
            if (System.getProperty(UITEST_REVEAL_ERROR_KEY) == "1") return VisaApiError.ServerUnavailable.left()
            return TangemPayCardDetails(
                pan = MOCK_PAN,
                cvv = MOCK_CVV,
                expirationYear = MOCK_EXPIRATION_YEAR,
                expirationMonth = MOCK_EXPIRATION_MONTH,
            ).right()
        }
        return real.revealCardDetails(userWalletId, cardId)
    }

    override suspend fun getPin(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?> {
        if (isMockMode) {
            System.getProperty(UITEST_PIN_DELAY_MS_KEY)?.toLongOrNull()?.let { delay(it) }
            if (System.getProperty(UITEST_PIN_ERROR_KEY) == "1") return VisaApiError.ServerUnavailable.left()
            return MOCK_PIN.right()
        }
        return real.getPin(userWalletId, cardId)
    }

    override suspend fun setPin(
        userWalletId: UserWalletId,
        cardId: String,
        pin: String,
    ): Either<UniversalError, SetPinResult> {
        if (isMockMode) return SetPinResult.SUCCESS.right()
        return real.setPin(userWalletId, cardId, pin)
    }

    override suspend fun isAddToWalletDone(userWalletId: UserWalletId): Either<UniversalError, Boolean> =
        real.isAddToWalletDone(userWalletId)

    override suspend fun setAddToWalletAsDone(userWalletId: UserWalletId): Either<UniversalError, Unit> =
        real.setAddToWalletAsDone(userWalletId)

    override suspend fun freezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = real.freezeCard(userWalletId, cardId)

    override suspend fun unfreezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = real.unfreezeCard(userWalletId, cardId)

    override fun cardFrozenState(cardId: String): Flow<TangemPayCardFrozenState> =
        real.cardFrozenState(cardId)

    override suspend fun cardFrozenStateSync(cardId: String): TangemPayCardFrozenState? =
        real.cardFrozenStateSync(cardId)

    override suspend fun setCardFrozenState(cardId: String, state: TangemPayCardFrozenState) =
        real.setCardFrozenState(cardId, state)

    override suspend fun getOrderInfo(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<UniversalError, TangemPayOrderInfo> = real.getOrderInfo(userWalletId, orderId)

    override suspend fun updateCardDisplayName(
        cardId: String,
        userWalletId: UserWalletId,
        displayName: CardDisplayName,
    ): Either<UniversalError, Unit> = real.updateCardDisplayName(cardId, userWalletId, displayName)
        .onRight { if (isMockMode) cardNameHolder.displayName = displayName }

    override suspend fun updateCardLimit(
        cardId: String,
        userWalletId: UserWalletId,
        limit: String,
    ): Either<UniversalError, Unit> = real.updateCardLimit(cardId, userWalletId, limit)

    private companion object {
        // UI-test hook: forces the reveal to fail so the error-toast path can be verified.
        const val UITEST_REVEAL_ERROR_KEY = "uitest.tangempay.card_details_error"

        // UI-test hooks: force the current-PIN read to fail / to stall so its error and loading states can be verified.
        const val UITEST_PIN_ERROR_KEY = "uitest.tangempay.pin_error"
        const val UITEST_PIN_DELAY_MS_KEY = "uitest.tangempay.pin_delay_ms"
        const val MOCK_PAN = "4242 4242 4242 4242"
        const val MOCK_CVV = "123"
        const val MOCK_EXPIRATION_YEAR = "2028"
        const val MOCK_EXPIRATION_MONTH = "12"
        const val MOCK_PIN = "1234"
    }
}