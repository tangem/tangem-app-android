package com.tangem.data.mcp.repository

import android.util.Base64
import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.mcp.converter.toDomain
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.request.McpDeviceVerificationConsent
import com.tangem.lib.auth.api.models.response.McpSessionResponse
import javax.inject.Inject

internal class DefaultMcpRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userWalletsListRepository: UserWalletsListRepository,
) : McpRepository {

    override suspend fun getDeviceVerification(userCode: String): Either<Throwable, McpDeviceVerification> {
        val response = authApi.getMcpDeviceVerification(userCode = userCode)

        return response.toEither().map { it.toDomain() }
    }

    override suspend fun approveDeviceVerification(
        verification: McpDeviceVerification,
        walletId: UserWalletId,
    ): Either<Throwable, McpDeviceVerificationResult> {
        val response = authApi.approveMcpDeviceVerification(
            userCode = verification.userCode,
            state = verification.state,
            consent = McpDeviceVerificationConsent(
                clientId = verification.clientId,
                scope = verification.scope,
                walletId = walletId.toAuthWalletId(),
            ),
        )

        return response.toEither().map { it.toDomain() }
    }

    override suspend fun getSessions(): Either<Throwable, List<McpSession>> {
        val response = authApi.getMcpSessions()

        return response.toEither().map { sessions -> sessions.toDomainWithStubbedFields() }
    }

    /**
     * The service scopes the session list by the access token, so its response carries neither the
     * wallet the authorization was granted for nor its creation time. Both are stubbed here — spread
     * over the wallets, timed to now — so the screen can be built against the final model. Delete

     */
    private suspend fun List<McpSessionResponse>.toDomainWithStubbedFields(): List<McpSession> {
        val wallets = userWalletsListRepository.userWalletsSync().ifEmpty { return emptyList() }

        return mapIndexed { index, session ->
            session.toDomain(
                walletId = wallets[index % wallets.size].walletId,
                createdAtMillis = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun revokeSession(sessionId: String): Either<Throwable, Unit> {
        return authApi.revokeMcpSession(sessionId = sessionId).toEither()
    }

    /** The auth service addresses wallets by the Base64 form of the id, the same one wallet registration uses. */
    private fun UserWalletId.toAuthWalletId(): String = Base64.encodeToString(value, Base64.NO_WRAP)

    private fun <T : Any> ApiResponse<T>.toEither(): Either<Throwable, T> = when (this) {
        is ApiResponse.Success -> Either.Right(data)
        is ApiResponse.Error -> Either.Left(cause)
    }
}