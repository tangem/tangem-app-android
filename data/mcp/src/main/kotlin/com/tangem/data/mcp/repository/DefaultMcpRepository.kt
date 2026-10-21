package com.tangem.data.mcp.repository

import android.util.Base64
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.recover
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.mcp.converter.McpClientConverter
import com.tangem.data.mcp.converter.toDomain
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.data.mcp.api.McpApi
import com.tangem.data.mcp.api.models.request.McpDeviceVerificationConsent
import com.tangem.data.mcp.api.models.response.McpSessionResponse
import com.tangem.data.mcp.store.McpAgentStore
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "MCP"

internal class DefaultMcpRepository @Inject constructor(
    private val mcpApi: McpApi,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val mcpClientConverter: McpClientConverter,
    private val agentStore: McpAgentStore,
) : McpRepository {

    private val sessions = MutableStateFlow<List<McpSession>>(value = emptyList())
    private val loadJobHolder = JobHolder()

    override fun getSessions(): Flow<List<McpSession>> = sessions.onStart { loadSessions(force = false) }

    override suspend fun getAgent(clientId: String): McpAgent {
        agentStore.getOrNull(clientId)?.let { return it }

        return mcpApi.getClient(clientId = clientId)
            .toEither()
            .map(mcpClientConverter::convert)
            .onRight { agentStore.store(it) }
            .onLeft { TangemLogger.withTag(TAG).e("Agent $clientId cannot be looked up, treating it as unknown", it) }
            .getOrElse { McpAgent.unknown(clientId) }
    }

    override suspend fun getDeviceVerification(userCode: String): Either<Throwable, McpDeviceVerification> {
        val response = mcpApi.getDeviceVerification(userCode = userCode)

        return response.toEither().map { it.toDomain() }
    }

    override suspend fun approveDeviceVerification(
        verification: McpDeviceVerification,
        walletId: UserWalletId,
    ): Either<Throwable, McpDeviceVerificationResult> {
        val response = mcpApi.approveDeviceVerification(
            userCode = verification.userCode,
            state = verification.state,
            consent = McpDeviceVerificationConsent(
                clientId = verification.clientId,
                scope = verification.scope,
                walletId = walletId.toAuthWalletId(),
            ),
        )

        return response.toEither()
            .map { it.toDomain() }
            .onRight { if (it.isSuccess) loadSessions(force = true) }
    }

    override suspend fun rejectDeviceVerification(verification: McpDeviceVerification): Either<Throwable, Unit> {
        val response = mcpApi.rejectDeviceVerification(
            userCode = verification.userCode,
            state = verification.state,
        )

        return response.toEither()
    }

    override suspend fun revokeSession(sessionId: String): Either<Throwable, Unit> {
        return mcpApi.revokeSession(sessionId = sessionId)
            .toEither()
            .recoverGoneSession()
            .onRight { loadSessions(force = true) }
    }

    override suspend fun revokeAllSessions() {
        val issuedSessions = sessions.value
        if (issuedSessions.isEmpty()) return

        withContext(dispatchers.io) {
            issuedSessions
                .map { session -> async { session.id to mcpApi.revokeSession(sessionId = session.id).toEither() } }
                .awaitAll()
                .forEach { (sessionId, result) ->
                    result.recoverGoneSession().onLeft {
                        TangemLogger.withTag(TAG).e("Agent session $sessionId was not revoked", it)
                    }
                }
        }

        loadSessions(force = true)
    }

    /**
     * Turns a session the service no longer knows into a success: the caller asked for it to be gone,
     * and it is. The reload that follows takes it off the screen without an error of its own.
     */
    private fun Either<Throwable, Unit>.recoverGoneSession(): Either<Throwable, Unit> = recover { error ->
        val code = (error as? ApiResponseError.HttpException)?.code

        if (code != ApiResponseError.HttpException.Code.NOT_FOUND &&
            code != ApiResponseError.HttpException.Code.GONE
        ) {
            raise(error)
        }
    }

    /**
     * Reads the list into [sessions].
     *
     * Collectors share one request: a read that is already in flight is left to finish instead of
     * being duplicated. A change made by this repository passes [force], which restarts the read —
     * the in-flight one predates the change and would put the old list back.
     */
    @Synchronized
    private fun loadSessions(force: Boolean) {
        if (!force && loadJobHolder.isActive) return

        appScope.launch(dispatchers.io) {
            mcpApi.getSessions().toEither().fold(
                ifLeft = { TangemLogger.withTag(TAG).e("Failed to load the agent sessions", it) },
                ifRight = { sessions.value = it.map(McpSessionResponse::toDomain) },
            )
        }
            .saveIn(loadJobHolder)
    }

    /** The auth service addresses wallets by the Base64 form of the id, the same one wallet registration uses. */
    private fun UserWalletId.toAuthWalletId(): String = Base64.encodeToString(value, Base64.NO_WRAP)

    private fun <T : Any> ApiResponse<T>.toEither(): Either<Throwable, T> = when (this) {
        is ApiResponse.Success -> Either.Right(data)
        is ApiResponse.Error -> Either.Left(cause)
    }
}