package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.squareup.moshi.Moshi
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.util.TangemPayErrorConverter
import com.tangem.data.pay.util.TangemPayWalletsManager
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthTokens
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
    private val dispatchers: CoroutineDispatcherProvider,
    private val tangemPayStorage: TangemPayStorage,
    private val authDataSource: TangemPayAuthDataSource,
    private val tangemPayWalletsManager: TangemPayWalletsManager,
) {

    private val customerWalletAddress = MutableStateFlow<String?>(null)

    private val refreshTokensMutex = Mutex()
    private var refreshTokensJob: Deferred<VisaAuthTokens>? = null

    private val errorConverter = TangemPayErrorConverter(moshi)

    suspend fun <T : Any> runWithErrorLogs(tag: String, requestBlock: suspend () -> T): Either<UniversalError, T> {
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
        requestBlock: suspend (header: String) -> ApiResponse<T>,
    ): Either<VisaApiError, T> {
        return catch { request(requestBlock) }
            .mapLeft { exception ->
                Timber.tag("TangemPayRequestPerformer").e(exception)
                errorConverter.convert(exception)
            }
    }

    suspend fun <T : Any> request(requestBlock: suspend (header: String) -> ApiResponse<T>): T =
        withContext(dispatchers.io) {
            performRequest(
                requestBlock = requestBlock,
                getTokens = ::getAccessTokens,
                refreshTokens = ::refreshAuthTokens,
            )
        }

    private suspend fun <T : Any> performRequest(
        requestBlock: suspend (header: String) -> ApiResponse<T>,
        getTokens: (suspend () -> VisaAuthTokens),
        refreshTokens: (suspend () -> VisaAuthTokens)? = null,
    ): T = runCatching {
        val tokens = getTokens()
        val header = tokens.getAuthHeader()
        requestBlock(header).getOrThrow()
    }.getOrElse { error ->
        val unauthorizedCode = ApiResponseError.HttpException.Code.UNAUTHORIZED
        if (error is ApiResponseError.HttpException && refreshTokens != null && error.code == unauthorizedCode) {
            refreshOrJoin(refreshTokens)
            performRequest(requestBlock, refreshTokens = null, getTokens = getTokens)
        } else {
            throw error
        }
    }

    private suspend fun refreshOrJoin(refreshTokens: suspend () -> VisaAuthTokens): VisaAuthTokens {
        val jobToAwait: Deferred<VisaAuthTokens> =
            refreshTokensMutex.withLock {
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

    suspend fun getCustomerWalletAddress(): String {
        val existingAddress = customerWalletAddress.value
        if (existingAddress != null) {
            return existingAddress
        }
        val storedAddress = tangemPayStorage.getCustomerWalletAddress(
            userWalletId = tangemPayWalletsManager.getDefaultWalletForTangemPay().walletId,
        ) ?: error("Can not find customer address")

        customerWalletAddress.value = storedAddress
        return storedAddress
    }

    private suspend fun getAccessTokens(): VisaAuthTokens {
        val walletAddress = getCustomerWalletAddress()
        val tokens = tangemPayStorage.getAuthTokens(walletAddress) ?: error("Auth tokens are not stored")
        return tokens
    }

    private suspend fun refreshAuthTokens(): VisaAuthTokens {
        val customerWalletAddress = getCustomerWalletAddress()
        val refreshToken = getAccessTokens().refreshToken.value
        val tokens = authDataSource.refreshAuthTokens(refreshToken)
            .fold(
                ifLeft = { error -> error("Cannot refresh tokens: ${error.message}") },
                ifRight = { it },
            )
        tangemPayStorage.storeAuthTokens(customerWalletAddress, tokens)
        return tokens
    }
}