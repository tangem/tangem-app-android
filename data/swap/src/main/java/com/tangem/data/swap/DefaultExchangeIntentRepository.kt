package com.tangem.data.swap

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.swap.converter.ExchangeIntentConverter
import com.tangem.domain.core.error.DataError
import com.tangem.domain.express.ExpressErrorResolver
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.ExchangeIntentRepository
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.utils.logging.TangemLogger

/**
 * `user-id` goes out as the app writes the wallet id (hex), while the owner `wid` of a hot wallet's agent token has
 * been base64 (backend plans hex). The ids are deliberately neither converted nor compared here.
 */
internal class DefaultExchangeIntentRepository(
    private val tangemExpressApi: TangemExpressApi,
    private val exchangeIntentConverter: ExchangeIntentConverter,
    private val expressErrorResolver: ExpressErrorResolver,
) : ExchangeIntentRepository {

    override suspend fun getExchangeIntent(
        userWalletId: UserWalletId,
        intentId: String,
    ): Either<Throwable, ExchangeIntent> {
        val response = tangemExpressApi.getExchangeIntent(
            userWalletId = userWalletId.stringValue,
            intentId = intentId,
        )

        val result: Either<Throwable, ExchangeIntent> = when (response) {
            is ApiResponse.Success -> exchangeIntentConverter.convert(response.data)
            is ApiResponse.Error -> {
                val cause = response.cause
                val isOfAnotherWallet = expressErrorResolver.resolve(cause) is ExpressError.IntentOfAnotherWalletError

                Either.Left(if (isOfAnotherWallet) wrongWalletError(intentId) else cause)
            }
        }

        return result.onLeft { error ->
            if (error is DataError.UserWalletError.WrongUserWallet) {
                TangemLogger.i("Exchange intent belongs to another wallet")
            } else {
                TangemLogger.e("Failed to read exchange intent $intentId", error)
            }
        }
    }

    private fun wrongWalletError(intentId: String): Throwable {
        return DataError.UserWalletError.WrongUserWallet("Intent $intentId belongs to another wallet")
    }
}