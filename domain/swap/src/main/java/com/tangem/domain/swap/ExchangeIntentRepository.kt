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
     * An unknown intent, another wallet's intent and an ambiguous amount are an [ExchangeIntentError], which keeps
     * the Express error code where Express gave one. Network failures and timeouts are
     * [ExchangeIntentError.Unreachable]; other failures, including HTTP 5xx and a response that cannot be parsed, are
     * returned as the raw failure of the call.
     */
    suspend fun getExchangeIntent(userWalletId: UserWalletId, intentId: String): Either<Throwable, ExchangeIntent>
}