package com.tangem.domain.swap

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError

/** Swaps AI agents have asked the wallets of the user to make. */
interface ExchangeIntentRepository {

    /**
     * Reads the exchange intent [intentId] as the wallet [userWalletId].
     *
     * Another wallet's intent is a `DataError.UserWalletError.WrongUserWallet`; an ambiguous amount is an
     * [ExchangeIntentError]; anything else, including a response that cannot be parsed, is the raw failure of the call.
     */
    suspend fun getExchangeIntent(userWalletId: UserWalletId, intentId: String): Either<Throwable, ExchangeIntent>
}