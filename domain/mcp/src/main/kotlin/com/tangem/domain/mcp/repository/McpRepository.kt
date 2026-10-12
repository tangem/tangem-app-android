package com.tangem.domain.mcp.repository

import arrow.core.Either
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

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

    /** Turns [verification] down, so the agent is told no instead of waiting for the code to expire. */
    suspend fun rejectDeviceVerification(verification: McpDeviceVerification): Either<Throwable, Unit>

    /**
     * Authorizations currently issued to MCP clients, as a hot list shared by every collector.
     *
     * Collecting it reads the list unless a read is already in flight, and the list is re-read after
     * every change this repository makes. A failed read leaves the last known list in place.
     */
    fun getSessions(): Flow<List<McpSession>>

    /** Revokes [sessionId], cutting off the client's access and refresh tokens. */
    suspend fun revokeSession(sessionId: String): Either<Throwable, Unit>
}