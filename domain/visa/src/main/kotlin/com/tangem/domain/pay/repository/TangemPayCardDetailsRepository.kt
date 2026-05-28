package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

interface TangemPayCardDetailsRepository {

    suspend fun getCardBalance(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardBalance>

    suspend fun revealCardDetails(userWalletId: UserWalletId): Either<UniversalError, TangemPayCardDetails>

    suspend fun getPin(userWalletId: UserWalletId, cardId: String): Either<UniversalError, String?>

    suspend fun setPin(userWalletId: UserWalletId, pin: String): Either<UniversalError, SetPinResult>

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