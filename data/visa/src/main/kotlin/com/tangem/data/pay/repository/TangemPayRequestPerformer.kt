package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.squareup.moshi.Moshi
import com.squareup.wire.Instant
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.getAuthHeader
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

internal class TangemPayRequestPerformer @Inject constructor(
    @NetworkMoshi moshi: Moshi,
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
) {

    private val customerWalletAddress = MutableStateFlow<String?>(null)

    private val refreshTokensMutex = Mutex()
    private var refreshTokensJob: Deferred<TangemPayAuthTokens>? = null

    private val errorConverter = TangemPayErrorConverter(moshi)

    @Deprecated("Do not use this method")
    suspend fun <T : Any> runWithErrorLogs(tag: String, requestBlock: suspend () -> T): Either<VisaApiError, T> {
        return try {
            val result = requestBlock()
            Either.Right(result)
        } catch (exception: Exception) {
            when (exception) {
                is CancellationException -> {
                    throw exception
                }
                else -> {
                    Timber.tag(tag).e(exception)
                    Either.Left(errorConverter.convert(exception))
                }
            }
        }
    }

    suspend fun <T : Any> makeSafeRequest(
        userWalletId: UserWalletId,
        requestBlock: suspend (header: String) -> ApiResponse<T>,
    ): Either<VisaApiError, T> {
        return performRequest(userWalletId, requestBlock)
    }

    @Deprecated("Use perform request instead", replaceWith = ReplaceWith("performRequest"))
    suspend fun <T : Any> request(
        userWalletId: UserWalletId,
        requestBlock: suspend (header: String) ->
        ApiResponse<T>,
    ): T = withContext(dispatchers.io) {
        performRequest(userWalletId, requestBlock = requestBlock)
            // to keep behaviour as previous
            .fold(
                ifRight = { it },
                ifLeft = { error -> error("Cannot perform request: $error") },
            )
    }

    suspend fun <T : Any> performWithStaticToken(
        requestBlock: suspend (header: String) -> ApiResponse<T>,
    ): Either<VisaApiError, T> = withContext(dispatchers.io) {
        catch(
            block = {
                val staticToken =
                    environmentConfigStorage.getConfigSync().bffStaticToken ?: error("BFF static token is null")
                when (val apiResponse = requestBlock(staticToken)) {
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
                val tokens = getAccessTokens(userWalletId)
                val now = Instant.now()
                val accessExpiresAt = Instant.ofEpochSecond(tokens.expiresAt)
                val refreshExpiresAt = Instant.ofEpochSecond(tokens.refreshExpiresAt)
                val apiResponse: ApiResponse<T> = if (accessExpiresAt.isAfter(now)) {
                    requestBlock(tokens.getAuthHeader())
                } else if (accessExpiresAt.isBefore(now) && refreshExpiresAt.isAfter(now)) {
                    val newTokens = refreshOrJoin(refreshTokens = { refreshAuthTokens(userWalletId) })
                    requestBlock(newTokens.getAuthHeader())
                } else {
                    return@catch VisaApiError.RefreshTokenExpired.left()
                }

                when (apiResponse) {
                    is ApiResponse.Error -> errorConverter.convert(apiResponse.cause).left()
                    is ApiResponse.Success<T> -> apiResponse.data.right()
                }
            },
            catch = { errorConverter.convert(it).left() },
        ).onLeft { visaApiError -> Timber.tag("TangemPayRequestPerformer").e(visaApiError.toString()) }
    }

    private suspend fun refreshOrJoin(refreshTokens: suspend () -> TangemPayAuthTokens): TangemPayAuthTokens {
        val jobToAwait: Deferred<TangemPayAuthTokens> = refreshTokensMutex.withLock {
            val current = refreshTokensJob
            if (current == null || current.isCompleted) {
                coroutineScope {
                    async { refreshTokens() }.also { refreshTokensJob = it }
                }
            } else {
                current
            }
        }
        val result = try {
            jobToAwait.await()
        } finally {
            refreshTokensMutex.withLock {
                if (refreshTokensJob === jobToAwait && jobToAwait.isCompleted) {
                    refreshTokensJob = null
                }
            }
        }
        return result
    }

    suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String {
        val existingAddress = customerWalletAddress.value
        if (existingAddress != null) {
            return existingAddress
        }
        val storedAddress = tangemPayStorage.getCustomerWalletAddress(
            userWalletId = userWalletId,
        ) ?: error("Can not find customer address")

        customerWalletAddress.value = storedAddress
        return storedAddress
    }

    private suspend fun getAccessTokens(userWalletId: UserWalletId): TangemPayAuthTokens {
        val walletAddress = getCustomerWalletAddress(userWalletId)
        val tokens = tangemPayStorage.getAuthTokens(walletAddress) ?: error("Auth tokens are not stored")
        return tokens
    }

    private suspend fun refreshAuthTokens(userWalletId: UserWalletId): TangemPayAuthTokens {
        val customerWalletAddress = getCustomerWalletAddress(userWalletId)
        val refreshToken = getAccessTokens(userWalletId).refreshToken
        val tokens = authDataSource.refreshAuthTokens(refreshToken)
            .fold(
                ifLeft = { error -> error("Cannot refresh tokens: ${error.message}") },
                ifRight = { it },
            )
        tangemPayStorage.storeAuthTokens(customerWalletAddress, tokens)
        return tokens
    }
}