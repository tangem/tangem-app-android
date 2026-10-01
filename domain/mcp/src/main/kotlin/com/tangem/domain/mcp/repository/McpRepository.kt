package com.tangem.domain.mcp.repository

import arrow.core.Either
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Authorizations the wallet owner has granted to MCP clients, and the device verification flow that
 * creates them.
 *
 * Every call runs on top of the mobile authentication session, so a verification and the approval
 * that follows it have to be made from the same device.
 */
interface McpRepository {

    /** Returns who is asking for access behind [userCode], so the owner can decide on it. */
    suspend fun getDeviceVerification(userCode: String): Either<Throwable, McpDeviceVerification>

    /**
     * Grants [verification] access to [walletId], with the client and the scope it carries.
     *
     * A refusal by the service comes back as [McpDeviceVerificationResult.isSuccess], not as a failure.
     */
    suspend fun approveDeviceVerification(
        verification: McpDeviceVerification,
        walletId: UserWalletId,
    ): Either<Throwable, McpDeviceVerificationResult>

    /** Returns the authorizations currently issued to MCP clients. */
    suspend fun getSessions(): Either<Throwable, List<McpSession>>

    /** Revokes [sessionId], cutting off the client's access and refresh tokens. */
    suspend fun revokeSession(sessionId: String): Either<Throwable, Unit>
}