package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** In MOCK env short-circuits RSA-encrypted flows (reveal/getPin/setPin) with hardcoded values. */
@Singleton
internal class MockAwareTangemPayCardDetailsRepository @Inject constructor(
    private val real: DefaultTangemPayCardDetailsRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : TangemPayCardDetailsRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(ApiConfig.ID.TangemPay)
            .environment == ApiEnvironment.MOCK

    override suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance> =
        real.getCardBalance(userWalletId)

    override suspend fun revealCardDetails(
        userWalletId: UserWalletId,
    ): Either<UniversalError, TangemPayCardDetails> {
        if (isMockMode) {
            return TangemPayCardDetails(
                pan = MOCK_PAN,
                cvv = MOCK_CVV,
                expirationYear = MOCK_EXPIRATION_YEAR,
                expirationMonth = MOCK_EXPIRATION_MONTH,
            ).right()
        }
        return real.revealCardDetails(userWalletId)
    }

    override suspend fun getPin(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?> {
        if (isMockMode) return MOCK_PIN.right()
        return real.getPin(userWalletId, cardId)
    }

    override suspend fun setPin(userWalletId: UserWalletId, pin: String): Either<UniversalError, SetPinResult> {
        if (isMockMode) return SetPinResult.SUCCESS.right()
        return real.setPin(userWalletId, pin)
    }

    override suspend fun isAddToWalletDone(userWalletId: UserWalletId): Either<UniversalError, Boolean> =
        real.isAddToWalletDone(userWalletId)

    override suspend fun setAddToWalletAsDone(userWalletId: UserWalletId): Either<UniversalError, Unit> =
        real.setAddToWalletAsDone(userWalletId)

    override suspend fun freezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardFrozenState> = real.freezeCard(userWalletId, cardId)

    override suspend fun unfreezeCard(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardFrozenState> = real.unfreezeCard(userWalletId, cardId)

    override fun cardFrozenState(cardId: String): Flow<TangemPayCardFrozenState> =
        real.cardFrozenState(cardId)

    override suspend fun cardFrozenStateSync(cardId: String): TangemPayCardFrozenState? =
        real.cardFrozenStateSync(cardId)

    override suspend fun updateCardDisplayName(
        cardId: String,
        userWalletId: UserWalletId,
        displayName: CardDisplayName,
    ): Either<UniversalError, Unit> = real.updateCardDisplayName(cardId, userWalletId, displayName)

    override suspend fun updateCardLimit(
        cardId: String,
        userWalletId: UserWalletId,
        limit: String,
    ): Either<UniversalError, Unit> = real.updateCardLimit(cardId, userWalletId, limit)

    private companion object {
        const val MOCK_PAN = "4242 4242 4242 4242"
        const val MOCK_CVV = "123"
        const val MOCK_EXPIRATION_YEAR = "2028"
        const val MOCK_EXPIRATION_MONTH = "12"
        const val MOCK_PIN = "1234"
    }
}