package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.SetPinResult
import com.tangem.domain.tangempay.model.TangemPayCardBalance
import com.tangem.domain.tangempay.model.TangemPayCardDetails
import com.tangem.domain.tangempay.model.TangemPayCardProvisioningData
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

interface TangemPayCardDetailsRepository {

    suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance>

    suspend fun revealCardDetails(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardDetails>

    suspend fun getPin(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?>

    suspend fun setPin(userWalletId: UserWalletId, cardId: String, pin: String): Either<UniversalError, SetPinResult>

    suspend fun getProvisioningData(
        userWalletId: UserWalletId,
        cardId: String,
    ): Either<UniversalError, TangemPayCardProvisioningData>

    fun isProductionEnvironment(): Boolean

    suspend fun isAddToWalletDone(userWalletId: UserWalletId): Either<UniversalError, Boolean>

    suspend fun setAddToWalletAsDone(userWalletId: UserWalletId): Either<UniversalError, Unit>

    suspend fun freezeCard(userWalletId: UserWalletId, cardId: String): Either<UniversalError, TangemPayOrderInfo>
    suspend fun unfreezeCard(userWalletId: UserWalletId, cardId: String): Either<UniversalError, TangemPayOrderInfo>

    fun cardFrozenState(cardId: String): Flow<TangemPayCardFrozenState>
    suspend fun cardFrozenStateSync(cardId: String): TangemPayCardFrozenState?
    suspend fun setCardFrozenState(cardId: String, state: TangemPayCardFrozenState)

    suspend fun updateCardDisplayName(
        cardId: String,
        userWalletId: UserWalletId,
        displayName: CardDisplayName,
    ): Either<UniversalError, Unit>

    suspend fun updateCardLimit(
        cardId: String,
        userWalletId: UserWalletId,
        limit: String,
    ): Either<UniversalError, Unit>

    suspend fun getOrderInfo(userWalletId: UserWalletId, orderId: String): Either<UniversalError, TangemPayOrderInfo>
}