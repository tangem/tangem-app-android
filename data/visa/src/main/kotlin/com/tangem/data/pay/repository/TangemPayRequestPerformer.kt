package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.squareup.wire.Instant
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.pay.TangemPayAuthApi
import com.tangem.datasource.api.pay.models.request.RefreshCustomerWalletAccessTokenRequest
import com.tangem.datasource.api.pay.models.response.TangemPayGetTokensResponse
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.getAuthHeader
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TangemPayRequestPerformer"

@Singleton
internal class TangemPayRequestPerformer @Inject constructor(
    private val errorConverter: TangemPayErrorConverter,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayAuthApi: TangemPayAuthApi,
    private val tangemPayStorage: TangemPayStorage,
) {

    private val customerWalletAddresses = ConcurrentHashMap<UserWalletId, String>()
    private val tokensMutex = Mutex()

    /**
     * Static token added in headers [com.tangem.datasource.api.common.config.TangemPay]
     */
    suspend fun <T : Any> performWithStaticToken(requestBlock: suspend () -> ApiResponse<T>): Either<VisaApiError, T> =
        withContext(dispatchers.io) {
            catch(
                block = {
                    when (val apiResponse = requestBlock()) {
                        is ApiResponse.Error -> errorConverter.convert(apiResponse.cause).left()
                        is ApiResponse.Success<T> -> apiResponse.data.right()
                    }
                },
                catch = { errorConverter.convert(it).left() },
            )
        }

    suspend fun <T : Any> performRequest(
        userWalletId: UserWalletId,
        requestBlock: suspend (header: String) -> ApiResponse<T>,
    ): Either<VisaApiError, T> = withContext(dispatchers.io) {
        catch(
            block = {
                val tokens = getAccessTokens(userWalletId).getOrElse { return@catch it.left() }

                when (val apiResponse: ApiResponse<T> = requestBlock(tokens.getAuthHeader())) {
                    is ApiResponse.Error -> errorConverter.convert(apiResponse.cause).left()
                    is ApiResponse.Success<T> -> apiResponse.data.right()
                }
            },
            catch = { errorConverter.convert(it).left() },
        ).onLeft { visaApiError -> Timber.tag(TAG).e(visaApiError.toString()) }
    }

    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String {
        val existingAddress = customerWalletAddresses[userWalletId]
        if (existingAddress != null) {
            return existingAddress
        }
        val storedAddress = tangemPayStorage.getCustomerWalletAddress(
            userWalletId = userWalletId,
        ) ?: error("Can not find customer address")

        customerWalletAddresses[userWalletId] = storedAddress
        return storedAddress
    }

    private suspend fun getAccessTokens(userWalletId: UserWalletId): Either<VisaApiError, TangemPayAuthTokens> {
        return tokensMutex.withLock {
            val walletAddress = getCustomerWalletAddress(userWalletId)
            val tokens = tangemPayStorage.getAuthTokens(walletAddress) ?: error("Auth tokens are not stored")
            val now = Instant.now()
            val accessExpiresAt = Instant.ofEpochSecond(tokens.expiresAt)
            val refreshExpiresAt = Instant.ofEpochSecond(tokens.refreshExpiresAt)

            if (accessExpiresAt.isAfter(now)) {
                tokens.right()
            } else if (accessExpiresAt.isBefore(now) && refreshExpiresAt.isAfter(now)) {
                refreshAuthTokens(userWalletId = userWalletId, refreshToken = tokens.refreshToken)
            } else {
                VisaApiError.RefreshTokenExpired.left()
            }
        }
    }

    private suspend fun refreshAuthTokens(
        userWalletId: UserWalletId,
        refreshToken: String,
    ): Either<VisaApiError, TangemPayAuthTokens> {
        val customerWalletAddress = getCustomerWalletAddress(userWalletId)
        val apiResponse = tangemPayAuthApi.refreshCustomerWalletAccessToken(
            request = RefreshCustomerWalletAccessTokenRequest(
                authType = "customer_wallet",
                refreshToken = refreshToken,
            ),
        )
        val responseEither = when (apiResponse) {
            is ApiResponse.Error -> errorConverter.convert(apiResponse.cause).left()
            is ApiResponse.Success<TangemPayGetTokensResponse> -> apiResponse.data.right()
        }
        return responseEither.map { response ->
            TangemPayAuthTokens(
                accessToken = response.accessToken,
                expiresAt = response.expiresAt,
                refreshToken = response.refreshToken,
                refreshExpiresAt = response.refreshExpiresAt,
            )
        }.mapLeft { error ->
            Timber.tag(TAG).e("Can not refresh auth tokens: $error")
            if (error is VisaApiError.ServerUnavailable) error else VisaApiError.RefreshTokenExpired
        }.onRight { tokens ->
            tangemPayStorage.storeAuthTokens(customerWalletAddress = customerWalletAddress, tokens = tokens)
        }
    }
}