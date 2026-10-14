package com.tangem.data.mcp.repository

import android.util.Base64
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.remote.response.ApiResponse
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
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MCP"

internal class DefaultMcpRepository @Inject constructor(
    private val mcpApi: McpApi,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : McpRepository {

    private val sessions = MutableStateFlow<List<McpSession>>(value = emptyList())
    private val loadJobHolder = JobHolder()

    override fun getSessions(): Flow<List<McpSession>> = sessions.onStart { loadSessions(force = false) }

    override suspend fun getAgent(clientId: String): McpAgent {
        return mcpApi.getClient(clientId = clientId)
            .toEither()
            .map { it.toDomain() }
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
            .onRight { loadSessions(force = true) }
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